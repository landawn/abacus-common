/*
 * Copyright (c) 2015, Haiyang Li.
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Immutable;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.NullSafe;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.Fn.BiPredicates;
import com.landawn.abacus.util.Fn.IntFunctions;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.function.ToFloatFunction;

/**
 * <p>
 * Note: This class includes codes copied from Apache Commons Lang, Google Guava and other open source projects under the Apache License 2.0.
 * The methods copied from other libraries/frameworks/projects may be modified in this class.
 * </p>
 * Class <code>N</code> is a general java utility class. It provides the most daily used operations for Object/primitive types/String/Array/Collection/Map/Bean...:
 *
 * <br />
 * <br />
 * When to throw exception? It's designed to avoid throwing any unnecessary
 * exception if the contract defined by method is not broken. for example, if
 * user tries to reverse a null or empty String. the input String will be
 * returned. But exception will be thrown if trying to repeat/swap a null or
 * empty string or operate Array/Collection by adding/removing...
 * <br />
 * There are only {@code fromIndex/startIndex} and {toIndex/endIndex} parameters in the methods defined in class {@code CommonUtil/N}, no {@code offset/count} parameters.
 *
 * @author Haiyang Li
 *
 * @version $Revision: 0.8 $ 07/03/10
 *
 * @see com.landawn.abacus.util.Array
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Maps
 * @see com.landawn.abacus.util.Strings
 * @see com.landawn.abacus.util.IOUtil
 */
@SuppressWarnings({ "java:S1192", "java:S6539" })
sealed class CommonUtil permits N {
    static final int BINARYSEARCH_THRESHOLD = 64;

    // ...
    static final int REVERSE_THRESHOLD = 18;

    static final int FILL_THRESHOLD = 25;

    static final int REPLACEALL_THRESHOLD = 11;

    static final int MIN_SIZE_FOR_COPY_ALL = 9;

    static final int DEFAULT_SIZE_FOR_NEW_COLLECTION = 9;

    // ...
    static final Random RAND = new SecureRandom();

    // ... it has to be big enough to make it's safety to add element to
    // ArrayBlockingQueue.
    @SuppressWarnings("deprecation")
    static final int POOL_SIZE = InternalUtil.POOL_SIZE;

    // ...
    /**
     * The index value when an element is not found in a list or array:
     * {@code -1}. This value is returned by methods in this class and can also
     * be used in comparisons with values returned by various method from
     * {@link java.util.List} .
     */
    public static final int INDEX_NOT_FOUND = -1;

    /**
     * An empty immutable {@code boolean} array.
     */
    public static final boolean[] EMPTY_BOOLEAN_ARRAY = {};
    /**
     * An empty immutable {@code char} array.
     */
    public static final char[] EMPTY_CHAR_ARRAY = {};
    /**
     * An empty immutable {@code byte} array.
     */
    public static final byte[] EMPTY_BYTE_ARRAY = {};
    /**
     * An empty immutable {@code short} array.
     */
    public static final short[] EMPTY_SHORT_ARRAY = {};
    /**
     * An empty immutable {@code int} array.
     */
    public static final int[] EMPTY_INT_ARRAY = {};
    /**
     * An empty immutable {@code long} array.
     */
    public static final long[] EMPTY_LONG_ARRAY = {};
    /**
     * An empty immutable {@code float} array.
     */
    public static final float[] EMPTY_FLOAT_ARRAY = {};
    /**
     * An empty immutable {@code double} array.
     */
    public static final double[] EMPTY_DOUBLE_ARRAY = {};
    /**
     * An empty immutable {@code Boolean} array.
     */
    public static final Boolean[] EMPTY_BOOLEAN_OBJ_ARRAY = {};
    /**
     * An empty immutable {@code Character} array.
     */
    public static final Character[] EMPTY_CHAR_OBJ_ARRAY = {};
    /**
     * An empty immutable {@code Byte} array.
     */
    public static final Byte[] EMPTY_BYTE_OBJ_ARRAY = {};
    /**
     * An empty immutable {@code Short} array.
     */
    public static final Short[] EMPTY_SHORT_OBJ_ARRAY = {};

    /**
     * An empty immutable {@code Integer} array.
     */
    public static final Integer[] EMPTY_INT_OBJ_ARRAY = {};
    /**
     * An empty immutable {@code Long} array.
     */
    public static final Long[] EMPTY_LONG_OBJ_ARRAY = {};
    /**
     * An empty immutable {@code Float} array.
     */
    public static final Float[] EMPTY_FLOAT_OBJ_ARRAY = {};
    /**
     * An empty immutable {@code Double} array.
     */
    public static final Double[] EMPTY_DOUBLE_OBJ_ARRAY = {};
    /**
     * An empty immutable {@code BigInteger} array.
     */
    public static final BigInteger[] EMPTY_BIG_INTEGER_ARRAY = {};
    /**
     * An empty immutable {@code BigDecimal} array.
     */
    public static final BigDecimal[] EMPTY_BIG_DECIMAL_ARRAY = {};
    /**
     * An empty immutable {@code String} array.
     */
    public static final String[] EMPTY_STRING_ARRAY = {};
    /**
     * An empty immutable {@code java.util.Date} array.
     */
    public static final java.util.Date[] EMPTY_JU_DATE_ARRAY = {};

    /**
     * An empty immutable {@code java.sql.Date} array.
     */
    public static final java.sql.Date[] EMPTY_DATE_ARRAY = {};

    /**
     * An empty immutable {@code Time} array.
     */
    public static final java.sql.Time[] EMPTY_TIME_ARRAY = {};

    /**
     * An empty immutable {@code Timestamp} array.
     */
    public static final java.sql.Timestamp[] EMPTY_TIMESTAMP_ARRAY = {};

    /**
    /**
     * An empty immutable {@code Calendar} array.
     */
    public static final Calendar[] EMPTY_CALENDAR_ARRAY = {};

    /**
    /**
     * An empty immutable {@code LocalDate} array.
     */
    public static final LocalDate[] EMPTY_LOCAL_DATE_ARRAY = {};

    /**
    /**
     * An empty immutable {@code LocalTime} array.
     */
    public static final LocalTime[] EMPTY_LOCAL_TIME_ARRAY = {};

    /**
    /**
     * An empty immutable {@code LocalDateTime} array.
     */
    public static final LocalDateTime[] EMPTY_LOCAL_DATE_TIME_ARRAY = {};

    /**
     * An empty immutable {@code Object} array.
     */
    public static final Object[] EMPTY_OBJECT_ARRAY = {};

    //    /**
    //     * An empty immutable {@code DataSet}.
    //     */
    //    public static final DataSet EMPTY_DATA_SET = RowDataSet.EMPTY_DATA_SET;

    /**
     * An empty immutable {@code Class} array.
     */
    static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];

    /**
     * An empty immutable {@code List}.
     */
    @SuppressWarnings("rawtypes")
    static final List EMPTY_LIST = Collections.emptyList();

    /**
     * An empty immutable {@code Set}.
     */
    @SuppressWarnings("rawtypes")
    static final Set EMPTY_SET = Collections.emptySet();

    /**
     * An empty immutable {@code SortedSet}.
     */
    @SuppressWarnings("rawtypes")
    static final SortedSet EMPTY_SORTED_SET = Collections.emptySortedSet();

    /**
     * An empty immutable {@code NavigableSet}.
     */
    @SuppressWarnings("rawtypes")
    static final NavigableSet EMPTY_NAVIGABLE_SET = Collections.emptyNavigableSet();

    /**
     * An empty immutable {@code Map}.
     */
    @SuppressWarnings("rawtypes")
    static final Map EMPTY_MAP = Collections.emptyMap();

    /**
     * An empty immutable {@code SortedMap}.
     */
    @SuppressWarnings("rawtypes")
    static final SortedMap EMPTY_SORTED_MAP = Collections.emptySortedMap();

    /**
     * An empty immutable {@code NavigableMap}.
     */
    @SuppressWarnings("rawtypes")
    static final NavigableMap EMPTY_NAVIGABLE_MAP = Collections.emptyNavigableMap();

    /**
     * An empty immutable {@code Iterator}.
     */
    @SuppressWarnings("rawtypes")
    static final Iterator EMPTY_ITERATOR = Collections.emptyIterator();

    /**
     * An empty immutable {@code ListIterator}.
     */
    @SuppressWarnings("rawtypes")
    static final ListIterator EMPTY_LIST_ITERATOR = Collections.emptyListIterator();

    @SuppressWarnings("rawtypes")
    static final Comparator NULL_MIN_COMPARATOR = Comparators.NULL_FIRST_COMPARATOR;

    @SuppressWarnings("rawtypes")
    static final Comparator NULL_MAX_COMPARATOR = Comparators.NULL_LAST_COMPARATOR;

    @SuppressWarnings("rawtypes")
    static final Comparator NATURAL_COMPARATOR = Comparators.NATURAL_ORDER;

    @SuppressWarnings("rawtypes")
    static final Comparator REVERSED_COMPARATOR = Comparators.REVERSED_ORDER;

    static final Comparator<Character> CHAR_COMPARATOR = Character::compare;

    static final Comparator<Byte> BYTE_COMPARATOR = Byte::compare;

    static final Comparator<Short> SHORT_COMPARATOR = Short::compare;

    static final Comparator<Integer> INT_COMPARATOR = Integer::compare;

    static final Comparator<Long> LONG_COMPARATOR = Long::compare;

    static final Comparator<Float> FLOAT_COMPARATOR = Float::compare;

    static final Comparator<Double> DOUBLE_COMPARATOR = Double::compare;

    // ...
    static final Object NULL_MASK = ClassUtil.createNullMask();

    // ...
    static final Map<Class<?>, Object> CLASS_EMPTY_ARRAY = new ConcurrentHashMap<>();

    static {
        CLASS_EMPTY_ARRAY.put(boolean.class, EMPTY_BOOLEAN_ARRAY);
        CLASS_EMPTY_ARRAY.put(Boolean.class, EMPTY_BOOLEAN_OBJ_ARRAY);

        CLASS_EMPTY_ARRAY.put(char.class, EMPTY_CHAR_ARRAY);
        CLASS_EMPTY_ARRAY.put(Character.class, EMPTY_CHAR_OBJ_ARRAY);

        CLASS_EMPTY_ARRAY.put(byte.class, EMPTY_BYTE_ARRAY);
        CLASS_EMPTY_ARRAY.put(Byte.class, EMPTY_BYTE_OBJ_ARRAY);

        CLASS_EMPTY_ARRAY.put(short.class, EMPTY_SHORT_ARRAY);
        CLASS_EMPTY_ARRAY.put(Short.class, EMPTY_SHORT_OBJ_ARRAY);

        CLASS_EMPTY_ARRAY.put(int.class, EMPTY_INT_ARRAY);
        CLASS_EMPTY_ARRAY.put(Integer.class, EMPTY_INT_OBJ_ARRAY);

        CLASS_EMPTY_ARRAY.put(long.class, EMPTY_LONG_ARRAY);
        CLASS_EMPTY_ARRAY.put(Long.class, EMPTY_LONG_OBJ_ARRAY);

        CLASS_EMPTY_ARRAY.put(float.class, EMPTY_FLOAT_ARRAY);
        CLASS_EMPTY_ARRAY.put(Float.class, EMPTY_FLOAT_OBJ_ARRAY);

        CLASS_EMPTY_ARRAY.put(double.class, EMPTY_DOUBLE_ARRAY);
        CLASS_EMPTY_ARRAY.put(Double.class, EMPTY_DOUBLE_OBJ_ARRAY);

        CLASS_EMPTY_ARRAY.put(String.class, EMPTY_STRING_ARRAY);
        CLASS_EMPTY_ARRAY.put(Object.class, EMPTY_OBJECT_ARRAY);
    }

    // ...
    static final Map<Class<?>, Integer> CLASS_TYPE_ENUM = new HashMap<>();

    static {
        CLASS_TYPE_ENUM.put(boolean.class, 1);
        CLASS_TYPE_ENUM.put(char.class, 2);
        CLASS_TYPE_ENUM.put(byte.class, 3);
        CLASS_TYPE_ENUM.put(short.class, 4);
        CLASS_TYPE_ENUM.put(int.class, 5);
        CLASS_TYPE_ENUM.put(long.class, 6);
        CLASS_TYPE_ENUM.put(float.class, 7);
        CLASS_TYPE_ENUM.put(double.class, 8);
        CLASS_TYPE_ENUM.put(String.class, 9);
        CLASS_TYPE_ENUM.put(boolean[].class, 11);
        CLASS_TYPE_ENUM.put(char[].class, 12);
        CLASS_TYPE_ENUM.put(byte[].class, 13);
        CLASS_TYPE_ENUM.put(short[].class, 14);
        CLASS_TYPE_ENUM.put(int[].class, 15);
        CLASS_TYPE_ENUM.put(long[].class, 16);
        CLASS_TYPE_ENUM.put(float[].class, 17);
        CLASS_TYPE_ENUM.put(double[].class, 18);
        CLASS_TYPE_ENUM.put(String[].class, 19);

        CLASS_TYPE_ENUM.put(Boolean.class, 21);
        CLASS_TYPE_ENUM.put(Character.class, 22);
        CLASS_TYPE_ENUM.put(Byte.class, 23);
        CLASS_TYPE_ENUM.put(Short.class, 24);
        CLASS_TYPE_ENUM.put(Integer.class, 25);
        CLASS_TYPE_ENUM.put(Long.class, 26);
        CLASS_TYPE_ENUM.put(Float.class, 27);
        CLASS_TYPE_ENUM.put(Double.class, 28);
    }

    // ...
    private static final Map<Class<? extends Enum<?>>, ImmutableList<? extends Enum<?>>> enumListPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<? extends Enum<?>>, ImmutableSet<? extends Enum<?>>> enumSetPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<? extends Enum<?>>, ImmutableBiMap<? extends Enum<?>, String>> enumMapPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<String, Type<?>> nameTypePool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Type<?>> clsTypePool = new ObjectPool<>(POOL_SIZE);

    static final String[] charStringCache = new String[128];

    static final int intStringCacheLow = -1001;

    static final int intStringCacheHigh = 10001;

    static final String[] intStringCache = new String[intStringCacheHigh - intStringCacheLow];

    static final Map<String, Integer> stringIntCache = new HashMap<>((int) (intStringCache.length * 1.5));

    static {
        for (int i = 0, j = intStringCacheLow, len = intStringCache.length; i < len; i++, j++) {
            intStringCache[i] = Integer.toString(j);
            stringIntCache.put(intStringCache[i], j);
        }

        for (int i = 0; i < charStringCache.length; i++) {
            charStringCache[i] = String.valueOf((char) i);
        }
    }

    CommonUtil() {
        // Utility class.
    }

    /**
     *
     * @param <T>
     * @param typeName
     * @return
     * @throws IllegalArgumentException if the specified <code>typeName</code> is <code>null</code>.
     */
    @SuppressWarnings("unchecked")
    public static <T> Type<T> typeOf(final String typeName) throws IllegalArgumentException {
        N.checkArgNotNull(typeName, "typeName");

        Type<?> type = nameTypePool.get(typeName);

        if (type == null) {
            type = TypeFactory.getType(typeName);

            nameTypePool.put(typeName, type);
        }

        return (Type<T>) type;
    }

    /**
     *
     * @param <T>
     * @param cls
     * @return
     * @throws IllegalArgumentException if the specified {@code Class} is <code>null</code>.
     */
    @SuppressWarnings("unchecked")
    public static <T> Type<T> typeOf(final Class<?> cls) throws IllegalArgumentException {
        N.checkArgNotNull(cls, "cls");

        Type<?> type = clsTypePool.get(cls);

        if (type == null) {
            type = TypeFactory.getType(cls);
            clsTypePool.put(cls, type);
        }

        return (Type<T>) type;
    }

    /**
     * Default value of.
     *
     * @param <T>
     * @param cls
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T defaultValueOf(final Class<T> cls) {
        return (T) typeOf(cls).defaultValue();
    }

    /**
     * Default if null.
     *
     * @param b
     * @return
     */
    public static boolean defaultIfNull(Boolean b) {
        if (b == null) {
            return false;
        }

        return b;
    }

    /**
     * Default if null.
     *
     * @param b
     * @param defaultForNull
     * @return
     */
    public static boolean defaultIfNull(Boolean b, boolean defaultForNull) {
        if (b == null) {
            return defaultForNull;
        }

        return b;
    }

    /**
     * Default if null.
     *
     * @param c
     * @return
     */
    public static char defaultIfNull(Character c) {
        if (c == null) {
            return Strings.CHAR_ZERO;
        }

        return c;
    }

    /**
     * Default if null.
     *
     * @param c
     * @param defaultForNull
     * @return
     */
    public static char defaultIfNull(Character c, char defaultForNull) {
        if (c == null) {
            return defaultForNull;
        }

        return c;
    }

    /**
     * Default if null.
     *
     * @param b
     * @return
     */
    public static byte defaultIfNull(Byte b) {
        if (b == null) {
            return (byte) 0;
        }

        return b;
    }

    /**
     * Default if null.
     *
     * @param b
     * @param defaultForNull
     * @return
     */
    public static byte defaultIfNull(Byte b, byte defaultForNull) {
        if (b == null) {
            return defaultForNull;
        }

        return b;
    }

    /**
     * Default if null.
     *
     * @param b
     * @return
     */
    public static short defaultIfNull(Short b) {
        if (b == null) {
            return (short) 0;
        }

        return b;
    }

    /**
     * Default if null.
     *
     * @param b
     * @param defaultForNull
     * @return
     */
    public static short defaultIfNull(Short b, short defaultForNull) {
        if (b == null) {
            return defaultForNull;
        }

        return b;
    }

    /**
     * Default if null.
     *
     * @param b
     * @return
     */
    public static int defaultIfNull(Integer b) {
        if (b == null) {
            return 0;
        }

        return b;
    }

    /**
     * Default if null.
     *
     * @param b
     * @param defaultForNull
     * @return
     */
    public static int defaultIfNull(Integer b, int defaultForNull) {
        if (b == null) {
            return defaultForNull;
        }

        return b;
    }

    /**
     * Default if null.
     *
     * @param b
     * @return
     */
    public static long defaultIfNull(Long b) {
        if (b == null) {
            return 0;
        }

        return b;
    }

    /**
     * Default if null.
     *
     * @param b
     * @param defaultForNull
     * @return
     */
    public static long defaultIfNull(Long b, long defaultForNull) {
        if (b == null) {
            return defaultForNull;
        }

        return b;
    }

    /**
     * Default if null.
     *
     * @param b
     * @return
     */
    public static float defaultIfNull(Float b) {
        if (b == null) {
            return 0;
        }

        return b;
    }

    /**
     * Default if null.
     *
     * @param b
     * @param defaultForNull
     * @return
     */
    public static float defaultIfNull(Float b, float defaultForNull) {
        if (b == null) {
            return defaultForNull;
        }

        return b;
    }

    /**
     * Default if null.
     *
     * @param b
     * @return
     */
    public static double defaultIfNull(Double b) {
        if (b == null) {
            return 0;
        }

        return b;
    }

    /**
     * Default if null.
     *
     * @param b
     * @param defaultForNull
     * @return
     */
    public static double defaultIfNull(Double b, double defaultForNull) {
        if (b == null) {
            return defaultForNull;
        }

        return b;
    }

    /**
     * Default if null.
     *
     * @param <T>
     * @param obj
     * @param defaultForNull
     * @return
     */
    public static <T> T defaultIfNull(final T obj, final T defaultForNull) {
        return obj == null ? defaultForNull : obj;
    }

    /**
     * Default if null.
     *
     * @param <T>
     * @param obj
     * @param supplierForDefault
     * @return
     */
    public static <T> T defaultIfNull(final T obj, final Supplier<? extends T> supplierForDefault) {
        if (obj == null) {
            return supplierForDefault.get();
        }

        return obj;
    }

    /**
     * Same as {@code Strings.defaultIfEmpty(CharSequence, CharSequence)}.
     *
     * @param <T>
     * @param str
     * @param defaultStr
     * @return
     * @see Strings#defaultIfEmpty(CharSequence, CharSequence)
     */
    public static <T extends CharSequence> T defaultIfEmpty(final T str, final T defaultStr) {
        return isEmpty(str) ? defaultStr : str;
    }

    /**
     *
     * @param <T>
     * @param str
     * @param getterForDefaultStr
     * @return
     * @see Strings#defaultIfEmpty(CharSequence, Supplier)
     */
    public static <T extends CharSequence> T defaultIfEmpty(final T str, final Supplier<? extends T> getterForDefaultStr) {
        if (isEmpty(str)) {
            return getterForDefaultStr.get();
        }

        return str;
    }

    /**
     * Same as {@code Strings.defaultIfBlank(CharSequence, CharSequence)}.
     *
     * @param <T>
     * @param str
     * @param defaultStr
     * @return
     * @see Strings#defaultIfBlank(CharSequence, CharSequence)
     */
    public static <T extends CharSequence> T defaultIfBlank(final T str, final T defaultStr) {
        return isBlank(str) ? defaultStr : str;
    }

    /**
     *
     * @param <T>
     * @param str
     * @param getterForDefaultStr
     * @return
     * @see Strings#defaultIfBlank(CharSequence, Supplier)
     */
    public static <T extends CharSequence> T defaultIfBlank(final T str, final Supplier<? extends T> getterForDefaultStr) {
        if (isBlank(str)) {
            return getterForDefaultStr.get();
        }

        return str;
    }

    /**
     * @param <T>
     * @param c
     * @param defaultColl
     * @return
     */
    public static <T extends Collection<?>> T defaultIfEmpty(final T c, final T defaultColl) {
        return isEmpty(c) ? defaultColl : c;
    }

    /**
     * @param <T>
     * @param m
     * @param defaultMap
     * @return
     */
    public static <T extends Map<?, ?>> T defaultIfEmpty(final T m, final T defaultMap) {
        return isEmpty(m) ? defaultMap : m;
    }

    /**
     *
     * @param val
     * @return
     */
    public static String stringOf(final boolean val) {
        return val ? Strings.TRUE : Strings.FALSE;
    }

    /**
     *
     * @param val
     * @return
     */
    public static String stringOf(final char val) {
        if (val < 128) {
            return charStringCache[val];
        }

        return String.valueOf(val);
    }

    /**
     *
     * @param val
     * @return
     */
    public static String stringOf(final byte val) {
        if (val > intStringCacheLow && val < intStringCacheHigh) {
            return intStringCache[val - intStringCacheLow];
        }

        return String.valueOf(val);
    }

    /**
     *
     * @param val
     * @return
     */
    public static String stringOf(final short val) {
        if (val > intStringCacheLow && val < intStringCacheHigh) {
            return intStringCache[val - intStringCacheLow];
        }

        return String.valueOf(val);
    }

    /**
     *
     * @param val
     * @return
     */
    public static String stringOf(final int val) {
        if (val > intStringCacheLow && val < intStringCacheHigh) {
            return intStringCache[val - intStringCacheLow];
        }

        return String.valueOf(val);
    }

    /**
     *
     * @param val
     * @return
     */
    public static String stringOf(final long val) {
        if (val > intStringCacheLow && val < intStringCacheHigh) {
            return intStringCache[(int) (val - intStringCacheLow)];
        }

        return String.valueOf(val);
    }

    /**
     *
     * @param val
     * @return
     */
    public static String stringOf(final float val) {
        return String.valueOf(val);
    }

    /**
     *
     * @param val
     * @return
     */
    public static String stringOf(final double val) {
        return String.valueOf(val);
    }

    /**
     *
     * @param obj
     * @return <code>null</code> if the specified object is null.
     */
    public static String stringOf(final Object obj) {
        return (obj == null) ? null : typeOf(obj.getClass()).stringOf(obj);
    }

    /**
     *
     *
     * @param <T>
     * @param str
     * @param targetType
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T valueOf(final String str, final Class<? extends T> targetType) {
        return (str == null) ? defaultValueOf(targetType) : (T) typeOf(targetType).valueOf(str);
    }

    private static final Map<Class<?>, BiFunction<Object, Class<?>, Object>> converterMap = new ConcurrentHashMap<>();

    /**
     *
     * @param srcClass
     * @param converter
     * @return {@code true} if there is no {@code converter} registered with specified {@code srcClass} yet before this call.
     * @throws IllegalArgumentException
     */
    @SuppressWarnings("rawtypes")
    public static boolean registerConverter(final Class<?> srcClass, final BiFunction<?, Class<?>, ?> converter) throws IllegalArgumentException {
        N.checkArgNotNull(srcClass, "srcClass");
        N.checkArgNotNull(converter, "converter");

        if (isBuiltinClass(srcClass)) {
            throw new IllegalArgumentException("Can't register converter with builtin class: " + ClassUtil.getCanonicalClassName(srcClass));
        }

        synchronized (converterMap) {
            if (converterMap.containsKey(srcClass)) {
                return false;
            }

            converterMap.put(srcClass, (BiFunction) converter);

            return true;
        }
    }

    static boolean isBuiltinClass(final Class<?> cls) {
        final Package pkg = cls.getPackage();

        if (pkg == null) {
            if (ClassUtil.isPrimitiveType(cls) || ClassUtil.isPrimitiveArrayType(cls)) {
                return true;
            } else if (cls.isArray()) {
                Class<?> componentType = cls.getComponentType();

                while (componentType.isArray()) {
                    componentType = componentType.getComponentType();
                }

                return ClassUtil.isPrimitiveType(cls) || ClassUtil.isPrimitiveArrayType(cls);
            }

            return false;
        }

        final String pkgName = pkg.getName();

        return Strings.isNotEmpty(pkgName) && (pkgName.startsWith("java.") || pkgName.startsWith("javax.") || pkgName.startsWith("com.landawn.abacus."));
    }

    /**
     * Try to convert the specified {@code obj} to the specified
     * {@code targetType}. Default value of {@code targetType} is returned if
     * {@code sourceObject} is null. An instance of {@code targetType} is returned if
     * convert successfully
     *
     * @param <T>
     * @param srcObj
     * @param targetType
     * @return
     */
    public static <T> T convert(final Object srcObj, final Class<? extends T> targetType) {
        if (srcObj == null) {
            return defaultValueOf(targetType);
        }

        final Class<?> srcClass = srcObj.getClass();
        BiFunction<Object, Class<?>, Object> converterFunc = null;

        if ((converterFunc = converterMap.get(srcClass)) != null) {
            return (T) converterFunc.apply(srcObj, targetType);
        }

        final Type<T> type = typeOf(targetType);

        return convert(srcObj, srcClass, type);
    }

    /**
     *
     * @param <T>
     * @param srcObj
     * @param targetType
     * @return
     */
    public static <T> T convert(final Object srcObj, final Type<? extends T> targetType) {
        if (srcObj == null) {
            return targetType.defaultValue();
        }

        final Class<?> srcClass = srcObj.getClass();
        BiFunction<Object, Class<?>, Object> converterFunc = null;

        if ((converterFunc = converterMap.get(srcClass)) != null) {
            return (T) converterFunc.apply(srcObj, targetType.clazz());
        }

        return convert(srcObj, srcClass, targetType);
    }

    @SuppressWarnings({ "rawtypes" })
    private static <T> T convert(final Object srcObj, Class<?> srcClass, final Type<? extends T> targetType) {
        if (targetType.clazz().isAssignableFrom(srcClass)) {
            return (T) srcObj;
        }

        final Type<Object> srcType = typeOf(srcClass);

        if (targetType.isString()) {
            return (T) srcType.stringOf(srcObj);
        } else if (targetType.isNumber()) {
            if (srcType.isString()) {
                return targetType.valueOf(srcObj);
            } else if (srcType.isNumber()) {
                return (T) Numbers.convert((Number) srcObj, (Type) targetType);
            }
        } else if (targetType.isBoolean() && srcType.isNumber()) {
            return (T) ((Boolean) (((Number) srcObj).longValue() > 0));
        } else if ((targetType.clazz().equals(int.class) || targetType.clazz().equals(Integer.class)) && srcType.clazz().equals(Character.class)) {
            return (T) (Integer.valueOf(((Character) srcObj).charValue())); //NOSONAR
        } else if ((targetType.clazz().equals(char.class) || targetType.clazz().equals(Character.class)) && srcType.clazz().equals(Integer.class)) {
            return (T) (Character.valueOf((char) ((Integer) srcObj).intValue()));
        } else if ((targetType.clazz().equals(long.class) || targetType.clazz().equals(Long.class)) && java.util.Date.class.isAssignableFrom(srcType.clazz())) {
            return (T) (Long) ((java.util.Date) srcObj).getTime();
        }

        if (targetType.isBean()) {
            if (srcType.isBean()) {
                return copy(srcObj, targetType.clazz());
            } else if (srcType.isMap()) {
                return Maps.map2Bean((Map<String, Object>) srcObj, targetType.clazz());
            }
        } else if (targetType.isMap()) {
            if (srcType.isBean() && targetType.getParameterTypes()[0].clazz().isAssignableFrom(String.class)
                    && Object.class.equals(targetType.getParameterTypes()[1].clazz())) {
                try {
                    final Map<String, Object> result = N.<String, Object> newMap((Class<Map>) targetType.clazz());
                    Maps.bean2Map(srcObj, result);
                    return (T) result;
                } catch (Exception e) {
                    // ignore.
                }
            } else if (srcType.isMap()) {
                final Map srcMap = (Map) srcObj;
                final Optional<Object> firstNonNullKeyOp = firstNonNull(srcMap.keySet());
                final Optional<Object> firstNonNullValueOp = firstNonNull(srcMap.values());

                if ((firstNonNullKeyOp.isEmpty() || targetType.getParameterTypes()[0].clazz().isAssignableFrom(firstNonNullKeyOp.get().getClass()))
                        && (firstNonNullValueOp.isEmpty()
                                || targetType.getParameterTypes()[1].clazz().isAssignableFrom(firstNonNullValueOp.get().getClass()))) {
                    final Map result = N.newMap((Class<Map>) targetType.clazz(), srcMap.size());
                    result.putAll(srcMap);
                    return (T) result;
                }
            }
        }

        if (targetType.isCollection()) {
            if (srcType.isCollection()) {
                final Collection srcColl = (Collection) srcObj;
                final Optional<Object> op = firstNonNull(srcColl);

                if (op.isEmpty() || targetType.getParameterTypes()[0].clazz().isAssignableFrom(op.get().getClass())) {
                    final Collection result = N.newCollection((Class<Collection>) targetType.clazz(), srcColl.size());
                    result.addAll(srcColl);
                    return (T) result;
                }
            } else if (srcType.isObjectArray() && targetType.getParameterTypes()[0].clazz().isAssignableFrom(srcType.clazz().getComponentType())) {
                final Object[] srcArray = (Object[]) srcObj;
                final Collection result = N.newCollection((Class<Collection>) targetType.clazz(), srcArray.length);
                result.addAll(Arrays.asList(srcArray));
                return (T) result;
            }
        }

        if (targetType.isObjectArray()) {
            if (srcType.isCollection()) {
                final Collection srcColl = (Collection) srcObj;
                final Optional<Object> op = firstNonNull(srcColl);

                if (op.isEmpty() || targetType.clazz().getComponentType().isAssignableFrom(op.get().getClass())) {
                    try {
                        final Object[] a = N.newArray(targetType.clazz().getComponentType(), srcColl.size());
                        srcColl.toArray(a);
                        return (T) a;
                    } catch (Exception e) {
                        // ignore;
                    }
                }
            }
            // If it works, it has returned by: targetType.clazz().isAssignableFrom(srcClass)
            //    } else if (srcType.isObjectArray()) {
            //        try {
            //            final Object[] srcArray = (Object[]) obj;
            //            final Object[] a = N.newArray(targetType.clazz().getComponentType(), srcArray.length);
            //            N.copy(srcArray, 0, a, 0, srcArray.length);
            //            return (T) a;
            //        } catch (Exception e) {
            //            // ignore;
            //        }
            //    }
        }

        if (targetType.clazz().equals(byte[].class)) {
            if (srcType.clazz().equals(Blob.class)) {
                final Blob blob = (Blob) srcObj;

                try {
                    return (T) blob.getBytes(1, (int) blob.length());
                } catch (SQLException e) {
                    throw new UncheckedSQLException(e);
                } finally {
                    try {
                        blob.free();
                    } catch (SQLException e) {
                        throw new UncheckedSQLException(e); //NOSONAR
                    }
                }
            } else if (srcType.clazz().equals(InputStream.class)) {
                final InputStream is = (InputStream) srcObj;

                try {
                    return (T) IOUtil.readAllBytes(is);
                } finally {
                    IOUtil.close(is);
                }
            }
        } else if (targetType.clazz().equals(char[].class)) {
            if (srcType.clazz().equals(Clob.class)) {
                final Clob clob = (Clob) srcObj;

                try {
                    return (T) clob.getSubString(1, (int) clob.length()).toCharArray();
                } catch (SQLException e) {
                    throw new UncheckedSQLException(e);
                } finally {
                    try {
                        clob.free();
                    } catch (SQLException e) {
                        throw new UncheckedSQLException(e); //NOSONAR
                    }
                }
            } else if (srcType.clazz().equals(Reader.class)) {
                final Reader reader = (Reader) srcObj;

                try {
                    return (T) IOUtil.readAllChars(reader);
                } finally {
                    IOUtil.close(reader);
                }
            } else if (srcType.clazz().equals(InputStream.class)) {
                final InputStream is = (InputStream) srcObj;

                try {
                    return (T) IOUtil.readAllChars(is);
                } finally {
                    IOUtil.close(is);
                }
            }
        } else if (targetType.clazz().equals(String.class)) {
            if (CharSequence.class.isAssignableFrom(srcType.clazz())) {
                return (T) ((CharSequence) srcObj).toString();
            } else if (srcType.clazz().equals(Clob.class)) {
                final Clob clob = (Clob) srcObj;

                try {
                    return (T) clob.getSubString(1, (int) clob.length());
                } catch (SQLException e) {
                    throw new UncheckedSQLException(e);
                } finally {
                    try {
                        clob.free();
                    } catch (SQLException e) {
                        throw new UncheckedSQLException(e); //NOSONAR
                    }
                }
            } else if (srcType.clazz().equals(Reader.class)) {
                final Reader reader = (Reader) srcObj;

                try {
                    return (T) IOUtil.readAllToString(reader);
                } finally {
                    IOUtil.close(reader);
                }
            } else if (srcType.clazz().equals(InputStream.class)) {
                final InputStream is = (InputStream) srcObj;

                try {
                    return (T) IOUtil.readAllToString(is);
                } finally {
                    IOUtil.close(is);
                }
            }
        } else if (targetType.clazz().equals(InputStream.class) && srcType.clazz().equals(byte[].class)) {
            return (T) new ByteArrayInputStream((byte[]) srcObj);
        } else if (targetType.clazz().equals(Reader.class) && CharSequence.class.isAssignableFrom(srcType.clazz())) {
            return (T) new StringReader(srcObj.toString());
        }

        if (srcObj instanceof AutoCloseable closeable) {
            try {
                return targetType.valueOf(srcObj);
            } finally {
                IOUtil.closeQuietly(closeable);
            }
        } else {
            return targetType.valueOf(srcObj);
        }
    }

    /**
     * Returns an empty <code>Nullable</code> if {@code val} is {@code null} while {@code targetType} is primitive or can not be assigned to {@code targetType}.
     * Please be aware that {@code null} can be assigned to any {@code Object} type except primitive types: {@code boolean/char/byte/short/int/long/double}.
     *
     * @param <T>
     * @param val
     * @param targetType
     * @return
     */
    @SuppressWarnings("unchecked")
    @Beta
    public static <T> Nullable<T> castIfAssignable(final Object val, final Class<? extends T> targetType) {
        if (ClassUtil.isPrimitiveType(targetType)) {
            return val != null && ClassUtil.wrap(targetType).isAssignableFrom(val.getClass()) ? Nullable.of((T) val) : Nullable.<T> empty();
        }

        return val == null || targetType.isAssignableFrom(val.getClass()) ? Nullable.of((T) val) : Nullable.<T> empty();
    }

    /**
     * Returns an empty <code>Nullable</code> if {@code val} is {@code null} while {@code targetType} is primitive or can not be assigned to {@code targetType}.
     * Please be aware that {@code null} can be assigned to any {@code Object} type except primitive types: {@code boolean/char/byte/short/int/long/double}.
     *
     * @param <T>
     * @param val
     * @param targetType
     * @return
     */
    @SuppressWarnings("unchecked")
    @Beta
    public static <T> Nullable<T> castIfAssignable(final Object val, final Type<? extends T> targetType) {
        return castIfAssignable(val, targetType.clazz());
    }

    //    /**
    //     * Checks if is bean.
    //     *
    //     * @param cls
    //     * @return true, if is bean
    //     * @see ClassUtil#isBeanClass(Class)
    //     * @deprecated replaced by {@code ClassUtil.isBeanClass(Class)}
    //     */
    //    @Deprecated
    //    public static boolean isBeanClass(final Class<?> cls) {
    //        return ClassUtil.isBeanClass(cls);
    //    }

    private static final Set<Class<?>> notKryoCompatible = newConcurrentHashSet();

    /**
     *
     * @param beanClass
     * @return
     * @see ClassUtil#getPropNameList(Class)
     */
    public static ImmutableList<String> getPropNames(final Class<?> beanClass) {
        return ClassUtil.getPropNameList(beanClass);
    }

    /**
     *
     * @param beanClass
     * @param propNameToExclude
     * @return
     * @see ClassUtil#getPropNames(Class, Set)
     * @see ClassUtil#getPropNames(Class, Collection)
     */
    public static List<String> getPropNames(final Class<?> beanClass, final Set<String> propNameToExclude) {
        return ClassUtil.getPropNames(beanClass, propNameToExclude);
    }

    /**
     *
     * @param bean
     * @return
     * @see ClassUtil#getNonNullPropNames(Object)
     * @see ClassUtil#getPropNames(Object, java.util.function.Predicate)
     * @see ClassUtil#getPropNames(Object, java.util.function.BiPredicate)
     */
    public static List<String> getNonNullPropNames(final Object bean) {
        return ClassUtil.getNonNullPropNames(bean);
    }

    /**
     *
     * @param <T>
     * @param bean
     * @param propName
     * @return
     * @see ClassUtil#getPropValue(Object, String)
     * @see BeanInfo#getPropValue(Object, String)
     */
    public static <T> T getPropValue(final Object bean, final String propName) {
        return ClassUtil.getPropValue(bean, propName);
    }

    /**
     * Gets the prop value.
     *
     * @param <T>
     * @param bean
     * @param propName
     * @param ignoreUnmatchedProperty
     * @return
     * @throws IllegalArgumentException if the specified property can't be gotten and ignoreUnmatchedProperty is false.
     * @see ClassUtil#getPropValue(Object, String, boolean)
     * @see BeanInfo#getPropValue(Object, String)
     */
    public static <T> T getPropValue(final Object bean, final String propName, final boolean ignoreUnmatchedProperty) {
        return ClassUtil.getPropValue(bean, propName, ignoreUnmatchedProperty);
    }

    /**
     * Refer to setPropValue(Method, Object, Object).
     *
     * @param bean
     * @param propName is case insensitive
     * @param propValue
     * @see ClassUtil#setPropValue(Object, String, Object)
     * @see BeanInfo#setPropValue(Object, String, Object)
     * @deprecated replaced by {@link BeanInfo#setPropValue(Object, String, Object)}
     */
    @Deprecated
    public static void setPropValue(final Object bean, final String propName, final Object propValue) {
        ClassUtil.setPropValue(bean, propName, propValue, false);
    }

    /**
     *
     * @param <T>
     * @param obj a Java object which must be serializable and deserializable through {@code Kryo} or {@code JSON}.
     * @return {@code null} if {@code bean} is {@code null}
     */
    @MayReturnNull
    @SuppressWarnings("unchecked")
    public static <T> T clone(final T obj) {
        if (obj == null) {
            return null; // NOSONAR
        }

        return (T) clone(obj, obj.getClass());
    }

    /**
     * Deeply copy by: obj -> serialize -> kryo/Json -> deserialize -> new object.
     *
     * @param <T>
     * @param obj a Java object which must be serializable and deserialiable through {@code Kryo} or {@code JSON}.
     * @param targetType
     * @return a new instance of {@code targetType} even if {@code bean} is {@code null}.
     * @throws IllegalArgumentException if {@code targetType} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    public static <T> T clone(final Object obj, final Class<? extends T> targetType) throws IllegalArgumentException {
        N.checkArgNotNull(targetType, "targetType");

        if (obj == null) {
            if (ClassUtil.isBeanClass(targetType)) {
                return copy(obj, targetType);
            } else {
                return newInstance(targetType);
            }
        }

        final Class<?> srcCls = obj.getClass();
        Object copy = null;

        if (Utils.kryoParser != null && targetType.equals(obj.getClass()) && !notKryoCompatible.contains(srcCls)) {
            try {
                copy = Utils.kryoParser.clone(obj);
            } catch (Exception e) {
                notKryoCompatible.add(srcCls);

                // ignore.
            }
        }

        if (copy == null) {
            String xml = Utils.abacusXMLParser.serialize(obj, Utils.xscForClone);
            copy = Utils.abacusXMLParser.deserialize(xml, targetType);
        }

        return (T) copy;
    }

    /**
     * Returns a new created instance of the same class and set with same
     * properties retrieved by 'getXXX' method in the specified {@code bean}.
     *
     * @param <T>
     * @param sourceBean a Java Object what allows access to properties using getter
     *            and setter methods.
     * @return {@code null} if {@code bean} is {@code null}
     */
    @MayReturnNull
    @SuppressWarnings("unchecked")
    public static <T> T copy(final T sourceBean) {
        if (sourceBean == null) {
            return null; // NOSONAR
        }

        return copy(sourceBean, (Class<T>) sourceBean.getClass());
    }

    /**
     *
     * @param <T>
     * @param sourceBean
     * @param selectPropNames
     * @return {@code null} if {@code bean} is {@code null}
     */
    @MayReturnNull
    public static <T> T copy(final T sourceBean, final Collection<String> selectPropNames) {
        if (sourceBean == null) {
            return null; // NOSONAR
        }

        return copy(sourceBean, selectPropNames, (Class<T>) sourceBean.getClass());
    }

    /**
     *
     * @param <T>
     * @param sourceBean
     * @param propFilter
     * @return {@code null} if {@code bean} is {@code null}
     */
    @MayReturnNull
    public static <T> T copy(final T sourceBean, final BiPredicate<String, ?> propFilter) {
        if (sourceBean == null) {
            return null; // NOSONAR
        }

        return copy(sourceBean, propFilter, (Class<T>) sourceBean.getClass());
    }

    /**
     *
     *
     * @param <T>
     * @param sourceBean
     * @param targetType
     * @return a new instance of {@code targetType} even if {@code bean} is {@code null}.
     * @throws IllegalArgumentException if {@code targetType} is {@code null}.
     */
    public static <T> T copy(final Object sourceBean, final Class<? extends T> targetType) throws IllegalArgumentException {
        return copy(sourceBean, (Collection<String>) null, targetType);
    }

    /**
     * Returns a new created instance of the specified {@code cls} and set with
     * same properties retrieved by 'getXXX' method in the specified
     * {@code bean}.
     *
     * @param <T>
     * @param sourceBean a Java Object what allows access to properties using getter
     *            and setter methods.
     * @param selectPropNames
     * @param targetType a Java Object what allows access to properties using getter
     *            and setter methods.
     * @return a new instance of {@code targetType} even if {@code bean} is {@code null}.
     * @throws IllegalArgumentException if {@code targetType} is {@code null}.
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> T copy(final Object sourceBean, final Collection<String> selectPropNames, final Class<? extends T> targetType)
            throws IllegalArgumentException {
        N.checkArgNotNull(targetType, "targetType");

        if (sourceBean != null) {
            final Class<?> srcCls = sourceBean.getClass();

            if (selectPropNames == null && Utils.kryoParser != null && targetType.equals(srcCls) && !notKryoCompatible.contains(srcCls)) {
                try {
                    final T copy = (T) Utils.kryoParser.copy(sourceBean);

                    if (copy != null) {
                        return copy;
                    }
                } catch (Exception e) {
                    notKryoCompatible.add(srcCls);

                    // ignore
                }
            }
        }

        final BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(targetType);
        Object result = targetBeanInfo.createBeanResult();

        if (sourceBean != null) {
            merge(sourceBean, result, selectPropNames, targetBeanInfo);
        }

        result = targetBeanInfo.finishBeanResult(result);

        return (T) result;
    }

    /**
     *
     *
     * @param <T>
     * @param sourceBean
     * @param ignoreUnmatchedProperty
     * @param ignoredPropNames
     * @param targetType
     * @return a new instance of {@code targetType} even if {@code bean} is {@code null}.
     * @throws IllegalArgumentException if {@code targetType} is {@code null}.
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> T copy(final Object sourceBean, final boolean ignoreUnmatchedProperty, final Set<String> ignoredPropNames,
            final Class<? extends T> targetType) throws IllegalArgumentException {
        N.checkArgNotNull(targetType, "targetType");

        if (sourceBean != null) {
            final Class<?> srcCls = sourceBean.getClass();

            if (ignoredPropNames == null && Utils.kryoParser != null && targetType.equals(srcCls) && !notKryoCompatible.contains(srcCls)) {
                try {
                    final T copy = (T) Utils.kryoParser.copy(sourceBean);

                    if (copy != null) {
                        return copy;
                    }
                } catch (Exception e) {
                    notKryoCompatible.add(srcCls);

                    // ignore
                }
            }
        }

        final BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(targetType);
        Object result = targetBeanInfo.createBeanResult();

        if (sourceBean != null) {
            merge(sourceBean, result, ignoreUnmatchedProperty, ignoredPropNames, targetBeanInfo);
        }

        result = targetBeanInfo.finishBeanResult(result);

        return (T) result;
    }

    /**
     * Set all the signed properties(including all primitive type properties) in
     * the specified {@code sourceBean} to the specified {@code targetBean}.
     *
     * @param <T>
     * @param sourceBean a Java Object what allows access to properties using getter
     *            and setter methods.
     * @param propFilter
     * @param targetType
     * @return {@code targetBean}
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}.
     */
    public static <T> T copy(final Object sourceBean, final BiPredicate<String, ?> propFilter, final Class<? extends T> targetType)
            throws IllegalArgumentException {
        return copy(sourceBean, propFilter, Fn.selectFirst(), targetType);
    }

    /**
     *
     *
     * @param <T>
     * @param sourceBean
     * @param mergeFunc the first parameter is source property value, the second parameter is target property value.
     * @param targetType
     * @return {@code targetBean}
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}.
     */
    public static <T> T copy(final Object sourceBean, final BinaryOperator<?> mergeFunc, final Class<? extends T> targetType) throws IllegalArgumentException {
        return copy(sourceBean, BiPredicates.alwaysTrue(), mergeFunc, targetType);
    }

    /**
     * Set all the signed properties(including all primitive type properties) in
     * the specified {@code sourceBean} to the specified {@code targetBean}.
     *
     * @param <T>
     * @param sourceBean a Java Object what allows access to properties using getter
     *            and setter methods.
     * @param selectPropNames
     * @param mergeFunc the first parameter is source property value, the second parameter is target property value.
     * @param targetType
     * @return {@code targetBean}
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}.
     */
    public static <T> T copy(final Object sourceBean, final Collection<String> selectPropNames, final BinaryOperator<?> mergeFunc,
            final Class<? extends T> targetType) throws IllegalArgumentException {
        N.checkArgNotNull(targetType, "targetType");

        final BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(targetType);
        Object result = targetBeanInfo.createBeanResult();

        if (sourceBean != null) {
            merge(sourceBean, result, selectPropNames, mergeFunc);
        }

        result = targetBeanInfo.finishBeanResult(result);

        return (T) result;
    }

    /**
     *
     *
     * @param <T>
     * @param sourceBean
     * @param ignoreUnmatchedProperty
     * @param ignoredPropNames
     * @param mergeFunc the first parameter is source property value, the second parameter is target property value.
     * @param targetType
     * @return {@code targetBean}
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}.
     */
    public static <T> T copy(final Object sourceBean, final boolean ignoreUnmatchedProperty, final Set<String> ignoredPropNames,
            final BinaryOperator<?> mergeFunc, final Class<? extends T> targetType) throws IllegalArgumentException {
        N.checkArgNotNull(targetType, "targetType");

        final BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(targetType);
        Object result = targetBeanInfo.createBeanResult();

        if (sourceBean != null) {
            merge(sourceBean, result, ignoreUnmatchedProperty, ignoredPropNames, mergeFunc);
        }

        result = targetBeanInfo.finishBeanResult(result);

        return (T) result;
    }

    /**
     * Set all the signed properties(including all primitive type properties) in
     * the specified {@code sourceBean} to the specified {@code targetBean}.
     *
     * @param <T>
     * @param sourceBean a Java Object what allows access to properties using getter
     *            and setter methods.
     * @param propFilter
     * @param mergeFunc the first parameter is source property value, the second parameter is target property value.
     * @param targetType
     * @return {@code targetBean}
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}.
     */
    public static <T> T copy(final Object sourceBean, final BiPredicate<String, ?> propFilter, final BinaryOperator<?> mergeFunc,
            final Class<? extends T> targetType) throws IllegalArgumentException {
        N.checkArgNotNull(targetType, "targetType");

        final BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(targetType);
        Object result = targetBeanInfo.createBeanResult();

        if (sourceBean != null) {
            merge(sourceBean, result, propFilter, mergeFunc);
        }

        result = targetBeanInfo.finishBeanResult(result);

        return (T) result;
    }

    /**
     *
     *
     * @param <T>
     * @param sourceBean
     * @param targetBean
     * @return {@code targetBean}
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}.
     */
    public static <T> T merge(final Object sourceBean, final T targetBean) throws IllegalArgumentException {
        return merge(sourceBean, targetBean, (Collection<String>) null);
    }

    /**
     * Set all the signed properties(including all primitive type properties) in
     * the specified {@code sourceBean} to the specified {@code targetBean}.
     *
     * @param <T>
     * @param sourceBean a Java Object what allows access to properties using getter
     *            and setter methods.
     * @param targetBean a Java Object what allows access to properties using getter
     *            and setter methods.
     * @param selectPropNames
     * @return {@code targetBean}
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}.
     */
    public static <T> T merge(final Object sourceBean, final T targetBean, final Collection<String> selectPropNames) throws IllegalArgumentException {
        N.checkArgNotNull(targetBean, "targetBean");

        return merge(sourceBean, targetBean, selectPropNames, ParserUtil.getBeanInfo(targetBean.getClass()));
    }

    private static <T> T merge(final Object sourceBean, final T targetBean, final Collection<String> selectPropNames, final BeanInfo targetBeanInfo)
            throws IllegalArgumentException {
        N.checkArgNotNull(targetBean, "targetBean");

        if (sourceBean == null) {
            return targetBean;
        }

        final BeanInfo srcBeanInfo = ParserUtil.getBeanInfo(sourceBean.getClass());
        final boolean ignoreUnmatchedProperty = selectPropNames == null;

        if (selectPropNames == null) {
            Object propValue = null;

            for (PropInfo propInfo : srcBeanInfo.propInfoList) {
                propValue = propInfo.getPropValue(sourceBean);

                if (notNullOrDefault(propValue)) {
                    targetBeanInfo.setPropValue(targetBean, propInfo, propValue, ignoreUnmatchedProperty);
                }
            }
        } else {
            PropInfo propInfo = null;
            Object propValue = null;

            for (String propName : selectPropNames) {
                propInfo = srcBeanInfo.getPropInfo(propName);
                propValue = propInfo.getPropValue(sourceBean);

                targetBeanInfo.setPropValue(targetBean, propInfo, propValue, ignoreUnmatchedProperty);
            }
        }

        return targetBean;
    }

    /**
     *
     *
     * @param <T>
     * @param sourceBean
     * @param targetBean
     * @param ignoreUnmatchedProperty
     * @param ignoredPropNames
     * @return {@code targetBean}
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}.
     */
    public static <T> T merge(final Object sourceBean, final T targetBean, final boolean ignoreUnmatchedProperty, final Set<String> ignoredPropNames)
            throws IllegalArgumentException {
        N.checkArgNotNull(targetBean, "targetBean");

        if (sourceBean == null) {
            return targetBean;
        }

        return merge(sourceBean, targetBean, ignoreUnmatchedProperty, ignoredPropNames, ParserUtil.getBeanInfo(targetBean.getClass()));
    }

    private static <T> T merge(final Object sourceBean, final T targetBean, final boolean ignoreUnmatchedProperty, final Set<String> ignoredPropNames,
            final BeanInfo targetBeanInfo) throws IllegalArgumentException {
        N.checkArgNotNull(targetBean, "targetBean");

        if (sourceBean == null) {
            return targetBean;
        }

        final BeanInfo srcBeanInfo = ParserUtil.getBeanInfo(sourceBean.getClass());

        Object propValue = null;

        for (PropInfo propInfo : srcBeanInfo.propInfoList) {
            if (ignoredPropNames == null || !ignoredPropNames.contains(propInfo.name)) {
                propValue = propInfo.getPropValue(sourceBean);

                if (notNullOrDefault(propValue)) {
                    targetBeanInfo.setPropValue(targetBean, propInfo, propValue, ignoreUnmatchedProperty);
                }
            }
        }

        return targetBean;
    }

    /**
     * Set all the signed properties(including all primitive type properties) in
     * the specified {@code sourceBean} to the specified {@code targetBean}.
     *
     * @param <T>
     * @param sourceBean a Java Object what allows access to properties using getter
     *            and setter methods.
     * @param targetBean a Java Object what allows access to properties using getter
     *            and setter methods.
     * @param propFilter
     * @return {@code targetBean}
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}.
     */
    public static <T> T merge(final Object sourceBean, final T targetBean, final BiPredicate<String, ?> propFilter) throws IllegalArgumentException {
        return merge(sourceBean, targetBean, propFilter, Fn.selectFirst());
    }

    /**
     *
     *
     * @param <T>
     * @param sourceBean
     * @param targetBean
     * @param mergeFunc the first parameter is source property value, the second parameter is target property value.
     * @return {@code targetBean}
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}.
     */
    public static <T> T merge(final Object sourceBean, final T targetBean, final BinaryOperator<?> mergeFunc) throws IllegalArgumentException {
        return merge(sourceBean, targetBean, BiPredicates.alwaysTrue(), mergeFunc);
    }

    /**
     * Set all the signed properties(including all primitive type properties) in
     * the specified {@code sourceBean} to the specified {@code targetBean}.
     *
     * @param <T>
     * @param sourceBean a Java Object what allows access to properties using getter
     *            and setter methods.
     * @param targetBean a Java Object what allows access to properties using getter
     *            and setter methods.
     * @param selectPropNames
     * @param mergeFunc the first parameter is source property value, the second parameter is target property value.
     * @return {@code targetBean}
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}.
     */
    public static <T> T merge(final Object sourceBean, final T targetBean, final Collection<String> selectPropNames, final BinaryOperator<?> mergeFunc)
            throws IllegalArgumentException {
        N.checkArgNotNull(targetBean, "targetBean");

        if (sourceBean == null) {
            return targetBean;
        }

        final BeanInfo srcBeanInfo = ParserUtil.getBeanInfo(sourceBean.getClass());
        final BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(targetBean.getClass());
        final BinaryOperator<Object> objMergeFunc = (BinaryOperator<Object>) mergeFunc;
        final boolean ignoreUnmatchedProperty = selectPropNames == null;

        if (selectPropNames == null) {
            PropInfo targetPropInfo = null;
            Object propValue = null;

            for (PropInfo propInfo : srcBeanInfo.propInfoList) {
                targetPropInfo = targetBeanInfo.getPropInfo(propInfo);

                if (targetPropInfo == null) {
                    //    if (!ignoreUnmatchedProperty) {
                    //        throw new IllegalArgumentException(
                    //                "No property found by name: " + propInfo.name + " in target bean class: " + targetBean.getClass());
                    //    }
                } else {
                    propValue = propInfo.getPropValue(sourceBean);
                    targetPropInfo.setPropValue(targetBean, objMergeFunc.apply(propValue, targetPropInfo.getPropValue(targetBean)));
                }
            }
        } else {
            PropInfo targetPropInfo = null;
            PropInfo propInfo = null;
            Object propValue = null;

            for (String propName : selectPropNames) {
                propInfo = srcBeanInfo.getPropInfo(propName);
                targetPropInfo = targetBeanInfo.getPropInfo(propInfo);

                if (targetPropInfo == null) {
                    if (!ignoreUnmatchedProperty) { //NOSONAR
                        throw new IllegalArgumentException("No property found by name: " + propName + " in target bean class: " + targetBean.getClass()); //NOSONAR
                    }
                } else {
                    propValue = srcBeanInfo.getPropValue(sourceBean, propName);
                    targetPropInfo.setPropValue(targetBean, objMergeFunc.apply(propValue, targetPropInfo.getPropValue(targetBean)));
                }
            }
        }

        return targetBean;
    }

    /**
     *
     *
     * @param <T>
     * @param sourceBean
     * @param targetBean
     * @param ignoreUnmatchedProperty
     * @param ignoredPropNames
     * @param mergeFunc the first parameter is source property value, the second parameter is target property value.
     * @return {@code targetBean}
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}.
     */
    public static <T> T merge(final Object sourceBean, final T targetBean, final boolean ignoreUnmatchedProperty, final Set<String> ignoredPropNames,
            final BinaryOperator<?> mergeFunc) throws IllegalArgumentException {
        N.checkArgNotNull(targetBean, "targetBean");

        if (sourceBean == null) {
            return targetBean;
        }

        final BeanInfo srcBeanInfo = ParserUtil.getBeanInfo(sourceBean.getClass());
        final BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(targetBean.getClass());
        final BinaryOperator<Object> objMergeFunc = (BinaryOperator<Object>) mergeFunc;

        PropInfo targetPropInfo = null;
        Object propValue = null;

        for (PropInfo propInfo : srcBeanInfo.propInfoList) {
            if (ignoredPropNames == null || !ignoredPropNames.contains(propInfo.name)) {
                targetPropInfo = targetBeanInfo.getPropInfo(propInfo);

                if (targetPropInfo == null) {
                    if (!ignoreUnmatchedProperty) {
                        throw new IllegalArgumentException("No property found by name: " + propInfo.name + " in target bean class: " + targetBean.getClass());
                    }
                } else {
                    propValue = propInfo.getPropValue(sourceBean);
                    targetPropInfo.setPropValue(targetBean, objMergeFunc.apply(propValue, targetPropInfo.getPropValue(targetBean)));
                }
            }
        }

        return targetBean;
    }

    /**
     * Set all the signed properties(including all primitive type properties) in
     * the specified {@code sourceBean} to the specified {@code targetBean}.
     *
     * @param <T>
     * @param sourceBean a Java Object what allows access to properties using getter
     *            and setter methods.
     * @param targetBean a Java Object what allows access to properties using getter
     *            and setter methods.
     * @param propFilter
     * @param mergeFunc the first parameter is source property value, the second parameter is target property value.
     * @return {@code targetBean}
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}.
     */
    public static <T> T merge(final Object sourceBean, final T targetBean, final BiPredicate<String, ?> propFilter, final BinaryOperator<?> mergeFunc)
            throws IllegalArgumentException {
        N.checkArgNotNull(targetBean, "targetBean");

        if (sourceBean == null) {
            return targetBean;
        }

        final BeanInfo srcBeanInfo = ParserUtil.getBeanInfo(sourceBean.getClass());
        final BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(targetBean.getClass());
        final BiPredicate<String, Object> objFilter = (BiPredicate<String, Object>) propFilter;
        final BinaryOperator<Object> objMergeFunc = (BinaryOperator<Object>) mergeFunc;

        Object propValue = null;
        PropInfo targetPropInfo = null;

        for (PropInfo propInfo : srcBeanInfo.propInfoList) {
            propValue = propInfo.getPropValue(sourceBean);

            if (objFilter.test(propInfo.name, propValue)) {
                targetPropInfo = targetBeanInfo.getPropInfo(propInfo);

                if (targetPropInfo == null) {
                    throw new IllegalArgumentException("No property found by name: " + propInfo.name + " in target bean class: " + targetBean.getClass());
                }

                targetPropInfo.setPropValue(targetBean, objMergeFunc.apply(propValue, targetPropInfo.getPropValue(targetBean)));
            }
        }

        return targetBean;
    }

    /**
     *
     * @param bean
     * @param propNames
     */
    @SafeVarargs
    public static void erase(final Object bean, final String... propNames) {
        if (bean == null || isEmpty(propNames)) {
            return;
        }

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(bean.getClass());

        for (String propName : propNames) {
            beanInfo.setPropValue(bean, propName, null);
        }
    }

    /**
     *
     * @param bean
     * @param propNames
     */
    public static void erase(final Object bean, final Collection<String> propNames) {
        if (bean == null || isEmpty(propNames)) {
            return;
        }

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(bean.getClass());

        for (String propName : propNames) {
            beanInfo.setPropValue(bean, propName, null);
        }
    }

    /**
     *
     * @param bean
     */
    public static void eraseAll(final Object bean) {
        if (bean == null) {
            return;
        }

        final Class<?> cls = bean.getClass();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(cls);

        for (PropInfo propInfo : beanInfo.propInfoList) {
            propInfo.setPropValue(bean, null);
        }
    }

    /**
     * Enum list of.
     *
     * @param <E>
     * @param enumClass
     * @return
     */
    public static <E extends Enum<E>> ImmutableList<E> enumListOf(final Class<E> enumClass) {
        ImmutableList<E> enumList = (ImmutableList<E>) enumListPool.get(enumClass);

        if (enumList == null) {
            enumList = ImmutableList.wrap(asList(enumClass.getEnumConstants()));

            enumListPool.put(enumClass, enumList);
        }

        return enumList;
    }

    /**
     * Enum set of.
     *
     * @param <E>
     * @param enumClass
     * @return
     */
    public static <E extends Enum<E>> ImmutableSet<E> enumSetOf(final Class<E> enumClass) {
        ImmutableSet<E> enumSet = (ImmutableSet<E>) enumSetPool.get(enumClass);

        if (enumSet == null) {
            enumSet = ImmutableSet.wrap(EnumSet.allOf(enumClass));

            enumSetPool.put(enumClass, enumSet);
        }

        return enumSet;
    }

    /**
     * Enum map of.
     *
     * @param <E>
     * @param enumClass
     * @return
     */
    public static <E extends Enum<E>> ImmutableBiMap<E, String> enumMapOf(final Class<E> enumClass) {
        ImmutableBiMap<E, String> enumMap = (ImmutableBiMap<E, String>) enumMapPool.get(enumClass);

        if (enumMap == null) {
            final EnumMap<E, String> keyMap = new EnumMap<>(enumClass);
            final Map<String, E> valueMap = new HashMap<>();

            for (final E e : enumClass.getEnumConstants()) {
                keyMap.put(e, e.name());
                valueMap.put(e.name(), e);
            }

            enumMap = ImmutableBiMap.wrap(new BiMap<>(keyMap, valueMap));

            enumMapPool.put(enumClass, enumMap);
        }

        return enumMap;
    }

    /**
     *
     * @param <T>
     * @param cls
     * @return T
     * @see Suppliers#ofCollection(Class)
     * @see Suppliers#registerForCollection(Class, java.util.function.Supplier)
     * @see Suppliers#ofMap(Class)
     * @see Suppliers#registerForMap(Class, java.util.function.Supplier)
     * @see IntFunctions#ofCollection(Class)
     * @see IntFunctions#registerForCollection(Class, java.util.function.IntFunction)
     * @see IntFunctions#ofMap(Class)
     * @see IntFunctions#registerForMap(Class, java.util.function.IntFunction)
     */
    @SuppressWarnings("rawtypes")
    public static <T> T newInstance(final Class<T> cls) {
        if (Modifier.isAbstract(cls.getModifiers())) {
            if (Collection.class.isAssignableFrom(cls)) {
                return (T) Suppliers.ofCollection((Class<Collection>) cls).get();
            } else if (Map.class.isAssignableFrom(cls)) {
                return (T) Suppliers.ofMap((Class<Map>) cls).get();
            } else {
                throw new IllegalArgumentException("Can't create instance for abstract class: " + cls);
            }
        }

        if (!Modifier.isStatic(cls.getModifiers()) && ClassUtil.isAnonymousOrMemeberClass(cls)) {
            try {
                // http://stackoverflow.com/questions/2097982/is-it-possible-to-create-an-instance-of-nested-class-using-java-reflection

                final List<Class<?>> toInstantiate = new ArrayList<>();
                Class<?> parent = cls.getEnclosingClass();

                do {
                    toInstantiate.add(parent);
                    parent = parent.getEnclosingClass();
                } while (parent != null && !Modifier.isStatic(parent.getModifiers()) && ClassUtil.isAnonymousOrMemeberClass(parent));

                if (parent != null) {
                    toInstantiate.add(parent);
                }

                reverse(toInstantiate);

                Object instance = null;
                for (Class<?> current : toInstantiate) {
                    instance = instance == null ? invoke(ClassUtil.getDeclaredConstructor(current))
                            : invoke(ClassUtil.getDeclaredConstructor(current, instance.getClass()), instance);
                }

                return invoke(ClassUtil.getDeclaredConstructor(cls, instance.getClass()), instance);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw ExceptionUtil.toRuntimeException(e);
            }
        } else {
            try {
                final Constructor<T> constructor = ClassUtil.getDeclaredConstructor(cls);

                if (constructor == null) {
                    throw new IllegalArgumentException("No default constructor found in class: " + ClassUtil.getCanonicalClassName(cls));
                }

                return invoke(constructor);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw ExceptionUtil.toRuntimeException(e);
            }
        }
    }

    /**
     *
     * @param <T>
     * @param c
     * @param args
     * @return
     * @throws InstantiationException the instantiation exception
     * @throws IllegalAccessException the illegal access exception
     * @throws IllegalArgumentException the illegal argument exception
     * @throws InvocationTargetException the invocation target exception
     */
    @SuppressWarnings({ "unchecked", "deprecation" })
    static <T> T invoke(final Constructor<T> c, final Object... args)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (!c.isAccessible()) {
            ClassUtil.setAccessibleQuietly(c, true);
        }

        return c.newInstance(args);
    }

    /**
     * New proxy instance.
     *
     * @param <T>
     * @param interfaceClass
     * @param h
     * @return
     */
    public static <T> T newProxyInstance(final Class<T> interfaceClass, final InvocationHandler h) {
        return newProxyInstance(asArray(interfaceClass), h);
    }

    /**
     * Refer to {@code java.lang.reflect}
     *
     * @param <T>
     * @param interfaceClasses
     * @param h
     * @return
     */
    public static <T> T newProxyInstance(final Class<?>[] interfaceClasses, final InvocationHandler h) {
        return (T) Proxy.newProxyInstance(CommonUtil.class.getClassLoader(), interfaceClasses, h); // NOSONAR
    }

    /**
     *
     * @param <T>
     * @param targetType
     * @return
     * @see Suppliers#ofCollection(Class)
     * @see Suppliers#registerForCollection(Class, java.util.function.Supplier)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collection<T> newCollection(final Class<? extends Collection> targetType) {
        return Suppliers.<T> ofCollection(targetType).get();
    }

    /**
     *
     * @param <T>
     * @param targetType
     * @param size
     * @return
     * @see IntFunctions#ofCollection(Class)
     * @see IntFunctions#registerForCollection(Class, java.util.function.IntFunction)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Collection<T> newCollection(final Class<? extends Collection> targetType, final int size) {
        return IntFunctions.<T> ofCollection(targetType).apply(size);
    }

    /**
     *
     * @param <K>
     * @param <V>
     * @param targetType
     * @return
     * @see Suppliers#ofMap(Class)
     * @see Suppliers#registerForMap(Class, java.util.function.Supplier)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Map<K, V> newMap(final Class<? extends Map> targetType) {
        return Suppliers.<K, V> ofMap(targetType).get();
    }

    /**
     *
     * @param <K>
     * @param <V>
     * @param targetType
     * @param size
     * @return
     * @see IntFunctions#ofMap(Class)
     * @see IntFunctions#registerForMap(Class, java.util.function.IntFunction)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Map<K, V> newMap(final Class<? extends Map> targetType, final int size) {
        return IntFunctions.<K, V> ofMap(targetType).apply(size);
    }

    /**
     *
     * @param <T>
     * @param componentType
     * @param length
     * @return T[]
     * @see Array#newInstance(Class, int)
     */
    @SuppressWarnings("unchecked")
    public static <T> T newArray(final Class<?> componentType, final int length) {
        return (T) Array.newInstance(componentType, length);
    }

    /**
     *
     * @param <T>
     * @param componentType
     * @param dimensions
     * @return
     * @throws IllegalArgumentException
     * @throws NegativeArraySizeException
     * @see Array#newInstance(Class, int...)
     */
    @SafeVarargs
    public static <T> T newArray(final Class<?> componentType, final int... dimensions) throws IllegalArgumentException, NegativeArraySizeException {
        return (T) Array.newInstance(componentType, dimensions);
    }

    /**
     *
     * @param <T>
     * @param targetType
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T newBean(final Class<T> targetType) {
        return newBean(targetType, null);
    }

    /**
     *
     * @param <T>
     * @param targetType
     * @param beanName
     * @return
     */
    public static <T> T newBean(final Class<T> targetType, final String beanName) {
        if (MapEntity.class.isAssignableFrom(targetType)) {
            return (T) new MapEntity(beanName);
        }

        return newInstance(targetType);
    }

    /**
     * Returns a set backed by the specified map.
     *
     * @param <E>
     * @param map the backing map
     * @return
     * @see Collections#newSetFromMap(Map)
     */
    public static <E> Set<E> newSetFromMap(final Map<E, Boolean> map) {
        return Collections.newSetFromMap(map);
    }

    /**
     * Inits the hash capacity.
     *
     * @param size
     * @return
     * @deprecated warning...
     */
    @Deprecated
    @Internal
    @Beta
    static int initHashCapacity(final int size) {
        checkArgNotNegative(size, "size");

        if (size == 0) {
            return 0;
        }

        int res = size < MAX_HASH_LENGTH ? (int) (size * 1.25) + 1 : MAX_ARRAY_SIZE;

        return res >= 1024 ? res : (res >= 256 ? 256 : res);
    }

    /**
     * New array list.
     *
     * @param <T>
     * @return
     */
    public static <T> ArrayList<T> newArrayList() { //NOSONAR
        return new ArrayList<>();
    }

    /**
     * New array list.
     *
     * @param <T>
     * @param initialCapacity
     * @return
     */
    public static <T> ArrayList<T> newArrayList(int initialCapacity) { //NOSONAR
        return new ArrayList<>(initialCapacity);
    }

    /**
     * New array list.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> ArrayList<T> newArrayList(Collection<? extends T> c) { //NOSONAR
        return isEmpty(c) ? new ArrayList<>() : new ArrayList<>(c);
    }

    /**
     * New linked list.
     *
     * @param <T>
     * @return
     */
    public static <T> LinkedList<T> newLinkedList() { //NOSONAR
        return new LinkedList<>();
    }

    /**
     * New linked list.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> LinkedList<T> newLinkedList(Collection<? extends T> c) { //NOSONAR
        return isEmpty(c) ? new LinkedList<>() : new LinkedList<>(c);
    }

    /**
     * New hash set.
     *
     * @param <T>
     * @return
     */
    public static <T> Set<T> newHashSet() {
        return new HashSet<>();
    }

    /**
     * New hash set.
     *
     * @param <T>
     * @param initialCapacity
     * @return
     */
    public static <T> Set<T> newHashSet(int initialCapacity) {
        return new HashSet<>(initHashCapacity(initialCapacity));
    }

    /**
     * New hash set.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> Set<T> newHashSet(Collection<? extends T> c) {
        return isEmpty(c) ? new HashSet<>() : new HashSet<>(c);
    }

    /**
     * New linked hash set.
     *
     * @param <T>
     * @return
     */
    public static <T> Set<T> newLinkedHashSet() {
        return new LinkedHashSet<>();
    }

    /**
     * New linked hash set.
     *
     * @param <T>
     * @param initialCapacity
     * @return
     */
    public static <T> Set<T> newLinkedHashSet(int initialCapacity) {
        return new LinkedHashSet<>(initHashCapacity(initialCapacity));
    }

    /**
     * New linked hash set.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> Set<T> newLinkedHashSet(Collection<? extends T> c) {
        return isEmpty(c) ? new LinkedHashSet<>() : new LinkedHashSet<>(c);
    }

    /**
     * New tree set.
     *
     * @param <T>
     * @return
     */
    public static <T extends Comparable<? super T>> TreeSet<T> newTreeSet() { //NOSONAR
        return new TreeSet<>();
    }

    /**
     * New tree set.
     *
     * @param <T>
     * @param comparator
     * @return
     */
    public static <T> TreeSet<T> newTreeSet(Comparator<? super T> comparator) { //NOSONAR
        return new TreeSet<>(comparator);
    }

    /**
     * New tree set.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Comparable<? super T>> TreeSet<T> newTreeSet(Collection<? extends T> c) { //NOSONAR
        return isEmpty(c) ? new TreeSet<>() : new TreeSet<>(c);
    }

    /**
     * New tree set.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> TreeSet<T> newTreeSet(SortedSet<T> c) { //NOSONAR
        return isEmpty(c) ? new TreeSet<>() : new TreeSet<>(c);
    }

    /**
     * New hash set.
     *
     * @param <T>
     * @return
     */
    public static <T> ConcurrentHashSet<T> newConcurrentHashSet() {
        return new ConcurrentHashSet<>();
    }

    /**
     * New hash set.
     *
     * @param <T>
     * @param initialCapacity
     * @return
     */
    public static <T> ConcurrentHashSet<T> newConcurrentHashSet(int initialCapacity) {
        return new ConcurrentHashSet<>(initHashCapacity(initialCapacity));
    }

    /**
     * New hash set.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> ConcurrentHashSet<T> newConcurrentHashSet(Collection<? extends T> c) {
        return isEmpty(c) ? new ConcurrentHashSet<>() : new ConcurrentHashSet<>(c);
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> Multiset<T> newMultiset() {
        return new Multiset<>();
    }

    /**
     *
     * @param <T>
     * @param initialCapacity
     * @return
     */
    public static <T> Multiset<T> newMultiset(final int initialCapacity) {
        return new Multiset<>(initialCapacity);
    }

    /**
     *
     * @param <T>
     * @param valueMapType
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Multiset<T> newMultiset(final Class<? extends Map> valueMapType) {
        return new Multiset<>(valueMapType);
    }

    /**
     *
     * @param <T>
     * @param mapSupplier
     * @return
     */
    public static <T> Multiset<T> newMultiset(final Supplier<? extends Map<T, ?>> mapSupplier) {
        return new Multiset<>(mapSupplier);
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> Multiset<T> newMultiset(final Collection<? extends T> c) {
        return new Multiset<>(c);
    }

    /**
     * New array deque.
     *
     * @param <T>
     * @return
     */
    public static <T> ArrayDeque<T> newArrayDeque() { //NOSONAR
        return new ArrayDeque<>();
    }

    /**
     * Constructs an empty array deque with an initial capacity sufficient to hold the specified number of elements.
     *
     * @param <T>
     * @param numElements lower bound on initial capacity of the deque.
     * @return
     */
    public static <T> ArrayDeque<T> newArrayDeque(int numElements) { //NOSONAR
        return new ArrayDeque<>(numElements);
    }

    /**
     * Constructs a deque containing the elements of the specified collection, in the order they are returned by the collection's iterator.
     *
     * @param <E>
     * @param c
     * @return
     */
    public static <E> ArrayDeque<E> newArrayDeque(Collection<? extends E> c) { //NOSONAR
        return new ArrayDeque<>(c);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param key
     * @param value
     * @return
     */
    public static <K, V> Map.Entry<K, V> newEntry(K key, V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    /**
     * New immutable entry.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param key
     * @param value
     * @return
     */
    public static <K, V> ImmutableEntry<K, V> newImmutableEntry(K key, V value) {
        return new ImmutableEntry<>(key, value);
    }

    /**
     * New hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    public static <K, V> Map<K, V> newHashMap() {
        return new HashMap<>();
    }

    /**
     * New hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param initialCapacity
     * @return
     */
    public static <K, V> Map<K, V> newHashMap(int initialCapacity) {
        return new HashMap<>(initHashCapacity(initialCapacity));
    }

    /**
     * New hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param m
     * @return
     */
    public static <K, V> Map<K, V> newHashMap(Map<? extends K, ? extends V> m) {
        return isEmpty(m) ? new HashMap<>() : new HashMap<>(m);
    }

    /**
     * New hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <E>
     * @param c
     * @param keyMapper
     * @return
     * @throws IllegalArgumentException
     * @throws E the e
     */
    public static <K, V, E extends Exception> Map<K, V> newHashMap(final Collection<? extends V> c,
            final Throwables.Function<? super V, ? extends K, E> keyMapper) throws IllegalArgumentException, E {
        checkArgNotNull(keyMapper);

        if (isEmpty(c)) {
            return new HashMap<>();
        }

        final Map<K, V> result = N.newHashMap(c.size());

        for (V v : c) {
            result.put(keyMapper.apply(v), v);
        }

        return result;
    }

    /**
     * New linked hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    public static <K, V> Map<K, V> newLinkedHashMap() {
        return new LinkedHashMap<>();
    }

    /**
     * New linked hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param initialCapacity
     * @return
     */
    public static <K, V> Map<K, V> newLinkedHashMap(int initialCapacity) {
        return new LinkedHashMap<>(initHashCapacity(initialCapacity));
    }

    /**
     * New linked hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param m
     * @return
     */
    public static <K, V> Map<K, V> newLinkedHashMap(Map<? extends K, ? extends V> m) {
        return isEmpty(m) ? new LinkedHashMap<>() : new LinkedHashMap<>(m);
    }

    /**
     * New linked hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <E>
     * @param c
     * @param keyMapper
     * @return
     * @throws IllegalArgumentException
     * @throws E the e
     */
    public static <K, V, E extends Exception> Map<K, V> newLinkedHashMap(final Collection<? extends V> c,
            final Throwables.Function<? super V, ? extends K, E> keyMapper) throws IllegalArgumentException, E {
        checkArgNotNull(keyMapper);

        if (isEmpty(c)) {
            return N.newLinkedHashMap();
        }

        final Map<K, V> result = N.newLinkedHashMap(c.size());

        for (V v : c) {
            result.put(keyMapper.apply(v), v);
        }

        return result;
    }

    /**
     * New tree map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    public static <K extends Comparable<? super K>, V> TreeMap<K, V> newTreeMap() { //NOSONAR
        return new TreeMap<>();
    }

    /**
     * New tree map.
     *
     * @param <C>
     * @param <K> the key type
     * @param <V> the value type
     * @param comparator
     * @return
     */
    public static <C, K extends C, V> TreeMap<K, V> newTreeMap(Comparator<C> comparator) { //NOSONAR
        return new TreeMap<>(comparator);
    }

    /**
     * New tree map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param m
     * @return
     */
    public static <K extends Comparable<? super K>, V> TreeMap<K, V> newTreeMap(Map<? extends K, ? extends V> m) { //NOSONAR
        return isEmpty(m) ? new TreeMap<>() : new TreeMap<>(m);
    }

    /**
     * New tree map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param m
     * @return
     */
    public static <K, V> TreeMap<K, V> newTreeMap(SortedMap<K, ? extends V> m) { //NOSONAR
        return isEmpty(m) ? new TreeMap<>() : new TreeMap<>(m);
    }

    /**
     * New identity hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    public static <K, V> IdentityHashMap<K, V> newIdentityHashMap() { //NOSONAR
        return new IdentityHashMap<>();
    }

    /**
     * New identity hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param initialCapacity
     * @return
     */
    public static <K, V> IdentityHashMap<K, V> newIdentityHashMap(int initialCapacity) { //NOSONAR
        return new IdentityHashMap<>(initHashCapacity(initialCapacity));
    }

    /**
     * New identity hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param m
     * @return
     */
    public static <K, V> IdentityHashMap<K, V> newIdentityHashMap(Map<? extends K, ? extends V> m) { //NOSONAR
        return isEmpty(m) ? new IdentityHashMap<>() : new IdentityHashMap<>(m);
    }

    /**
     * New concurrent hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap() { //NOSONAR
        return new ConcurrentHashMap<>();
    }

    /**
     * New concurrent hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param initialCapacity
     * @return
     */
    public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap(int initialCapacity) { //NOSONAR
        return new ConcurrentHashMap<>(initHashCapacity(initialCapacity));
    }

    /**
     * New concurrent hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param m
     * @return
     */
    public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap(Map<? extends K, ? extends V> m) { //NOSONAR
        return isEmpty(m) ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(m);
    }

    /**
     * New bi map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    public static <K, V> BiMap<K, V> newBiMap() {
        return new BiMap<>();
    }

    /**
     * New bi map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param initialCapacity
     * @return
     */
    public static <K, V> BiMap<K, V> newBiMap(int initialCapacity) {
        return new BiMap<>(initialCapacity);
    }

    /**
     * New bi map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param initialCapacity
     * @param loadFactor
     * @return
     */
    public static <K, V> BiMap<K, V> newBiMap(int initialCapacity, float loadFactor) {
        return new BiMap<>(initialCapacity, loadFactor);
    }

    /**
     * New bi map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyMapType
     * @param valueMapType
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> BiMap<K, V> newBiMap(final Class<? extends Map> keyMapType, final Class<? extends Map> valueMapType) {
        return new BiMap<>(keyMapType, valueMapType);
    }

    /**
     * New bi map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyMapSupplier
     * @param valueMapSupplier
     * @return
     */
    public static <K, V> BiMap<K, V> newBiMap(final Supplier<? extends Map<K, V>> keyMapSupplier, final Supplier<? extends Map<V, K>> valueMapSupplier) {
        return new BiMap<>(keyMapSupplier, valueMapSupplier);
    }

    /**
     *
     * @param <K> the key type
     * @param <E>
     * @param <V> the value type
     * @param mapSupplier
     * @param valueSupplier
     * @return
     */
    public static <K, E, V extends Collection<E>> Multimap<K, E, V> newMultimap(final Supplier<? extends Map<K, V>> mapSupplier,
            final Supplier<? extends V> valueSupplier) {
        return new Multimap<>(mapSupplier, valueSupplier);
    }

    /**
     * New list multimap.
     *
     * @param <K> the key type
     * @param <E>
     * @return
     */
    public static <K, E> ListMultimap<K, E> newListMultimap() {
        return new ListMultimap<>();
    }

    /**
     * New list multimap.
     *
     * @param <K> the key type
     * @param <E>
     * @param initialCapacity
     * @return
     */
    public static <K, E> ListMultimap<K, E> newListMultimap(final int initialCapacity) {
        return new ListMultimap<>(initialCapacity);
    }

    /**
     * New list multimap.
     *
     * @param <K> the key type
     * @param <E>
     * @param mapType
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <K, E> ListMultimap<K, E> newListMultimap(final Class<? extends Map> mapType) {
        return new ListMultimap<>(mapType, ArrayList.class);
    }

    /**
     * New list multimap.
     *
     * @param <K> the key type
     * @param <E>
     * @param mapType
     * @param valueType
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <K, E> ListMultimap<K, E> newListMultimap(final Class<? extends Map> mapType, final Class<? extends List> valueType) {
        return new ListMultimap<>(mapType, valueType);
    }

    /**
     * New list multimap.
     *
     * @param <K> the key type
     * @param <E>
     * @param mapSupplier
     * @param valueSupplier
     * @return
     */
    public static <K, E> ListMultimap<K, E> newListMultimap(final Supplier<? extends Map<K, List<E>>> mapSupplier,
            final Supplier<? extends List<E>> valueSupplier) {
        return new ListMultimap<>(mapSupplier, valueSupplier);
    }

    /**
     * New list multimap.
     *
     * @param <K> the key type
     * @param <E>
     * @param m
     * @return
     */
    public static <K, E> ListMultimap<K, E> newListMultimap(final Map<? extends K, ? extends E> m) {
        final ListMultimap<K, E> multiMap = newListMultimap(size(m));

        multiMap.put(m);

        return multiMap;
    }

    /**
     *
     * @param <T>
     * @param <K> the key type
     * @param c
     * @param keyMapper
     * @return
     */
    public static <T, K> ListMultimap<K, T> newListMultimap(final Collection<? extends T> c, final Function<? super T, ? extends K> keyMapper) {
        return ListMultimap.create(c, keyMapper);
    }

    /**
     *
     * @param <T>
     * @param <K> the key type
     * @param <E>
     * @param c
     * @param keyMapper
     * @param valueExtractor
     * @return
     */
    public static <T, K, E> ListMultimap<K, E> newListMultimap(final Collection<? extends T> c, final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends E> valueExtractor) {
        return ListMultimap.create(c, keyMapper, valueExtractor);
    }

    /**
     * Return a {@code ListMultimap} backed by {@code LinkedHashMap}. {@code 'Linked'} is about the map, not the value.
     *
     * @param <K> the key type
     * @param <E>
     * @return
     */
    public static <K, E> ListMultimap<K, E> newLinkedListMultimap() {
        return new ListMultimap<>(LinkedHashMap.class, ArrayList.class);
    }

    /**
     * Return a {@code ListMultimap} backed by {@code LinkedHashMap}. {@code 'Linked'} is about the map, not the value.
     *
     * @param <K> the key type
     * @param <E>
     * @param initialCapacity
     * @return
     */
    public static <K, E> ListMultimap<K, E> newLinkedListMultimap(final int initialCapacity) {
        return new ListMultimap<>(N.<K, List<E>> newLinkedHashMap(initialCapacity), ArrayList.class);
    }

    /**
     * Return a {@code ListMultimap} backed by {@code LinkedHashMap}. {@code 'Linked'} is about the map, not the value.
     *
     * @param <K> the key type
     * @param <E>
     * @param m
     * @return
     */
    public static <K, E> ListMultimap<K, E> newLinkedListMultimap(final Map<? extends K, ? extends E> m) {
        final ListMultimap<K, E> multiMap = new ListMultimap<>(N.<K, List<E>> newLinkedHashMap(N.size(m)), ArrayList.class);

        multiMap.put(m);

        return multiMap;
    }

    /**
     * Return a {@code ListMultimap} backed by {@code SortedMap}. {@code 'Sorted'} is about the map, not the value.
     *
     * @param <K> the key type
     * @param <E>
     * @return
     */
    public static <K extends Comparable<? super K>, E> ListMultimap<K, E> newSortedListMultimap() {
        return new ListMultimap<>(new TreeMap<>(), ArrayList.class);
    }

    /**
     * Return a {@code ListMultimap} backed by {@code SortedMap}. {@code 'Sorted'} is about the map, not the value.
     *
     * @param <K> the key type
     * @param <E>
     * @param m
     * @return
     */
    public static <K extends Comparable<? super K>, E> ListMultimap<K, E> newSortedListMultimap(final Map<? extends K, ? extends E> m) {
        final ListMultimap<K, E> multiMap = new ListMultimap<>(new TreeMap<>(), ArrayList.class);

        multiMap.put(m);

        return multiMap;
    }

    /**
     * New set multimap.
     *
     * @param <K> the key type
     * @param <E>
     * @return
     */
    public static <K, E> SetMultimap<K, E> newSetMultimap() {
        return new SetMultimap<>();
    }

    /**
     * New set multimap.
     *
     * @param <K> the key type
     * @param <E>
     * @param initialCapacity
     * @return
     */
    public static <K, E> SetMultimap<K, E> newSetMultimap(final int initialCapacity) {
        return new SetMultimap<>(initialCapacity);
    }

    /**
     * New set multimap.
     *
     * @param <K> the key type
     * @param <E>
     * @param mapType
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <K, E> SetMultimap<K, E> newSetMultimap(final Class<? extends Map> mapType) {
        return new SetMultimap<>(mapType, HashSet.class);
    }

    /**
     * New set multimap.
     *
     * @param <K> the key type
     * @param <E>
     * @param mapType
     * @param valueType
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <K, E> SetMultimap<K, E> newSetMultimap(final Class<? extends Map> mapType, final Class<? extends Set> valueType) {
        return new SetMultimap<>(mapType, valueType);
    }

    /**
     * New set multimap.
     *
     * @param <K> the key type
     * @param <E>
     * @param mapSupplier
     * @param valueSupplier
     * @return
     */
    public static <K, E> SetMultimap<K, E> newSetMultimap(final Supplier<? extends Map<K, Set<E>>> mapSupplier,
            final Supplier<? extends Set<E>> valueSupplier) {
        return new SetMultimap<>(mapSupplier, valueSupplier);
    }

    /**
     * New set multimap.
     *
     * @param <K> the key type
     * @param <E>
     * @param m
     * @return
     */
    public static <K, E> SetMultimap<K, E> newSetMultimap(final Map<? extends K, ? extends E> m) {
        final SetMultimap<K, E> multiMap = newSetMultimap(N.size(m));

        multiMap.put(m);

        return multiMap;
    }

    /**
     *
     * @param <T>
     * @param <K> the key type
     * @param c
     * @param keyMapper
     * @return
     */
    public static <T, K> SetMultimap<K, T> newSetMultimap(final Collection<? extends T> c, final Function<? super T, ? extends K> keyMapper) {
        return SetMultimap.create(c, keyMapper);
    }

    /**
     *
     * @param <T>
     * @param <K> the key type
     * @param <E>
     * @param c
     * @param keyMapper
     * @param valueExtractor
     * @return
     */
    public static <T, K, E> SetMultimap<K, E> newSetMultimap(final Collection<? extends T> c, final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends E> valueExtractor) {
        return SetMultimap.create(c, keyMapper, valueExtractor);
    }

    /**
     * Return a {@code SetMultimap} backed by {@code LinkedHashMap}. {@code 'Linked'} is about the map, not the value.
     *
     * @param <K> the key type
     * @param <E>
     * @return
     */
    public static <K, E> SetMultimap<K, E> newLinkedSetMultimap() {
        return new SetMultimap<>(LinkedHashMap.class, HashSet.class);
    }

    /**
     * Return a {@code SetMultimap} backed by {@code LinkedHashMap}. {@code 'Linked'} is about the map, not the value.
     *
     * @param <K> the key type
     * @param <E>
     * @param initialCapacity
     * @return
     */
    public static <K, E> SetMultimap<K, E> newLinkedSetMultimap(final int initialCapacity) {
        return new SetMultimap<>(N.<K, Set<E>> newLinkedHashMap(initialCapacity), HashSet.class);
    }

    /**
     * Return a {@code SetMultimap} backed by {@code LinkedHashMap}. {@code 'Linked'} is about the map, not the value.
     *
     * @param <K> the key type
     * @param <E>
     * @param m
     * @return
     */
    public static <K, E> SetMultimap<K, E> newLinkedSetMultimap(final Map<? extends K, ? extends E> m) {
        final SetMultimap<K, E> multiMap = new SetMultimap<>(N.<K, Set<E>> newLinkedHashMap(N.size(m)), HashSet.class);

        multiMap.put(m);

        return multiMap;
    }

    /**
     * Return a {@code SetMultimap} backed by {@code SortedMap}. {@code 'Sorted'} is about the map, not the value.
     *
     *
     * @param <K> the key type
     * @param <E>
     * @return
     */
    public static <K extends Comparable<? super K>, E> SetMultimap<K, E> newSortedSetMultimap() {
        return new SetMultimap<>(new TreeMap<>(), HashSet.class);
    }

    /**
     * Return a {@code SetMultimap} backed by {@code SortedMap}. {@code 'Sorted'} is about the map, not the value.
     *
     * @param <K> the key type
     * @param <E>
     * @param m
     * @return
     */
    public static <K extends Comparable<? super K>, E> SetMultimap<K, E> newSortedSetMultimap(final Map<? extends K, ? extends E> m) {
        final SetMultimap<K, E> multiMap = new SetMultimap<>(new TreeMap<>(), HashSet.class);

        multiMap.put(m);

        return multiMap;
    }

    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    static final int MAX_HASH_LENGTH = (int) (MAX_ARRAY_SIZE / 1.25) - 1;

    /**
     * New empty data set.
     *
     * @return
     */
    public static DataSet newEmptyDataSet() {
        return new RowDataSet(new ArrayList<>(), new ArrayList<>());
    }

    /**
     * New empty data set.
     *
     * @param columnNames
     * @return
     */
    public static DataSet newEmptyDataSet(final Collection<String> columnNames) {
        if (isEmpty(columnNames)) {
            return newEmptyDataSet();
        }

        final List<List<Object>> columnList = new ArrayList<>(columnNames.size());

        for (int i = 0, size = columnNames.size(); i < size; i++) {
            columnList.add(new ArrayList<>(0));
        }

        return new RowDataSet(new ArrayList<>(columnNames), columnList);
    }

    /**
     * Convert the specified Map to a two columns <code>DataSet</code>: one column is for keys and one column is for values.
     *
     * @param keyColumnName
     * @param valueColumnName
     * @param m
     * @return
     */
    public static DataSet newDataSet(final String keyColumnName, final String valueColumnName, final Map<?, ?> m) {
        final List<Object> keyColumn = new ArrayList<>(m.size());
        final List<Object> valueColumn = new ArrayList<>(m.size());

        for (Map.Entry<?, ?> entry : m.entrySet()) {
            keyColumn.add(entry.getKey());
            valueColumn.add(entry.getValue());
        }

        final List<String> columnNameList = asList(keyColumnName, valueColumnName);
        final List<List<Object>> columnList = asList(keyColumn, valueColumn);

        return newDataSet(columnNameList, columnList);
    }

    /**
     * The first row will be used as column names if its type is array or list,
     * or obtain the column names from first row if its type is bean or map.
     *
     * @param rowList list of row which can be: Map/Bean/Array/List
     * @return
     */
    public static DataSet newDataSet(final Collection<?> rowList) {
        return newDataSet(null, rowList);
    }

    /**
     * If the specified {@code columnNames} is null or empty, the first row will be used as column names if its type is array or list,
     * or obtain the column names from first row if its type is bean or map.
     *
     * @param columnNames
     * @param rowList
     * @return
     */
    public static DataSet newDataSet(Collection<String> columnNames, Collection<?> rowList) {
        if (isEmpty(columnNames) && isEmpty(rowList)) {
            // throw new IllegalArgumentException("Column name list and row list can not be both null or empty");
            return newEmptyDataSet();
        } else if (isEmpty(rowList)) {
            return newEmptyDataSet(columnNames);
        }

        int startRowIndex = 0;

        if (isEmpty(columnNames)) {
            final Object firstNonNullRow = N.firstNonNull(rowList).orElse(null);

            if (firstNonNullRow == null) {
                return newEmptyDataSet();
            }

            final Class<?> cls = firstNonNullRow.getClass();
            final Type<?> type = typeOf(cls);

            if (type.isMap()) {
                columnNames = new ArrayList<>(((Map<String, Object>) firstNonNullRow).keySet());
            } else if (type.isBean()) {
                columnNames = new ArrayList<>(ClassUtil.getPropNameList(cls));
            } else if (type.isArray()) {
                final Object[] a = (Object[]) firstNonNullRow;
                columnNames = new ArrayList<>(a.length);

                for (Object e : a) {
                    columnNames.add(N.stringOf(e));
                }

                startRowIndex = 1;
            } else if (type.isCollection()) {
                final Collection<?> c = (Collection<?>) firstNonNullRow;
                columnNames = new ArrayList<>(c.size());

                for (Object e : c) {
                    columnNames.add(N.stringOf(e));
                }

                startRowIndex = 1;
            } else {
                throw new IllegalArgumentException("Unsupported header type: " + type.name() + " when specified 'columnNames' is null or empty");
            }

            if (isEmpty(columnNames)) {
                throw new IllegalArgumentException("Column name list can not be obtained from row list because it's empty or null");
            }
        }

        final int rowCount = rowList.size() - startRowIndex;
        final int columnCount = columnNames.size();
        final List<String> columnNameList = new ArrayList<>(columnNames);
        final List<List<Object>> columnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            columnList.add(new ArrayList<>(rowCount));
        }

        Type<?> type = null;

        for (Object row : rowList) {
            if (startRowIndex-- > 0) {
                // skip
                continue;
            }

            if (row == null) {
                for (int i = 0; i < columnCount; i++) {
                    columnList.get(i).add(null);
                }

                continue;
            }

            final Class<?> cls = row.getClass();
            type = typeOf(cls);

            if (type.isMap()) {
                Map<String, Object> props = (Map<String, Object>) row;

                for (int i = 0; i < columnCount; i++) {
                    columnList.get(i).add(props.get(columnNameList.get(i)));
                }
            } else if (type.isBean()) {
                final BeanInfo beanInfo = ParserUtil.getBeanInfo(cls);
                PropInfo propInfo = null;

                for (int i = 0; i < columnCount; i++) {
                    propInfo = beanInfo.getPropInfo(columnNameList.get(i));

                    if (propInfo == null) {
                        columnList.get(i).add(null);
                    } else {
                        columnList.get(i).add(propInfo.getPropValue(row));
                    }
                }
            } else if (type.isArray()) {
                if (type.isPrimitiveArray()) {
                    for (int i = 0; i < columnCount; i++) {
                        columnList.get(i).add(Array.get(row, i));
                    }
                } else {
                    Object[] array = (Object[]) row;

                    for (int i = 0; i < columnCount; i++) {
                        columnList.get(i).add(array[i]);
                    }
                }
            } else if (type.isCollection()) {
                final Iterator<Object> it = ((Collection<Object>) row).iterator();

                for (int i = 0; i < columnCount; i++) {
                    columnList.get(i).add(it.next());
                }
            } else {
                throw new IllegalArgumentException(
                        "Unsupported row type: " + ClassUtil.getCanonicalClassName(row.getClass()) + ". Only array, collection, map and bean are supported");
            }
        }

        return new RowDataSet(columnNameList, columnList);
    }

    /**
     *
     *
     * @param columnNames
     * @param rowList
     * @return
     */
    public static DataSet newDataSet(Collection<String> columnNames, final Object[][] rowList) {
        if (isEmpty(columnNames) && isEmpty(rowList)) {
            // throw new IllegalArgumentException("Column name list and row list can not be both null or empty");
            return newEmptyDataSet();
        } else if (isEmpty(rowList)) {
            return newEmptyDataSet(columnNames);
        }

        N.checkArgument(N.size(columnNames) == N.len(rowList[0]), "length of 'columnNames' is not equals to length of 'rowList[0]'");

        return newDataSet(columnNames, N.asList(rowList));
    }

    /**
     * New data set.
     *
     * @param <C>
     * @param map keys are column names, values are columns
     * @return
     */
    public static <C extends Collection<?>> DataSet newDataSet(final Map<String, C> map) {
        if (isEmpty(map)) {
            return newEmptyDataSet();
        }

        int maxColumnLen = 0;

        for (C v : map.values()) {
            maxColumnLen = N.max(maxColumnLen, size(v));
        }

        final List<String> columnNameList = new ArrayList<>(map.keySet());
        final List<List<Object>> columnList = new ArrayList<>(columnNameList.size());
        List<Object> column = null;

        for (C v : map.values()) {
            column = new ArrayList<>(maxColumnLen);

            if (notEmpty(v)) {
                column.addAll(v);
            }

            if (column.size() < maxColumnLen) {
                fill(column, column.size(), maxColumnLen, null);
            }

            columnList.add(column);
        }

        return new RowDataSet(columnNameList, columnList);
    }

    /**
     *
     *
     * @param a
     * @param b
     * @return
     * @throws IllegalArgumentException
     */
    public static DataSet merge(final DataSet a, final DataSet b) throws IllegalArgumentException {
        N.checkArgNotNull(a);
        N.checkArgNotNull(b);

        return a.merge(b);
    }

    /**
     *
     *
     * @param a
     * @param b
     * @param c
     * @return
     * @throws IllegalArgumentException
     */
    public static DataSet merge(final DataSet a, final DataSet b, final DataSet c) throws IllegalArgumentException {
        N.checkArgNotNull(a);
        N.checkArgNotNull(b);
        N.checkArgNotNull(c);

        return merge(N.asList(a, b, c));
    }

    /**
     *
     *
     * @param dss
     * @return
     */
    public static DataSet merge(final Collection<? extends DataSet> dss) {
        return merge(dss, false);
    }

    /**
     *
     *
     * @param dss
     * @param requiresSameColumns
     * @return
     */
    public static DataSet merge(final Collection<? extends DataSet> dss, final boolean requiresSameColumns) {
        if (requiresSameColumns && size(dss) > 1) {
            final Iterator<? extends DataSet> iter = dss.iterator();
            final DataSet firstDataSet = iter.next();

            if (iter.hasNext()) {
                checkIfColumnNamesAreSame(firstDataSet, iter.next());
            }
        }

        if (N.isEmpty(dss)) {
            return N.newEmptyDataSet();
        } else if (dss.size() == 1) {
            return N.newEmptyDataSet().merge(dss.iterator().next());
        } else if (dss.size() == 2) {
            final Iterator<? extends DataSet> iter = dss.iterator();
            return iter.next().merge(iter.next());
        } else {
            final Set<String> columnNameSet = N.newLinkedHashSet();
            final Properties<String, Object> props = new Properties<>();
            int totalSize = 0;

            for (DataSet ds : dss) {
                columnNameSet.addAll(ds.columnNameList());
                totalSize += ds.size();

                if (N.notEmpty(ds.properties())) {
                    props.putAll(ds.properties());
                }
            }

            final int newColumnCount = columnNameSet.size();
            final List<String> newColumnNameList = new ArrayList<>(columnNameSet);
            final List<List<Object>> newColumnList = new ArrayList<>(newColumnCount);

            for (int i = 0; i < newColumnCount; i++) {
                newColumnList.add(new ArrayList<>(totalSize));
            }

            for (DataSet ds : dss) {
                if (ds.size() == 0) {
                    continue;
                }

                List<Object> column = null;

                for (int i = 0; i < newColumnCount; i++) {
                    column = newColumnList.get(i);

                    if (ds.containsColumn(newColumnNameList.get(i))) {
                        column.addAll(ds.getColumn(newColumnNameList.get(i)));
                    } else {
                        N.fill(column, column.size(), column.size() + ds.size(), null);
                    }
                }
            }

            return new RowDataSet(newColumnNameList, newColumnList, props);
        }
    }

    private static void checkIfColumnNamesAreSame(DataSet a, DataSet b) {
        if (!(a.columnNameList().size() == b.columnNameList().size() && a.columnNameList().containsAll(b.columnNameList())
                && b.columnNameList().containsAll(a.columnNameList()))) {
            throw new IllegalArgumentException("These two DataSets don't have same column names: " + a.columnNameList() + ", " + b.columnNameList());
        }
    }

    /**
     * Returns an empty array if the specified collection is null or empty.
     *
     * @param c
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Object[] toArray(final Collection<?> c) {
        if (isEmpty(c)) {
            return EMPTY_OBJECT_ARRAY;
        }

        return c.toArray(new Object[c.size()]);
    }

    /**
     *
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    @SuppressWarnings("rawtypes")
    public static Object[] toArray(final Collection<?> c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (isEmpty(c)) {
            return EMPTY_OBJECT_ARRAY;
        } else if (fromIndex == 0 || toIndex == c.size()) {
            return c.toArray(new Object[c.size()]);
        } else if (c instanceof List) {
            return ((List) c).subList(fromIndex, toIndex).toArray(new Object[toIndex - fromIndex]);
        } else {
            final Object[] res = new Object[toIndex - fromIndex];
            final Iterator<?> iter = c.iterator();
            int idx = 0;

            while (idx < fromIndex && iter.hasNext()) {
                iter.next();
                idx++;
            }

            while (idx < toIndex && iter.hasNext()) {
                res[idx - fromIndex] = iter.next();
                idx++;
            }

            return res;
        }
    }

    /**
     *
     *
     * @param <A>
     * @param <T>
     * @param c
     * @param a
     * @return the specified array if the specified collection is {@code null} or empty.
     * @throws IndexOutOfBoundsException
     * @throws IllegalArgumentException if the specified {@code Array} is <code>null</code>.
     */
    public static <A, T extends A> A[] toArray(final Collection<? extends T> c, final A[] a) throws IndexOutOfBoundsException, IllegalArgumentException {
        checkArgNotNull(a);

        if (isEmpty(c)) {
            return a;
        }

        return c.toArray(a);
    }

    /**
     *
     * @param <A>
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param a
     * @return the specified array if the specified collection is {@code null} or empty.
     * @throws IllegalArgumentException if the specified {@code Array} is <code>null</code>.
     */
    public static <A, T extends A> A[] toArray(final Collection<? extends T> c, final int fromIndex, final int toIndex, final A[] a)
            throws IllegalArgumentException {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(a);

        if (isEmpty(c)) {
            return a;
        } else if (fromIndex == 0 || toIndex == c.size()) {
            return c.toArray(a);
        } else if (c instanceof List) {
            return ((List<T>) c).subList(fromIndex, toIndex).toArray(a);
        } else {
            final A[] res = a.length >= toIndex - fromIndex ? a : (A[]) newArray(a.getClass().getComponentType(), toIndex - fromIndex);
            final Iterator<? extends T> iter = c.iterator();
            int idx = 0;

            while (idx < fromIndex && iter.hasNext()) {
                iter.next();
                idx++;
            }

            while (idx < toIndex && iter.hasNext()) {
                res[idx - fromIndex] = iter.next();
                idx++;
            }

            return res;
        }
    }

    /**
     *
     *
     * @param <A>
     * @param <T>
     * @param c
     * @param arraySupplier
     * @return
     * @throws IllegalArgumentException
     */
    public static <A, T extends A> A[] toArray(final Collection<? extends T> c, final IntFunction<A[]> arraySupplier) throws IllegalArgumentException {
        checkArgNotNull(arraySupplier);

        if (isEmpty(c)) {
            return arraySupplier.apply(0);
        }

        return toArray(c, arraySupplier);
    }

    /**
     *
     *
     * @param <A>
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param arraySupplier
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static <A, T extends A> A[] toArray(final Collection<? extends T> c, final int fromIndex, final int toIndex, final IntFunction<A[]> arraySupplier)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNull(arraySupplier);
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (isEmpty(c)) {
            return arraySupplier.apply(0);
        } else if (fromIndex == 0 || toIndex == c.size()) {
            return c.toArray(arraySupplier.apply(c.size()));
        } else if (c instanceof List) {
            return ((List<T>) c).subList(fromIndex, toIndex).toArray(arraySupplier.apply(toIndex - fromIndex));
        } else {
            final A[] res = arraySupplier.apply(toIndex - fromIndex);
            final Iterator<? extends T> iter = c.iterator();
            int idx = 0;

            while (idx < fromIndex && iter.hasNext()) {
                iter.next();
                idx++;
            }

            while (idx < toIndex && iter.hasNext()) {
                res[idx - fromIndex] = iter.next();
                idx++;
            }

            return res;
        }
    }

    /**
     *
     *
     * @param <A>
     * @param <T>
     * @param c
     * @param targetType
     * @return
     * @throws IndexOutOfBoundsException
     * @throws IllegalArgumentException if the specified {@code Class} is <code>null</code>.
     */
    public static <A, T extends A> A[] toArray(final Collection<? extends T> c, final Class<A[]> targetType)
            throws IndexOutOfBoundsException, IllegalArgumentException {
        checkArgNotNull(targetType);

        if (isEmpty(c)) {
            return newArray(targetType.getComponentType(), 0);
        }

        return c.toArray((A[]) newArray(targetType.getComponentType(), c.size()));
    }

    /**
     *
     *
     * @param <A>
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param targetType
     * @return
     * @throws IllegalArgumentException if the specified {@code Class} is <code>null</code>.
     */
    public static <A, T extends A> A[] toArray(final Collection<? extends T> c, final int fromIndex, final int toIndex, final Class<A[]> targetType)
            throws IllegalArgumentException {
        checkArgNotNull(targetType);
        checkFromToIndex(fromIndex, toIndex, size(c));

        final A[] res = newArray(targetType.getComponentType(), toIndex - fromIndex);

        if (isEmpty(c)) {
            return res;
        } else if (fromIndex == 0 || toIndex == c.size()) {
            return c.toArray(res);
        } else if (c instanceof List) {
            return ((List<T>) c).subList(fromIndex, toIndex).toArray(res);
        } else {
            final Iterator<? extends T> iter = c.iterator();
            int idx = 0;

            while (idx < fromIndex && iter.hasNext()) {
                iter.next();
                idx++;
            }

            while (idx < toIndex && iter.hasNext()) {
                res[idx - fromIndex] = iter.next();
                idx++;
            }

            return res;
        }
    }

    /**
     * To boolean array.
     *
     * @param c
     * @return
     */
    public static boolean[] toBooleanArray(final Collection<Boolean> c) {
        return toBooleanArray(c, false);
    }

    /**
     * To boolean array.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static boolean[] toBooleanArray(final Collection<Boolean> c, final int fromIndex, final int toIndex) {
        return toBooleanArray(c, fromIndex, toIndex, false);
    }

    /**
     * To boolean array.
     *
     * @param c
     * @param defaultForNull
     * @return
     */
    public static boolean[] toBooleanArray(final Collection<Boolean> c, final boolean defaultForNull) {
        return toBooleanArray(c, 0, size(c), defaultForNull);
    }

    /**
     * To boolean array.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param defaultForNull
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static boolean[] toBooleanArray(final Collection<Boolean> c, final int fromIndex, final int toIndex, final boolean defaultForNull)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (fromIndex == toIndex) {
            return EMPTY_BOOLEAN_ARRAY;
        }

        final int len = toIndex - fromIndex;
        boolean[] result = new boolean[len];

        if (c instanceof List && c instanceof RandomAccess) {
            final List<Boolean> list = (List<Boolean>) c;
            Boolean val = null;

            for (int i = 0; i < len; i++) {
                if ((val = list.get(i + fromIndex)) == null) {
                    result[i] = defaultForNull;
                } else {
                    result[i] = val;
                }
            }
        } else {
            final Iterator<Boolean> iter = c.iterator();

            if (fromIndex > 0) {
                int offset = 0;

                while (offset++ < fromIndex) {
                    iter.next();
                }
            }

            Boolean val = null;

            for (int i = 0; i < len; i++) {
                if ((val = iter.next()) == null) {
                    result[i] = defaultForNull;
                } else {
                    result[i] = val;
                }
            }
        }

        return result;
    }

    /**
     *
     *
     * @param a
     * @return {@code null} if {@code (a == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static boolean[] toBooleanArray(final byte[] a) {
        if (a == null) {
            return null; // NOSONAR
        } else if (a.length == 0) {
            return EMPTY_BOOLEAN_ARRAY;
        } else {
            final int len = a.length;
            final boolean[] result = new boolean[len];

            for (int i = 0; i < len; i++) {
                result[i] = a[i] > Numbers.BYTE_ZERO;
            }

            return result;
        }
    }

    /**
     * To char array.
     *
     * @param c
     * @return
     */
    public static char[] toCharArray(final Collection<Character> c) {
        return toCharArray(c, (char) 0);
    }

    /**
     * To char array.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static char[] toCharArray(final Collection<Character> c, final int fromIndex, final int toIndex) {
        return toCharArray(c, fromIndex, toIndex, (char) 0);
    }

    /**
     * To char array.
     *
     * @param c
     * @param defaultForNull
     * @return
     */
    public static char[] toCharArray(final Collection<Character> c, final char defaultForNull) {
        return toCharArray(c, 0, size(c), defaultForNull);
    }

    /**
     * To char array.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param defaultForNull
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static char[] toCharArray(final Collection<Character> c, final int fromIndex, final int toIndex, final char defaultForNull)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (fromIndex == toIndex) {
            return EMPTY_CHAR_ARRAY;
        }

        final int len = toIndex - fromIndex;
        char[] result = new char[len];

        if (c instanceof List && c instanceof RandomAccess) {
            final List<Character> list = (List<Character>) c;
            Character val = null;

            for (int i = 0; i < len; i++) {
                if ((val = list.get(i + fromIndex)) == null) {
                    result[i] = defaultForNull;
                } else {
                    result[i] = val;
                }
            }
        } else {
            final Iterator<Character> iter = c.iterator();

            if (fromIndex > 0) {
                int offset = 0;

                while (offset++ < fromIndex) {
                    iter.next();
                }
            }

            Character val = null;

            for (int i = 0; i < len; i++) {
                if ((val = iter.next()) == null) {
                    result[i] = defaultForNull;
                } else {
                    result[i] = val;
                }
            }
        }

        return result;
    }

    /**
     * To byte array.
     *
     * @param c
     * @return
     */
    public static byte[] toByteArray(final Collection<? extends Number> c) {
        return toByteArray(c, (byte) 0);
    }

    /**
     * To byte array.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static byte[] toByteArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex) {
        return toByteArray(c, fromIndex, toIndex, (byte) 0);
    }

    /**
     * To byte array.
     *
     * @param c
     * @param defaultForNull
     * @return
     */
    public static byte[] toByteArray(final Collection<? extends Number> c, final byte defaultForNull) {
        return toByteArray(c, 0, size(c), defaultForNull);
    }

    /**
     * To byte array.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param defaultForNull
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static byte[] toByteArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex, final byte defaultForNull)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (fromIndex == toIndex) {
            return EMPTY_BYTE_ARRAY;
        }

        final int len = toIndex - fromIndex;
        byte[] result = new byte[len];

        if (c instanceof List && c instanceof RandomAccess) {
            final List<? extends Number> list = (List<? extends Number>) c;
            Number val = null;

            for (int i = 0; i < len; i++) {
                if ((val = list.get(i + fromIndex)) == null) {
                    result[i] = defaultForNull;
                } else {
                    result[i] = val.byteValue();
                }
            }
        } else {
            final Iterator<? extends Number> iter = c.iterator();

            if (fromIndex > 0) {
                int offset = 0;

                while (offset++ < fromIndex) {
                    iter.next();
                }
            }

            Number val = null;

            for (int i = 0; i < len; i++) {
                if ((val = iter.next()) == null) {
                    result[i] = defaultForNull;
                } else {
                    result[i] = val.byteValue();
                }
            }
        }

        return result;
    }

    /**
     *
     *
     * @param a
     * @return {@code null} if {@code (a == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static byte[] toByteArray(final boolean[] a) {
        if (a == null) {
            return null; // NOSONAR
        } else if (a.length == 0) {
            return EMPTY_BYTE_ARRAY;
        } else {
            final int len = a.length;
            final byte[] result = new byte[len];

            for (int i = 0; i < len; i++) {
                result[i] = a[i] ? Numbers.BYTE_ONE : Numbers.BYTE_ZERO;
            }

            return result;
        }
    }

    /**
     * To short array.
     *
     * @param c
     * @return
     */
    public static short[] toShortArray(final Collection<? extends Number> c) {
        return toShortArray(c, (short) 0);
    }

    /**
     * To short array.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static short[] toShortArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex) {
        return toShortArray(c, fromIndex, toIndex, (short) 0);
    }

    /**
     * To short array.
     *
     * @param c
     * @param defaultForNull
     * @return
     */
    public static short[] toShortArray(final Collection<? extends Number> c, final short defaultForNull) {
        return toShortArray(c, 0, size(c), defaultForNull);
    }

    /**
     * To short array.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param defaultForNull
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static short[] toShortArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex, final short defaultForNull)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (fromIndex == toIndex) {
            return EMPTY_SHORT_ARRAY;
        }

        final int len = toIndex - fromIndex;
        short[] result = new short[len];

        if (c instanceof List && c instanceof RandomAccess) {
            final List<? extends Number> list = (List<? extends Number>) c;
            Number val = null;

            for (int i = 0; i < len; i++) {
                if ((val = list.get(i + fromIndex)) == null) {
                    result[i] = defaultForNull;
                } else {
                    result[i] = val.shortValue();
                }
            }
        } else {
            final Iterator<? extends Number> iter = c.iterator();

            if (fromIndex > 0) {
                int offset = 0;

                while (offset++ < fromIndex) {
                    iter.next();
                }
            }

            Number val = null;

            for (int i = 0; i < len; i++) {
                if ((val = iter.next()) == null) {
                    result[i] = defaultForNull;
                } else {
                    result[i] = val.shortValue();
                }
            }
        }

        return result;
    }

    /**
     * To int array.
     *
     * @param c
     * @return
     */
    public static int[] toIntArray(final Collection<? extends Number> c) {
        return toIntArray(c, 0);
    }

    /**
     * To int array.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static int[] toIntArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex) {
        return toIntArray(c, fromIndex, toIndex, 0);
    }

    /**
     * To int array.
     *
     * @param c
     * @param defaultForNull
     * @return
     */
    public static int[] toIntArray(final Collection<? extends Number> c, final int defaultForNull) {
        return toIntArray(c, 0, size(c), defaultForNull);
    }

    /**
     * To int array.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param defaultForNull
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static int[] toIntArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex, final int defaultForNull)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (fromIndex == toIndex) {
            return EMPTY_INT_ARRAY;
        }

        final int len = toIndex - fromIndex;
        int[] result = new int[len];

        if (c instanceof List && c instanceof RandomAccess) {
            final List<? extends Number> list = (List<? extends Number>) c;
            Number val = null;

            for (int i = 0; i < len; i++) {
                if ((val = list.get(i + fromIndex)) == null) {
                    result[i] = defaultForNull;
                } else {
                    result[i] = val.intValue();
                }
            }
        } else {
            final Iterator<? extends Number> iter = c.iterator();

            if (fromIndex > 0) {
                int offset = 0;

                while (offset++ < fromIndex) {
                    iter.next();
                }
            }

            Number val = null;

            for (int i = 0; i < len; i++) {
                if ((val = iter.next()) == null) {
                    result[i] = defaultForNull;
                } else {
                    result[i] = val.intValue();
                }
            }
        }

        return result;
    }

    /**
     *
     *
     * @param a
     * @return {@code null} if {@code (a == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static int[] toIntArray(final char[] a) {
        if (a == null) {
            return null; // NOSONAR
        } else if (a.length == 0) {
            return EMPTY_INT_ARRAY;
        } else {
            final int len = a.length;
            final int[] result = new int[len];

            for (int i = 0; i < len; i++) {
                result[i] = a[i]; //NOSONAR
            }

            return result;
        }
    }

    /**
     * To long array.
     *
     * @param c
     * @return
     */
    public static long[] toLongArray(final Collection<? extends Number> c) {
        return toLongArray(c, 0);
    }

    /**
     * To long array.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static long[] toLongArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex) {
        return toLongArray(c, fromIndex, toIndex, 0);
    }

    /**
     * To long array.
     *
     * @param c
     * @param defaultForNull
     * @return
     */
    public static long[] toLongArray(final Collection<? extends Number> c, final long defaultForNull) {
        return toLongArray(c, 0, size(c), defaultForNull);
    }

    /**
     * To long array.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param defaultForNull
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static long[] toLongArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex, final long defaultForNull)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (fromIndex == toIndex) {
            return EMPTY_LONG_ARRAY;
        }

        final int len = toIndex - fromIndex;
        long[] result = new long[len];

        if (c instanceof List && c instanceof RandomAccess) {
            final List<? extends Number> list = (List<? extends Number>) c;
            Number val = null;

            for (int i = 0; i < len; i++) {
                if ((val = list.get(i + fromIndex)) == null) {
                    result[i] = defaultForNull;
                } else {
                    result[i] = val.longValue();
                }
            }
        } else {
            final Iterator<? extends Number> iter = c.iterator();

            if (fromIndex > 0) {
                int offset = 0;

                while (offset++ < fromIndex) {
                    iter.next();
                }
            }

            Number val = null;

            for (int i = 0; i < len; i++) {
                if ((val = iter.next()) == null) {
                    result[i] = defaultForNull;
                } else {
                    result[i] = val.longValue();
                }
            }
        }

        return result;
    }

    /**
     * To float array.
     *
     * @param c
     * @return
     */
    public static float[] toFloatArray(final Collection<? extends Number> c) {
        return toFloatArray(c, 0);
    }

    /**
     * To float array.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static float[] toFloatArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex) {
        return toFloatArray(c, fromIndex, toIndex, 0);
    }

    /**
     * To float array.
     *
     * @param c
     * @param defaultForNull
     * @return
     */
    public static float[] toFloatArray(final Collection<? extends Number> c, final float defaultForNull) {
        return toFloatArray(c, 0, size(c), defaultForNull);
    }

    /**
     * To float array.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param defaultForNull
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static float[] toFloatArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex, final float defaultForNull)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (fromIndex == toIndex) {
            return EMPTY_FLOAT_ARRAY;
        }

        final int len = toIndex - fromIndex;
        float[] result = new float[len];

        if (c instanceof List && c instanceof RandomAccess) {
            final List<? extends Number> list = (List<? extends Number>) c;
            Number val = null;

            for (int i = 0; i < len; i++) {
                if ((val = list.get(i + fromIndex)) == null) {
                    result[i] = defaultForNull;
                } else {
                    result[i] = Numbers.toFloat(val);
                }
            }
        } else {
            final Iterator<? extends Number> iter = c.iterator();

            if (fromIndex > 0) {
                int offset = 0;

                while (offset++ < fromIndex) {
                    iter.next();
                }
            }

            Number val = null;

            for (int i = 0; i < len; i++) {
                if ((val = iter.next()) == null) {
                    result[i] = defaultForNull;
                } else {
                    result[i] = Numbers.toFloat(val);
                }
            }
        }

        return result;
    }

    /**
     * To double array.
     *
     * @param c
     * @return
     */
    public static double[] toDoubleArray(final Collection<? extends Number> c) {
        return toDoubleArray(c, 0);
    }

    /**
     * To double array.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static double[] toDoubleArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex) {
        return toDoubleArray(c, fromIndex, toIndex, 0);
    }

    /**
     * To double array.
     *
     * @param c
     * @param defaultForNull
     * @return
     */
    public static double[] toDoubleArray(final Collection<? extends Number> c, final double defaultForNull) {
        return toDoubleArray(c, 0, size(c), defaultForNull);
    }

    /**
     * To double array.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param defaultForNull
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static double[] toDoubleArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex, final double defaultForNull)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (fromIndex == toIndex) {
            return EMPTY_DOUBLE_ARRAY;
        }

        final int len = toIndex - fromIndex;
        double[] result = new double[len];

        if (c instanceof List && c instanceof RandomAccess) {
            final List<? extends Number> list = (List<? extends Number>) c;
            Number val = null;

            for (int i = 0; i < len; i++) {
                if ((val = list.get(i + fromIndex)) == null) {
                    result[i] = defaultForNull;
                } else {
                    result[i] = Numbers.toDouble(val);
                }
            }
        } else {
            final Iterator<? extends Number> iter = c.iterator();

            if (fromIndex > 0) {
                int offset = 0;

                while (offset++ < fromIndex) {
                    iter.next();
                }
            }

            Number val = null;

            for (int i = 0; i < len; i++) {
                if ((val = iter.next()) == null) {
                    result[i] = defaultForNull;
                } else {
                    result[i] = Numbers.toDouble(val);
                }
            }
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static List<Boolean> toList(final boolean[] a) {
        return toList(a, 0, len(a));
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static List<Boolean> toList(final boolean[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final List<Boolean> result = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static List<Character> toList(final char[] a) {
        return toList(a, 0, len(a));
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static List<Character> toList(final char[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final List<Character> result = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static List<Byte> toList(final byte[] a) {
        return toList(a, 0, len(a));
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static List<Byte> toList(final byte[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final List<Byte> result = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static List<Short> toList(final short[] a) {
        return toList(a, 0, len(a));
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static List<Short> toList(final short[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final List<Short> result = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static List<Integer> toList(final int[] a) {
        return toList(a, 0, len(a));
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static List<Integer> toList(final int[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final List<Integer> result = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static List<Long> toList(final long[] a) {
        return toList(a, 0, len(a));
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static List<Long> toList(final long[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final List<Long> result = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static List<Float> toList(final float[] a) {
        return toList(a, 0, len(a));
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static List<Float> toList(final float[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final List<Float> result = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static List<Double> toList(final double[] a) {
        return toList(a, 0, len(a));
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static List<Double> toList(final double[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final List<Double> result = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T> List<T> toList(final T[] a) {
        if (isEmpty(a)) {
            return new ArrayList<>();
        }

        return asList(a);
    }

    /**
     *
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static <T> List<T> toList(final T[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new ArrayList<>();
        } else if (fromIndex == 0 && toIndex == a.length) {
            return asList(a);
        }

        final List<T> result = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]); //NOSONAR
        }

        return result;
    }

    /**
     *
     *
     * @param <T>
     * @param iter
     * @return
     */
    public static <T> List<T> toList(final Iterator<? extends T> iter) {
        if (iter == null) {
            return new ArrayList<>();
        }

        final List<T> result = new ArrayList<>();

        while (iter.hasNext()) {
            result.add(iter.next());
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Set<Boolean> toSet(final boolean[] a) {
        return toSet(a, 0, len(a));
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static Set<Boolean> toSet(final boolean[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return newHashSet();
        }

        final Set<Boolean> result = newHashSet(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Set<Character> toSet(final char[] a) {
        return toSet(a, 0, len(a));
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static Set<Character> toSet(final char[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return newHashSet();
        }

        final Set<Character> result = newHashSet(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Set<Byte> toSet(final byte[] a) {
        return toSet(a, 0, len(a));
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static Set<Byte> toSet(final byte[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return newHashSet();
        }

        final Set<Byte> result = newHashSet(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Set<Short> toSet(final short[] a) {
        return toSet(a, 0, len(a));
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static Set<Short> toSet(final short[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return newHashSet();
        }

        final Set<Short> result = newHashSet(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Set<Integer> toSet(final int[] a) {
        return toSet(a, 0, len(a));
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static Set<Integer> toSet(final int[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return newHashSet();
        }

        final Set<Integer> result = newHashSet(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Set<Long> toSet(final long[] a) {
        return toSet(a, 0, len(a));
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static Set<Long> toSet(final long[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return newHashSet();
        }

        final Set<Long> result = newHashSet(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Set<Float> toSet(final float[] a) {
        return toSet(a, 0, len(a));
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static Set<Float> toSet(final float[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return newHashSet();
        }

        final Set<Float> result = newHashSet(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     */
    public static Set<Double> toSet(final double[] a) {
        return toSet(a, 0, len(a));
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static Set<Double> toSet(final double[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return newHashSet();
        }

        final Set<Double> result = newHashSet(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T> Set<T> toSet(final T[] a) {
        if (isEmpty(a)) {
            return newHashSet();
        }

        return asSet(a);
    }

    /**
     *
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static <T> Set<T> toSet(final T[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return newHashSet();
        }

        final Set<T> result = newHashSet(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]); //NOSONAR
        }

        return result;
    }

    /**
     *
     *
     * @param <T>
     * @param iter
     * @return
     */
    public static <T> Set<T> toSet(final Iterator<? extends T> iter) {
        if (iter == null) {
            return N.newHashSet();
        }

        final Set<T> result = N.newHashSet();

        while (iter.hasNext()) {
            result.add(iter.next());
        }

        return result;
    }

    /**
     *
     * @param <C>
     * @param a
     * @param supplier
     * @return
     */
    public static <C extends Collection<Boolean>> C toCollection(final boolean[] a, final IntFunction<? extends C> supplier) {
        return toCollection(a, 0, len(a), supplier);
    }

    /**
     *
     *
     * @param <C>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static <C extends Collection<Boolean>> C toCollection(final boolean[] a, final int fromIndex, final int toIndex,
            final IntFunction<? extends C> supplier) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param <C>
     * @param a
     * @param supplier
     * @return
     */
    public static <C extends Collection<Character>> C toCollection(final char[] a, final IntFunction<? extends C> supplier) {
        return toCollection(a, 0, len(a), supplier);
    }

    /**
     *
     *
     * @param <C>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static <C extends Collection<Character>> C toCollection(final char[] a, final int fromIndex, final int toIndex,
            final IntFunction<? extends C> supplier) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param <C>
     * @param a
     * @param supplier
     * @return
     */
    public static <C extends Collection<Byte>> C toCollection(final byte[] a, final IntFunction<? extends C> supplier) {
        return toCollection(a, 0, len(a), supplier);
    }

    /**
     *
     *
     * @param <C>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static <C extends Collection<Byte>> C toCollection(final byte[] a, final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param <C>
     * @param a
     * @param supplier
     * @return
     */
    public static <C extends Collection<Short>> C toCollection(final short[] a, final IntFunction<? extends C> supplier) {
        return toCollection(a, 0, len(a), supplier);
    }

    /**
     *
     *
     * @param <C>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static <C extends Collection<Short>> C toCollection(final short[] a, final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param <C>
     * @param a
     * @param supplier
     * @return
     */
    public static <C extends Collection<Integer>> C toCollection(final int[] a, final IntFunction<? extends C> supplier) {
        return toCollection(a, 0, len(a), supplier);
    }

    /**
     *
     *
     * @param <C>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static <C extends Collection<Integer>> C toCollection(final int[] a, final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param <C>
     * @param a
     * @param supplier
     * @return
     */
    public static <C extends Collection<Long>> C toCollection(final long[] a, final IntFunction<? extends C> supplier) {
        return toCollection(a, 0, len(a), supplier);
    }

    /**
     *
     *
     * @param <C>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static <C extends Collection<Long>> C toCollection(final long[] a, final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param <C>
     * @param a
     * @param supplier
     * @return
     */
    public static <C extends Collection<Float>> C toCollection(final float[] a, final IntFunction<? extends C> supplier) {
        return toCollection(a, 0, len(a), supplier);
    }

    /**
     *
     *
     * @param <C>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static <C extends Collection<Float>> C toCollection(final float[] a, final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param <C>
     * @param a
     * @param supplier
     * @return
     */
    public static <C extends Collection<Double>> C toCollection(final double[] a, final IntFunction<? extends C> supplier) {
        return toCollection(a, 0, len(a), supplier);
    }

    /**
     *
     *
     * @param <C>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static <C extends Collection<Double>> C toCollection(final double[] a, final int fromIndex, final int toIndex,
            final IntFunction<? extends C> supplier) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <C>
     * @param a
     * @param supplier
     * @return
     */
    public static <T, C extends Collection<T>> C toCollection(final T[] a, final IntFunction<? extends C> supplier) {
        if (isEmpty(a)) {
            return supplier.apply(0);
        }

        return toCollection(a, 0, a.length, supplier);
    }

    /**
     *
     *
     * @param <T>
     * @param <C>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static <T, C extends Collection<T>> C toCollection(final T[] a, final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return supplier.apply(0);
        } else if (fromIndex == 0 && toIndex == a.length && a.length >= MIN_SIZE_FOR_COPY_ALL) {
            final C result = supplier.apply(a.length);
            result.addAll(Arrays.asList(a));
            return result;
        } else {
            final C result = supplier.apply(toIndex - fromIndex);

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(a[i]); //NOSONAR
            }

            return result;
        }
    }

    /**
     *
     *
     * @param <T>
     * @param <C>
     * @param c
     * @param supplier
     * @return
     */
    public static <T, C extends Collection<T>> C toCollection(final Iterable<? extends T> c, final IntFunction<? extends C> supplier) {
        if (c == null) {
            return supplier.apply(0);
        }

        if (c instanceof Collection) {
            final Collection<T> tmp = (Collection<T>) c;
            final C ret = supplier.apply(tmp.size());

            ret.addAll(tmp);

            return ret;
        } else {
            final Iterator<? extends T> iter = c.iterator();
            final C ret = supplier.apply(0);

            while (iter.hasNext()) {
                ret.add(iter.next());
            }

            return ret;
        }
    }

    /**
     *
     *
     * @param <T>
     * @param <C>
     * @param iter
     * @param supplier
     * @return
     */
    public static <T, C extends Collection<T>> C toCollection(final Iterator<? extends T> iter, final Supplier<? extends C> supplier) {
        final C c = supplier.get();

        if (iter == null) {
            return c;
        }

        while (iter.hasNext()) {
            c.add(iter.next());
        }

        return c;
    }

    /**
     *
     *
     * @param <T>
     * @param <K> the key type
     * @param c
     * @param keyMapper
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, K> Map<K, T> toMap(Iterable<? extends T> c, final Function<? super T, ? extends K> keyMapper) throws IllegalArgumentException {
        N.checkArgNotNull(keyMapper);

        if (c == null) {
            return new HashMap<>(0);
        }

        final Map<K, T> result = N.newHashMap(c instanceof Collection ? ((Collection<T>) c).size() : 0);

        for (T e : c) {
            result.put(keyMapper.apply(e), e);
        }

        return result;
    }

    /**
     *
     *
     * @param <T>
     * @param <K> the key type
     * @param <V> the value type
     * @param c
     * @param keyMapper
     * @param valueExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, K, V> Map<K, V> toMap(Iterable<? extends T> c, final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyMapper);
        N.checkArgNotNull(valueExtractor);

        if (c == null) {
            return new HashMap<>(0);
        }

        final Map<K, V> result = N.newHashMap(c instanceof Collection ? ((Collection<T>) c).size() : 0);

        for (T e : c) {
            result.put(keyMapper.apply(e), valueExtractor.apply(e));
        }

        return result;
    }

    /**
     *
     *
     * @param <T>
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param c
     * @param keyMapper
     * @param valueExtractor
     * @param mapSupplier
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, K, V, M extends Map<K, V>> M toMap(Iterable<? extends T> c, final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueExtractor, final IntFunction<? extends M> mapSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(keyMapper);
        N.checkArgNotNull(valueExtractor);
        N.checkArgNotNull(mapSupplier);

        if (c == null) {
            return mapSupplier.apply(0);
        }

        final M result = mapSupplier.apply(c instanceof Collection ? ((Collection<T>) c).size() : 0);

        for (T e : c) {
            result.put(keyMapper.apply(e), valueExtractor.apply(e));
        }

        return result;
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param <M>
     * @param c
     * @param keyMapper
     * @param valueExtractor
     * @param mergeFunction
     * @param mapSupplier
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, K, V, M extends Map<K, V>> M toMap(Iterable<? extends T> c, final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueExtractor, final BinaryOperator<V> mergeFunction, final IntFunction<? extends M> mapSupplier)
            throws IllegalArgumentException {
        N.checkArgNotNull(keyMapper);
        N.checkArgNotNull(valueExtractor);
        N.checkArgNotNull(mergeFunction);
        N.checkArgNotNull(mapSupplier);

        if (c == null) {
            return mapSupplier.apply(0);
        }

        final M result = mapSupplier.apply(c instanceof Collection ? ((Collection<T>) c).size() : 0);
        K key = null;

        for (T e : c) {
            key = keyMapper.apply(e);

            final V oldValue = result.get(key);

            if (oldValue == null && !result.containsKey(key)) {
                result.put(key, valueExtractor.apply(e));
            } else {
                result.put(key, mergeFunction.apply(oldValue, valueExtractor.apply(e)));
            }
        }

        return result;
    }

    /**
     *
     *
     * @param <T>
     * @param <K> the key type
     * @param iter
     * @param keyMapper
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, K> Map<K, T> toMap(final Iterator<? extends T> iter, final Function<? super T, K> keyMapper) throws IllegalArgumentException {
        N.checkArgNotNull(keyMapper);

        if (iter == null) {
            return new HashMap<>();
        }

        final Map<K, T> result = new HashMap<>();
        T e = null;

        while (iter.hasNext()) {
            e = iter.next();
            result.put(keyMapper.apply(e), e);
        }

        return result;
    }

    /**
     *
     *
     * @param <T>
     * @param <K> the key type
     * @param <V> the value type
     * @param iter
     * @param keyMapper
     * @param valueExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, K, V> Map<K, V> toMap(final Iterator<? extends T> iter, final Function<? super T, K> keyMapper,
            final Function<? super T, ? extends V> valueExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyMapper);
        N.checkArgNotNull(valueExtractor);

        if (iter == null) {
            return new HashMap<>();
        }

        final Map<K, V> result = new HashMap<>();
        T e = null;

        while (iter.hasNext()) {
            e = iter.next();
            result.put(keyMapper.apply(e), valueExtractor.apply(e));
        }

        return result;
    }

    /**
     *
     *
     * @param <T>
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param iter
     * @param keyMapper
     * @param valueExtractor
     * @param mapSupplier
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, K, V, M extends Map<K, V>> M toMap(final Iterator<? extends T> iter, final Function<? super T, K> keyMapper,
            final Function<? super T, ? extends V> valueExtractor, final Supplier<? extends M> mapSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(keyMapper);
        N.checkArgNotNull(valueExtractor);
        N.checkArgNotNull(mapSupplier);

        if (iter == null) {
            return mapSupplier.get();
        }

        final M result = mapSupplier.get();
        T e = null;

        while (iter.hasNext()) {
            e = iter.next();
            result.put(keyMapper.apply(e), valueExtractor.apply(e));
        }

        return result;
    }

    /**
     *
     *
     * @param <T>
     * @param <K>
     * @param <V>
     * @param <M>
     * @param iter
     * @param keyMapper
     * @param valueExtractor
     * @param mergeFunction
     * @param mapSupplier
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, K, V, M extends Map<K, V>> M toMap(final Iterator<? extends T> iter, final Function<? super T, K> keyMapper,
            final Function<? super T, ? extends V> valueExtractor, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapSupplier)
            throws IllegalArgumentException {
        N.checkArgNotNull(keyMapper);
        N.checkArgNotNull(valueExtractor);
        N.checkArgNotNull(mergeFunction);
        N.checkArgNotNull(mapSupplier);

        if (iter == null) {
            return mapSupplier.get();
        }

        final M result = mapSupplier.get();
        T e = null;
        K key = null;

        while (iter.hasNext()) {
            e = iter.next();
            key = keyMapper.apply(e);

            final V oldValue = result.get(key);

            if (oldValue == null && !result.containsKey(key)) {
                result.put(key, valueExtractor.apply(e));
            } else {
                result.put(key, mergeFunction.apply(oldValue, valueExtractor.apply(e)));
            }
        }

        return result;
    }

    /**
     * The input array is returned.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> T[] asArray(final T... a) {
        return a;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <T>
     * @param m
     * @param a
     * @return
     */
    @SuppressWarnings("unchecked")
    static <K, V, T extends Map<K, V>> T newMap(final T m, final Object... a) {
        if (isEmpty(a)) {
            return m;
        }

        if (a.length == 1) {
            if (a[0] instanceof Map) {
                m.putAll((Map<K, V>) a[0]);
            } else if (ClassUtil.isBeanClass(a[0].getClass())) {
                Maps.bean2Map(a[0], (Map<String, Object>) m);
            } else {
                throw new IllegalArgumentException(
                        "The parameters must be the pairs of property name and value, or Map, or a bean class with getter/setter methods.");
            }
        } else {
            if (0 != (a.length % 2)) {
                throw new IllegalArgumentException(
                        "The parameters must be the pairs of property name and value, or Map, or a bean class with getter/setter methods.");
            }

            for (int i = 0; i < a.length; i++) {
                m.put((K) a[i], (V) a[++i]);
            }
        }

        return m;
    }

    /**
     * Return a modifiable {@code Map} with specified key/value.
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1
     * @param v1
     * @return
     */
    public static <K, V> Map<K, V> asMap(final K k1, final V v1) {
        final Map<K, V> map = N.newHashMap(1);
        map.put(k1, v1);
        return map;
    }

    /**
     * Return a modifiable {@code Map} with specified keys/values.
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @return
     */
    public static <K, V> Map<K, V> asMap(final K k1, final V v1, final K k2, final V v2) {
        final Map<K, V> map = N.newHashMap(2);
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    /**
     * Return a modifiable {@code Map} with specified keys/values.
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @return
     */
    public static <K, V> Map<K, V> asMap(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3) {
        final Map<K, V> map = N.newHashMap(3);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }

    /**
     * Return a modifiable {@code Map} with specified keys/values.
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @param k4
     * @param v4
     * @return
     */
    public static <K, V> Map<K, V> asMap(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4) {
        final Map<K, V> map = N.newHashMap(4);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        return map;
    }

    /**
     * Return a modifiable {@code Map} with specified keys/values.
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @param k4
     * @param v4
     * @param k5
     * @param v5
     * @return
     */
    public static <K, V> Map<K, V> asMap(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5) {
        final Map<K, V> map = N.newHashMap(5);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        return map;
    }

    /**
     * Return a modifiable {@code Map} with specified keys/values.
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @param k4
     * @param v4
     * @param k5
     * @param v5
     * @param k6
     * @param v6
     * @return
     */
    public static <K, V> Map<K, V> asMap(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5, final V v5,
            final K k6, final V v6) {
        final Map<K, V> map = N.newHashMap(6);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        return map;
    }

    /**
     * Return a modifiable {@code Map} with specified keys/values.
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @param k4
     * @param v4
     * @param k5
     * @param v5
     * @param k6
     * @param v6
     * @param k7
     * @param v7
     * @return
     */
    public static <K, V> Map<K, V> asMap(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5, final V v5,
            final K k6, final V v6, final K k7, final V v7) {
        final Map<K, V> map = N.newHashMap(7);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        return map;
    }

    /**
     * Return a modifiable {@code Map} with specified key/value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param a
     * @return
     * @deprecated
     */
    @Deprecated
    @SafeVarargs
    @NullSafe
    public static <K, V> Map<K, V> asMap(final Object... a) {
        if (isEmpty(a)) {
            return new HashMap<>();
        }

        return newMap(N.<K, V> newHashMap(a.length / 2), a);
    }

    /**
     * Return a modifiable {@code LinkedHashMap} with specified keys/values.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1
     * @param v1
     * @return
     */
    public static <K, V> Map<K, V> asLinkedHashMap(final K k1, final V v1) {
        final Map<K, V> map = N.newLinkedHashMap(1);
        map.put(k1, v1);
        return map;
    }

    /**
     * Return a modifiable {@code LinkedHashMap} with specified keys/values.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @return
     */
    public static <K, V> Map<K, V> asLinkedHashMap(final K k1, final V v1, final K k2, final V v2) {
        final Map<K, V> map = N.newLinkedHashMap(2);
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    /**
     * Return a modifiable {@code LinkedHashMap} with specified keys/values.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @return
     */
    public static <K, V> Map<K, V> asLinkedHashMap(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3) {
        final Map<K, V> map = N.newLinkedHashMap(3);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }

    /**
     * Return a modifiable {@code LinkedHashMap} with specified keys/values.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @param k4
     * @param v4
     * @return
     */
    public static <K, V> Map<K, V> asLinkedHashMap(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4) {
        final Map<K, V> map = N.newLinkedHashMap(4);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        return map;
    }

    /**
     * Return a modifiable {@code LinkedHashMap} with specified keys/values.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @param k4
     * @param v4
     * @param k5
     * @param v5
     * @return
     */
    public static <K, V> Map<K, V> asLinkedHashMap(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5) {
        final Map<K, V> map = N.newLinkedHashMap(5);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        return map;
    }

    /**
     * Return a modifiable {@code LinkedHashMap} with specified keys/values.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @param k4
     * @param v4
     * @param k5
     * @param v5
     * @param k6
     * @param v6
     * @return
     */
    public static <K, V> Map<K, V> asLinkedHashMap(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6) {
        final Map<K, V> map = N.newLinkedHashMap(6);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        return map;
    }

    /**
     * Return a modifiable {@code LinkedHashMap} with specified keys/values.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @param k4
     * @param v4
     * @param k5
     * @param v5
     * @param k6
     * @param v6
     * @param k7
     * @param v7
     * @return
     */
    public static <K, V> Map<K, V> asLinkedHashMap(final K k1, final V v1, final K k2, final V v2, final K k3, final V v3, final K k4, final V v4, final K k5,
            final V v5, final K k6, final V v6, final K k7, final V v7) {
        final Map<K, V> map = N.newLinkedHashMap(7);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        return map;
    }

    /**
     * Return a modifiable {@code LinkedHashMap} with specified keys/values.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param a
     * @return
     * @deprecated
     */
    @Deprecated
    @SafeVarargs
    public static <K, V> Map<K, V> asLinkedHashMap(final Object... a) {
        if (isEmpty(a)) {
            return N.newLinkedHashMap();
        }

        return newMap(N.<K, V> newLinkedHashMap(a.length / 2), a);
    }

    /**
     * Return a modifiable {@code Map} with specified key/value.
     *
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static Map<String, Object> asProps(final String propName, final Object propValue) {
        final Map<String, Object> props = N.newLinkedHashMap(1);
        props.put(propName, propValue);

        return props;
    }

    /**
     * Return a modifiable {@code Map} with specified keys/values.
     *
     *
     * @param propName1
     * @param propValue1
     * @param propName2
     * @param propValue2
     * @return
     */
    public static Map<String, Object> asProps(final String propName1, final Object propValue1, final String propName2, final Object propValue2) {
        final Map<String, Object> props = N.newLinkedHashMap(2);
        props.put(propName1, propValue1);
        props.put(propName2, propValue2);

        return props;
    }

    /**
     * Return a modifiable {@code Map} with specified keys/values.
     *
     *
     * @param propName1
     * @param propValue1
     * @param propName2
     * @param propValue2
     * @param propName3
     * @param propValue3
     * @return
     */
    public static Map<String, Object> asProps(final String propName1, final Object propValue1, final String propName2, final Object propValue2,
            final String propName3, final Object propValue3) {
        final Map<String, Object> props = N.newLinkedHashMap(3);
        props.put(propName1, propValue1);
        props.put(propName2, propValue2);
        props.put(propName3, propValue3);

        return props;
    }

    /**
     * Return a modifiable {@code Map} with specified keys/values.
     *
     *
     * @param propName1
     * @param propValue1
     * @param propName2
     * @param propValue2
     * @param propName3
     * @param propValue3
     * @param propName4
     * @param propValue4
     * @return
     */
    public static Map<String, Object> asProps(final String propName1, final Object propValue1, final String propName2, final Object propValue2,
            final String propName3, final Object propValue3, final String propName4, final Object propValue4) {
        final Map<String, Object> props = N.newLinkedHashMap(4);
        props.put(propName1, propValue1);
        props.put(propName2, propValue2);
        props.put(propName3, propValue3);
        props.put(propName4, propValue4);

        return props;
    }

    /**
     * Return a modifiable {@code Map} with specified keys/values.
     *
     *
     * @param propName1
     * @param propValue1
     * @param propName2
     * @param propValue2
     * @param propName3
     * @param propValue3
     * @param propName4
     * @param propValue4
     * @param propName5
     * @param propValue5
     * @return
     */
    public static Map<String, Object> asProps(final String propName1, final Object propValue1, final String propName2, final Object propValue2,
            final String propName3, final Object propValue3, final String propName4, final Object propValue4, final String propName5, final Object propValue5) {
        final Map<String, Object> props = N.newLinkedHashMap(5);
        props.put(propName1, propValue1);
        props.put(propName2, propValue2);
        props.put(propName3, propValue3);
        props.put(propName4, propValue4);
        props.put(propName5, propValue5);

        return props;
    }

    /**
     * Return a modifiable {@code Map} with specified keys/values.
     *
     * @param a pairs of property name and value or a Java Bean Object what
     *            allows access to properties using getter and setter methods.
     * @return
     * @deprecated
     */
    @Deprecated
    @SafeVarargs
    public static Map<String, Object> asProps(final Object... a) {
        if (isEmpty(a)) {
            return N.newLinkedHashMap();
        }

        return newMap(N.<String, Object> newLinkedHashMap(a.length / 2), a);
    }

    /**
     * Return a modifiable {@code List} with specified element.
     *
     * @param <T>
     * @param e
     * @return
     */
    public static <T> List<T> asList(final T e) {
        final List<T> list = new ArrayList<>(1);
        list.add(e);
        return list;
    }

    /**
     * Return a modifiable {@code List} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @return
     */
    public static <T> List<T> asList(final T e1, final T e2) {
        final List<T> list = new ArrayList<>(2);
        list.add(e1);
        list.add(e2);
        return list;
    }

    /**
     * Return a modifiable {@code List} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @return
     */
    public static <T> List<T> asList(final T e1, final T e2, final T e3) {
        final List<T> list = new ArrayList<>(3);
        list.add(e1);
        list.add(e2);
        list.add(e3);
        return list;
    }

    /**
     * Return a modifiable {@code List} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @return
     */
    public static <T> List<T> asList(final T e1, final T e2, final T e3, final T e4) {
        final List<T> list = new ArrayList<>(4);
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        return list;
    }

    /**
     * Return a modifiable {@code List} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @return
     */
    public static <T> List<T> asList(final T e1, final T e2, final T e3, final T e4, final T e5) {
        final List<T> list = new ArrayList<>(5);
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        list.add(e5);
        return list;
    }

    /**
     * Return a modifiable {@code List} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @param e6
     * @return
     */
    public static <T> List<T> asList(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6) {
        final List<T> list = new ArrayList<>(6);
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        list.add(e5);
        list.add(e6);
        return list;
    }

    /**
     * Return a modifiable {@code List} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @param e6
     * @param e7
     * @return
     */
    public static <T> List<T> asList(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6, final T e7) {
        final List<T> list = new ArrayList<>(7);
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        list.add(e5);
        list.add(e6);
        list.add(e7);
        return list;
    }

    /**
     * Return a modifiable {@code List} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @param e6
     * @param e7
     * @param e8
     * @return
     */
    public static <T> List<T> asList(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6, final T e7, final T e8) {
        final List<T> list = new ArrayList<>(8);
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        list.add(e5);
        list.add(e6);
        list.add(e7);
        list.add(e8);
        return list;
    }

    /**
     * Return a modifiable {@code List} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @param e6
     * @param e7
     * @param e8
     * @param e9
     * @return
     */
    public static <T> List<T> asList(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6, final T e7, final T e8, final T e9) {
        final List<T> list = new ArrayList<>(9);
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        list.add(e5);
        list.add(e6);
        list.add(e7);
        list.add(e8);
        list.add(e9);
        return list;
    }

    /**
     * Return a modifiable {@code List} with specified elements. It's not backed by the specified array.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    @NullSafe
    public static <T> List<T> asList(final T... a) {
        if (isEmpty(a)) {
            return new ArrayList<>();
        }

        final List<T> list = new ArrayList<>(a.length);

        if (a.length < MIN_SIZE_FOR_COPY_ALL) {
            Collections.addAll(list, a);
        } else {
            list.addAll(Arrays.asList(a));
        }

        return list;
    }

    /**
     * Return a modifiable {@code LinkedList} with specified element.
     *
     * @param <T>
     * @param e
     * @return
     */
    public static <T> LinkedList<T> asLinkedList(final T e) { //NOSONAR
        final LinkedList<T> list = new LinkedList<>();
        list.add(e);
        return list;
    }

    /**
     * Return a modifiable {@code LinkedList} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @return
     */
    public static <T> LinkedList<T> asLinkedList(final T e1, final T e2) { //NOSONAR
        final LinkedList<T> list = new LinkedList<>();
        list.add(e1);
        list.add(e2);
        return list;
    }

    /**
     * Return a modifiable {@code LinkedList} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @return
     */
    public static <T> LinkedList<T> asLinkedList(final T e1, final T e2, final T e3) { //NOSONAR
        final LinkedList<T> list = new LinkedList<>();
        list.add(e1);
        list.add(e2);
        list.add(e3);
        return list;
    }

    /**
     * Return a modifiable {@code LinkedList} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @return
     */
    public static <T> LinkedList<T> asLinkedList(final T e1, final T e2, final T e3, final T e4) { //NOSONAR
        final LinkedList<T> list = new LinkedList<>();
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        return list;
    }

    /**
     * Return a modifiable {@code LinkedList} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @return
     */
    public static <T> LinkedList<T> asLinkedList(final T e1, final T e2, final T e3, final T e4, final T e5) { //NOSONAR
        final LinkedList<T> list = new LinkedList<>();
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        list.add(e5);
        return list;
    }

    /**
     * Return a modifiable {@code LinkedList} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @param e6
     * @return
     */
    public static <T> LinkedList<T> asLinkedList(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6) { //NOSONAR
        final LinkedList<T> list = new LinkedList<>();
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        list.add(e5);
        list.add(e6);
        return list;
    }

    /**
     * Return a modifiable {@code LinkedList} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @param e6
     * @param e7
     * @return
     */
    public static <T> LinkedList<T> asLinkedList(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6, final T e7) { //NOSONAR
        final LinkedList<T> list = new LinkedList<>();
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        list.add(e5);
        list.add(e6);
        list.add(e7);
        return list;
    }

    /**
     * Return a modifiable {@code LinkedList} with specified elements. . It's not backed by the specified array.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    @NullSafe
    public static <T> LinkedList<T> asLinkedList(final T... a) { //NOSONAR
        if (isEmpty(a)) {
            return new LinkedList<>();
        }

        final LinkedList<T> list = new LinkedList<>();

        Collections.addAll(list, a);

        return list;
    }

    /**
     * Return a modifiable {@code Set} with specified element.
     *
     * @param <T>
     * @param e
     * @return
     */
    public static <T> Set<T> asSet(final T e) {
        final Set<T> set = newHashSet(1);
        set.add(e);
        return set;
    }

    /**
     * Return a modifiable {@code Set} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @return
     */
    public static <T> Set<T> asSet(final T e1, final T e2) {
        final Set<T> set = newHashSet(2);
        set.add(e1);
        set.add(e2);
        return set;
    }

    /**
     * Return a modifiable {@code Set} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @return
     */
    public static <T> Set<T> asSet(final T e1, final T e2, final T e3) {
        final Set<T> set = newHashSet(3);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        return set;
    }

    /**
     * Return a modifiable {@code Set} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @return
     */
    public static <T> Set<T> asSet(final T e1, final T e2, final T e3, final T e4) {
        final Set<T> set = newHashSet(4);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        return set;
    }

    /**
     * Return a modifiable {@code Set} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @return
     */
    public static <T> Set<T> asSet(final T e1, final T e2, final T e3, final T e4, final T e5) {
        final Set<T> set = newHashSet(5);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        set.add(e5);
        return set;
    }

    /**
     * Return a modifiable {@code Set} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @param e6
     * @return
     */
    public static <T> Set<T> asSet(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6) {
        final Set<T> set = newHashSet(6);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        set.add(e5);
        set.add(e6);
        return set;
    }

    /**
     * Return a modifiable {@code Set} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @param e6
     * @param e7
     * @return
     */
    public static <T> Set<T> asSet(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6, final T e7) {
        final Set<T> set = newHashSet(7);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        set.add(e5);
        set.add(e6);
        set.add(e7);
        return set;
    }

    /**
     * Return a modifiable {@code Set} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @param e6
     * @param e7
     * @param e8
     * @return
     */
    public static <T> Set<T> asSet(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6, final T e7, final T e8) {
        final Set<T> set = newHashSet(8);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        set.add(e5);
        set.add(e6);
        set.add(e7);
        set.add(e8);
        return set;
    }

    /**
     * Return a modifiable {@code Set} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @param e6
     * @param e7
     * @param e8
     * @param e9
     * @return
     */
    public static <T> Set<T> asSet(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6, final T e7, final T e8, final T e9) {
        final Set<T> set = newHashSet(9);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        set.add(e5);
        set.add(e6);
        set.add(e7);
        set.add(e8);
        set.add(e9);
        return set;
    }

    /**
     * Return a modifiable {@code Set} with specified elements. It's not backed by the specified array.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    @NullSafe
    public static <T> Set<T> asSet(final T... a) {
        if (isEmpty(a)) {
            return newHashSet();
        }

        final Set<T> set = newHashSet(a.length);

        Collections.addAll(set, a);

        return set;
    }

    /**
     * Return a modifiable {@code LinkedHashSet} with specified element.
     *
     * @param <T>
     * @param e
     * @return
     */
    public static <T> Set<T> asLinkedHashSet(final T e) {
        final Set<T> set = newLinkedHashSet(1);
        set.add(e);
        return set;
    }

    /**
     * Return a modifiable {@code LinkedHashSet} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @return
     */
    public static <T> Set<T> asLinkedHashSet(final T e1, final T e2) {
        final Set<T> set = newLinkedHashSet(2);
        set.add(e1);
        set.add(e2);
        return set;
    }

    /**
     * Return a modifiable {@code LinkedHashSet} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @return
     */
    public static <T> Set<T> asLinkedHashSet(final T e1, final T e2, final T e3) {
        final Set<T> set = newLinkedHashSet(3);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        return set;
    }

    /**
     * Return a modifiable {@code LinkedHashSet} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @return
     */
    public static <T> Set<T> asLinkedHashSet(final T e1, final T e2, final T e3, final T e4) {
        final Set<T> set = newLinkedHashSet(4);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        return set;
    }

    /**
     * Return a modifiable {@code LinkedHashSet} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @return
     */
    public static <T> Set<T> asLinkedHashSet(final T e1, final T e2, final T e3, final T e4, final T e5) {
        final Set<T> set = newLinkedHashSet(5);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        set.add(e5);
        return set;
    }

    /**
     * Return a modifiable {@code LinkedHashSet} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @param e6
     * @return
     */
    public static <T> Set<T> asLinkedHashSet(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6) {
        final Set<T> set = newLinkedHashSet(6);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        set.add(e5);
        set.add(e6);
        return set;
    }

    /**
     * Return a modifiable {@code LinkedHashSet} with specified elements.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @param e6
     * @param e7
     * @return
     */
    public static <T> Set<T> asLinkedHashSet(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6, final T e7) {
        final Set<T> set = newLinkedHashSet(7);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        set.add(e5);
        set.add(e6);
        set.add(e7);
        return set;
    }

    /**
     * Return a modifiable {@code LinkedHashSet} with specified elements. It's not backed by the specified array.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    @NullSafe
    public static <T> Set<T> asLinkedHashSet(final T... a) {
        if (isEmpty(a)) {
            return newLinkedHashSet();
        }

        final Set<T> set = newLinkedHashSet(a.length);

        Collections.addAll(set, a);

        return set;
    }

    /**
     * Return a modifiable {@code SortedSet} with specified elements. It's not backed by the specified array.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    @NullSafe
    public static <T> SortedSet<T> asSortedSet(final T... a) {
        if (isEmpty(a)) {
            return new TreeSet<>();
        }

        final SortedSet<T> set = new TreeSet<>();

        Collections.addAll(set, a);

        return set;
    }

    /**
     * Return a modifiable {@code NavigableSet} with specified elements. It's not backed by the specified array.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> NavigableSet<T> asNavigableSet(final T... a) {
        if (isEmpty(a)) {
            return new TreeSet<>();
        }

        final NavigableSet<T> set = new TreeSet<>();

        Collections.addAll(set, a);

        return set;
    }

    /**
     * Return a modifiable {@code Queue} with specified elements. It's not backed by the specified array.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> Queue<T> asQueue(final T... a) {
        return asArrayDeque(a);
    }

    /**
     * Return a modifiable {@code ArrayBlockingQueue} with specified elements. It's not backed by the specified array.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> ArrayBlockingQueue<T> asArrayBlockingQueue(final T... a) {
        if (isEmpty(a)) {
            return new ArrayBlockingQueue<>(0);
        }

        final ArrayBlockingQueue<T> queue = new ArrayBlockingQueue<>(a.length);

        Collections.addAll(queue, a);

        return queue;
    }

    /**
     * Return a modifiable {@code LinkedBlockingQueue} with specified elements. It's not backed by the specified array.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> LinkedBlockingQueue<T> asLinkedBlockingQueue(final T... a) {
        if (isEmpty(a)) {
            return new LinkedBlockingQueue<>();
        }

        final LinkedBlockingQueue<T> queue = new LinkedBlockingQueue<>(a.length);

        Collections.addAll(queue, a);

        return queue;
    }

    /**
     * Return a modifiable {@code ConcurrentLinkedQueue} with specified elements. It's not backed by the specified array.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> ConcurrentLinkedQueue<T> asConcurrentLinkedQueue(final T... a) { //NOSONAR
        if (isEmpty(a)) {
            return new ConcurrentLinkedQueue<>();
        }

        final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();

        Collections.addAll(queue, a);

        return queue;
    }

    /**
     * Return a modifiable {@code DelayQueue} with specified elements. It's not backed by the specified array.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T extends Delayed> DelayQueue<T> asDelayQueue(final T... a) {
        if (isEmpty(a)) {
            return new DelayQueue<>();
        }

        final DelayQueue<T> queue = new DelayQueue<>();

        Collections.addAll(queue, a);

        return queue;
    }

    /**
     * Return a modifiable {@code PriorityQueue} with specified elements. It's not backed by the specified array.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> PriorityQueue<T> asPriorityQueue(final T... a) {
        if (isEmpty(a)) {
            return new PriorityQueue<>();
        }

        final PriorityQueue<T> queue = new PriorityQueue<>(a.length);

        Collections.addAll(queue, a);

        return queue;
    }

    /**
     * Return a modifiable {@code Deque} with specified elements. It's not backed by the specified array.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> Deque<T> asDeque(final T... a) {
        return asArrayDeque(a);
    }

    /**
     * Return a modifiable {@code ArrayDeque} with specified elements. It's not backed by the specified array.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> ArrayDeque<T> asArrayDeque(final T... a) { //NOSONAR
        if (isEmpty(a)) {
            return new ArrayDeque<>();
        }

        final ArrayDeque<T> arrayDeque = new ArrayDeque<>(a.length);

        Collections.addAll(arrayDeque, a);

        return arrayDeque;
    }

    /**
     * Return a modifiable {@code LinkedBlockingDeque} with specified elements. It's not backed by the specified array.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> LinkedBlockingDeque<T> asLinkedBlockingDeque(final T... a) {
        if (isEmpty(a)) {
            return new LinkedBlockingDeque<>();
        }

        final LinkedBlockingDeque<T> deque = new LinkedBlockingDeque<>(a.length);

        Collections.addAll(deque, a);

        return deque;
    }

    /**
     * Return a modifiable {@code ConcurrentLinkedDeque} with specified elements. It's not backed by the specified array.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> ConcurrentLinkedDeque<T> asConcurrentLinkedDeque(final T... a) { //NOSONAR
        if (isEmpty(a)) {
            return new ConcurrentLinkedDeque<>();
        }

        final ConcurrentLinkedDeque<T> deque = new ConcurrentLinkedDeque<>();

        Collections.addAll(deque, a);

        return deque;
    }

    /**
     * Return a modifiable {@code Multiset} with specified elements. It's not backed by the specified array.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> Multiset<T> asMultiset(final T... a) {
        return Multiset.of(a);
    }

    /**
     * Wrap the specified value with a singleton list.
     *
     * @param <T>
     * @param o
     * @return an immutable list
     * @see java.util.Collections#singletonList(Object)
     */
    @Immutable
    public static <T> List<T> asSingletonList(final T o) {
        return Collections.singletonList(o);
    }

    /**
     * Wrap the specified value with a singleton set.
     *
     * @param <T>
     * @param o
     * @return an immutable set
     * @see java.util.Collections#singleton(Object)
     */
    @Immutable
    public static <T> Set<T> asSingletonSet(final T o) {
        return Collections.singleton(o);
    }

    /**
     * Wrap the specified key/value with a singleton map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param key
     * @param value
     * @return an immutable Map
     * @see java.util.Collections#singletonMap(Object, Object)
     */
    @Immutable
    public static <K, V> Map<K, V> asSingletonMap(final K key, final V value) {
        return Collections.singletonMap(key, value);
    }

    /**
     * Returns an empty immutable  {@code List}.
     *
     * @param <T>
     * @return
     * @see Collections#emptyList()
     */
    @Immutable
    public static <T> List<T> emptyList() {
        return EMPTY_LIST;
    }

    /**
     * Returns an empty immutable  {@code Set}.
     *
     * @param <T>
     * @return
     * @see Collections#emptySet()
     */
    @Immutable
    public static <T> Set<T> emptySet() {
        return EMPTY_SET;
    }

    /**
     * Returns an empty immutable  {@code SortedSet}.
     *
     * @param <T>
     * @return
     * @see Collections#emptySortedSet()
     */
    @Immutable
    public static <T> SortedSet<T> emptySortedSet() {
        return EMPTY_SORTED_SET;
    }

    /**
     * Returns an empty immutable  {@code NavigableSet}.
     *
     * @param <T>
     * @return
     * @see Collections#emptyNavigableSet()
     */
    @Immutable
    public static <T> NavigableSet<T> emptyNavigableSet() {
        return EMPTY_NAVIGABLE_SET;
    }

    /**
     * Returns an empty immutable  {@code Map}.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     * @see Collections#emptyMap()
     */
    @Immutable
    public static <K, V> Map<K, V> emptyMap() {
        return EMPTY_MAP;
    }

    /**
     * Returns an empty immutable  {@code SortedMap}.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     * @see Collections#emptySortedMap()
     */
    @Immutable
    public static <K, V> SortedMap<K, V> emptySortedMap() {
        return EMPTY_SORTED_MAP;
    }

    /**
     * Returns an empty immutable  {@code NavigableMap}.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     * @see Collections#emptyNavigableMap()
     */
    @Immutable
    public static <K, V> NavigableMap<K, V> emptyNavigableMap() {
        return EMPTY_NAVIGABLE_MAP;
    }

    /**
     * Returns an empty immutable  {@code Iterator}.
     *
     * @param <T>
     * @return
     * @see Collections#emptyIterator()
     */
    public static <T> Iterator<T> emptyIterator() {
        return EMPTY_ITERATOR;
    }

    /**
     * Returns an empty immutable  {@code ListIterator}.
     *
     * @param <T>
     * @return
     * @see Collections#emptyListIterator()
     */
    @Immutable
    public static <T> ListIterator<T> emptyListIterator() {
        return EMPTY_LIST_ITERATOR;
    }

    private static final ByteArrayInputStream EMPTY_INPUT_STREAM = new ByteArrayInputStream(EMPTY_BYTE_ARRAY);

    /**
     * Returns an empty immutable  {@code InputStream}.
     *
     * @return
     */
    @Immutable
    public static InputStream emptyInputStream() {
        return EMPTY_INPUT_STREAM;
    }

    /**
     * Returns an unmodifiable empty {@code DataSet}.
     *
     * @return
     * @see DataSet#empty()
     */
    @Immutable
    public static DataSet emptyDataSet() {
        return DataSet.empty();
    }

    /**
     *
     *
     * @param <T>
     * @param c
     * @param index
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException the index out of bounds exception
     */
    public static <T> T getElement(final Iterable<? extends T> c, int index) throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNull(c, "c");

        if (c instanceof Collection) {
            checkIndex(index, ((Collection<T>) c).size());
        }

        if (c instanceof List) {
            return ((List<T>) c).get(index);
        }

        return getElement(c.iterator(), index);
    }

    /**
     *
     *
     * @param <T>
     * @param iter
     * @param index
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException the index out of bounds exception
     */
    public static <T> T getElement(final Iterator<? extends T> iter, long index) throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNull(iter, "iter");

        while (index-- > 0 && iter.hasNext()) {
            iter.next();
        }

        if (iter.hasNext()) {
            return iter.next();
        } else {
            throw new IndexOutOfBoundsException("Index: " + index + " is bigger than the maximum index of the specified Iterable or Iterator");
        }
    }

    /**
     * Gets the only element.
     *
     * @param <T>
     * @param c
     * @return throws TooManyElementsException if there are more than one elements in the specified {@code iterable}.
     * @throws TooManyElementsException
     */
    public static <T> Nullable<T> getOnlyElement(Iterable<? extends T> c) throws TooManyElementsException {
        if (c == null) {
            return Nullable.empty();
        }

        if (c instanceof Collection && ((Collection<T>) c).size() > 1) {
            final Iterator<? extends T> iter = c.iterator();

            throw new TooManyElementsException("Expected at most one element but was: [" + Strings.concat(iter.next(), ", ", iter.next(), "...]"));
        }

        return getOnlyElement(c.iterator());
    }

    /**
     * Gets the only element.
     *
     * @param <T>
     * @param iter
     * @return throws TooManyElementsException if there are more than one elements in the specified {@code iter}.
     * @throws TooManyElementsException the duplicated result exception
     */
    public static <T> Nullable<T> getOnlyElement(final Iterator<? extends T> iter) throws TooManyElementsException {
        if (iter == null) {
            return Nullable.empty();
        }

        final T first = iter.next();

        if (iter.hasNext()) {
            throw new TooManyElementsException("Expected at most one element but was: [" + Strings.concat(first, ", ", iter.next(), "...]"));
        }

        return Nullable.of(first);
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> Nullable<T> firstElement(final Iterable<? extends T> c) {
        if (isEmpty(c)) {
            return Nullable.empty();
        }

        if (c instanceof List && c instanceof RandomAccess) {
            return Nullable.of(((List<T>) c).get(0));
        } else {
            return Nullable.of(c.iterator().next());
        }
    }

    /**
     *
     * @param <T>
     * @param iter
     * @return
     */
    public static <T> Nullable<T> firstElement(final Iterator<? extends T> iter) {
        return iter != null && iter.hasNext() ? Nullable.of(iter.next()) : Nullable.<T> empty();
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> Nullable<T> lastElement(final Iterable<? extends T> c) {
        if (isEmpty(c)) {
            return Nullable.empty();
        }

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            return Nullable.of(list.get(list.size() - 1));
        }

        final Iterator<T> descendingIterator = getDescendingIteratorIfPossible(c);

        if (descendingIterator != null) {
            return Nullable.of(descendingIterator.next());
        }

        return lastElement(c.iterator());
    }

    /**
     *
     * @param <T>
     * @param iter
     * @return
     */
    public static <T> Nullable<T> lastElement(final Iterator<? extends T> iter) {
        if (iter == null || !iter.hasNext()) {
            return Nullable.empty();
        }

        T e = null;

        while (iter.hasNext()) {
            e = iter.next();
        }

        return Nullable.of(e);
    }

    /**
     * Return at most first <code>n</code> elements.
     *
     * @param <T>
     * @param c
     * @param n
     * @return the list
     * @throws IllegalArgumentException
     */
    @Beta
    public static <T> List<T> firstElements(final Collection<? extends T> c, final int n) throws IllegalArgumentException {
        N.checkArgument(n >= 0, "'n' can't be negative: " + n);

        if (N.isEmpty(c) || n == 0) {
            return new ArrayList<>();
        } else if (c.size() <= n) {
            return new ArrayList<>(c);
        } else if (c instanceof List) {
            return new ArrayList<>(((List<T>) c).subList(0, n));
        } else {
            final List<T> result = new ArrayList<>(N.min(n, c.size()));
            int cnt = 0;

            for (T e : c) {
                result.add(e);

                if (++cnt == n) {
                    break;
                }
            }

            return result;
        }
    }

    /**
     * Return at most last <code>n</code> elements.
     *
     * @param <T>
     * @param c
     * @param n
     * @return the list
     * @throws IllegalArgumentException
     */
    @Beta
    public static <T> List<T> lastElements(final Collection<? extends T> c, final int n) throws IllegalArgumentException {
        N.checkArgument(n >= 0, "'n' can't be negative: " + n);

        if (N.isEmpty(c) || n == 0) {
            return new ArrayList<>();
        } else if (c.size() <= n) {
            return new ArrayList<>(c);
        } else if (c instanceof List) {
            return new ArrayList<>(((List<T>) c).subList(c.size() - n, c.size()));
        } else {
            final List<T> result = new ArrayList<>(N.min(n, c.size()));
            final Iterator<? extends T> iter = c.iterator();
            int offset = c.size() - n;

            while (offset-- > 0) {
                iter.next();
            }

            while (iter.hasNext()) {
                result.add(iter.next());
            }

            return result;
        }
    }

    /**
     * First non null.
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    public static <T> Optional<T> firstNonNull(final T a, final T b) {
        return a != null ? Optional.of(a) : (b != null ? Optional.of(b) : Optional.<T> empty());
    }

    /**
     * First non null.
     *
     * @param <T>
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static <T> Optional<T> firstNonNull(final T a, final T b, final T c) {
        return a != null ? Optional.of(a) : (b != null ? Optional.of(b) : (c != null ? Optional.of(c) : Optional.<T> empty()));
    }

    /**
     * First non null.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> Optional<T> firstNonNull(final T... a) {
        if (isEmpty(a)) {
            return Optional.empty();
        }

        for (T e : a) {
            if (e != null) {
                return Optional.of(e);
            }
        }

        return Optional.empty();
    }

    /**
     * First non null.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> Optional<T> firstNonNull(final Iterable<? extends T> c) {
        if (isEmpty(c)) {
            return Optional.empty();
        }

        for (T e : c) {
            if (e != null) {
                return Optional.of(e);
            }
        }

        return Optional.empty();
    }

    /**
     * First non null.
     *
     * @param <T>
     * @param iter
     * @return
     */
    public static <T> Optional<T> firstNonNull(final Iterator<? extends T> iter) {
        if (iter == null) {
            return Optional.empty();
        }

        T e = null;

        while (iter.hasNext()) {
            if ((e = iter.next()) != null) {
                return Optional.of(e);
            }
        }

        return Optional.empty();
    }

    /**
     * Last non null.
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    public static <T> Optional<T> lastNonNull(final T a, final T b) {
        return b != null ? Optional.of(b) : (a != null ? Optional.of(a) : Optional.<T> empty());
    }

    /**
     * Last non null.
     *
     * @param <T>
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static <T> Optional<T> lastNonNull(final T a, final T b, final T c) {
        return c != null ? Optional.of(c) : (b != null ? Optional.of(b) : (a != null ? Optional.of(a) : Optional.<T> empty()));
    }

    /**
     * Last non null.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> Optional<T> lastNonNull(final T... a) {
        if (isEmpty(a)) {
            return Optional.empty();
        }

        for (int i = a.length - 1; i >= 0; i--) {
            if (a[i] != null) {
                return Optional.of(a[i]);
            }
        }

        return Optional.empty();
    }

    /**
     * Last non null.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> Optional<T> lastNonNull(final Iterable<? extends T> c) {
        if (isEmpty(c)) {
            return Optional.empty();
        }

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = list.size() - 1; i >= 0; i--) {
                if (list.get(i) != null) {
                    return Optional.of(list.get(i));
                }
            }

            return Optional.empty();
        }

        final Iterator<T> descendingIterator = getDescendingIteratorIfPossible(c);

        if (descendingIterator != null) {
            T next = null;

            while (descendingIterator.hasNext()) {
                if ((next = descendingIterator.next()) != null) {
                    return Optional.of(next);
                }
            }

            return Optional.empty();
        }

        return lastNonNull(c.iterator());
    }

    /**
     * Last non null.
     *
     * @param <T>
     * @param iter
     * @return
     */
    public static <T> Optional<T> lastNonNull(final Iterator<? extends T> iter) {
        if (iter == null) {
            return Optional.empty();
        }

        T e = null;
        T lastNonNull = null;

        while (iter.hasNext()) {
            if ((e = iter.next()) != null) {
                lastNonNull = e;
            }
        }

        return Optional.ofNullable(lastNonNull);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    public static <T> Optional<T[]> firstNonEmpty(final T[] a, final T[] b) {
        return a != null && a.length > 0 ? Optional.of(a) : (b != null && b.length > 0 ? Optional.of(b) : Optional.<T[]> empty());
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static <T> Optional<T[]> firstNonEmpty(final T[] a, final T[] b, final T[] c) {
        return a != null && a.length > 0 ? Optional.of(a)
                : (b != null && b.length > 0 ? Optional.of(b) : (c != null && c.length > 0 ? Optional.of(c) : Optional.<T[]> empty()));
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    public static <T extends Collection<?>> Optional<T> firstNonEmpty(final T a, final T b) {
        return a != null && a.size() > 0 ? Optional.of(a) : (b != null && b.size() > 0 ? Optional.of(b) : Optional.<T> empty());
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static <T extends Collection<?>> Optional<T> firstNonEmpty(final T a, final T b, final T c) {
        return a != null && a.size() > 0 ? Optional.of(a)
                : (b != null && b.size() > 0 ? Optional.of(b) : (c != null && c.size() > 0 ? Optional.of(c) : Optional.<T> empty()));
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    public static <T extends CharSequence> Optional<T> firstNonEmpty(final T a, final T b) {
        return Strings.isNotEmpty(a) ? Optional.of(a) : (Strings.isNotEmpty(b) ? Optional.of(b) : Optional.<T> empty());
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static <T extends CharSequence> Optional<T> firstNonEmpty(final T a, final T b, final T c) {
        return Strings.isNotEmpty(a) ? Optional.of(a)
                : (Strings.isNotEmpty(b) ? Optional.of(b) : (Strings.isNotEmpty(c) ? Optional.of(c) : Optional.<T> empty()));
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T extends CharSequence> Optional<T> firstNonEmpty(final T... a) {
        if (N.isEmpty(a)) {
            return Optional.empty();
        }

        for (T e : a) {
            if (Strings.isNotEmpty(e)) {
                return Optional.of(e);
            }
        }

        return Optional.empty();
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    public static <T extends CharSequence> Optional<T> firstNonBlank(final T a, final T b) {
        return Strings.isNotBlank(a) ? Optional.of(a) : (Strings.isNotBlank(b) ? Optional.of(b) : Optional.<T> empty());
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static <T extends CharSequence> Optional<T> firstNonBlank(final T a, final T b, final T c) {
        return Strings.isNotBlank(a) ? Optional.of(a)
                : (Strings.isNotBlank(b) ? Optional.of(b) : (Strings.isNotBlank(c) ? Optional.of(c) : Optional.<T> empty()));
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T extends CharSequence> Optional<T> firstNonBlank(final T... a) {
        if (N.isEmpty(a)) {
            return Optional.empty();
        }

        for (T e : a) {
            if (Strings.isNotBlank(e)) {
                return Optional.of(e);
            }
        }

        return Optional.empty();
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     */
    public static <K, V> Optional<Map.Entry<K, V>> firstEntry(final Map<K, V> map) {
        if (map == null || map.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(map.entrySet().iterator().next());
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     */
    public static <K, V> Optional<Map.Entry<K, V>> lastEntry(final Map<K, V> map) {
        if (map == null || map.isEmpty()) {
            return Optional.empty();
        }

        return lastNonNull(map.entrySet().iterator());
    }

    /**
     * First or null if empty.
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T> T firstOrNullIfEmpty(final T[] a) {
        return a == null || a.length == 0 ? null : a[0];
    }

    /**
     * First or null if empty.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> T firstOrNullIfEmpty(final Iterable<? extends T> c) {
        return firstOrDefaultIfEmpty(c, null);
    }

    /**
     * First or null if empty.
     *
     * @param <T>
     * @param iter
     * @return
     */
    public static <T> T firstOrNullIfEmpty(final Iterator<? extends T> iter) {
        return firstOrDefaultIfEmpty(iter, null);
    }

    /**
     * First or default if empty.
     *
     * @param <T>
     * @param a
     * @param defaultValueForEmpty
     * @return
     */
    public static <T> T firstOrDefaultIfEmpty(final T[] a, final T defaultValueForEmpty) {
        return a == null || a.length == 0 ? defaultValueForEmpty : a[0];
    }

    /**
     * First or default if empty.
     *
     * @param <T>
     * @param c
     * @param defaultValueForEmpty
     * @return
     */
    public static <T> T firstOrDefaultIfEmpty(final Iterable<? extends T> c, final T defaultValueForEmpty) {
        if (isEmpty(c)) {
            return defaultValueForEmpty;
        }

        if (c instanceof List && c instanceof RandomAccess) {
            return ((List<T>) c).get(0);
        } else {
            return c.iterator().next();
        }
    }

    /**
     * First or default if empty.
     *
     * @param <T>
     * @param iter
     * @param defaultValueForEmpty
     * @return
     */
    public static <T> T firstOrDefaultIfEmpty(final Iterator<? extends T> iter, final T defaultValueForEmpty) {
        if (iter == null || !iter.hasNext()) {
            return defaultValueForEmpty;
        }

        return iter.next();
    }

    /**
     * Last or null if empty.
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T> T lastOrNullIfEmpty(final T[] a) {
        return a == null || a.length == 0 ? null : a[a.length - 1];
    }

    /**
     * Last or null if empty.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> T lastOrNullIfEmpty(final Iterable<? extends T> c) {
        return lastOrDefaultIfEmpty(c, null);
    }

    /**
     * Last or null if empty.
     *
     * @param <T>
     * @param iter
     * @return
     */
    public static <T> T lastOrNullIfEmpty(final Iterator<? extends T> iter) {
        return lastOrDefaultIfEmpty(iter, null);
    }

    /**
     * Last or default if empty.
     *
     * @param <T>
     * @param a
     * @param defaultValueForEmpty
     * @return
     */
    public static <T> T lastOrDefaultIfEmpty(final T[] a, final T defaultValueForEmpty) {
        return a == null || a.length == 0 ? defaultValueForEmpty : a[a.length - 1];
    }

    /**
     * Last or default if empty.
     *
     * @param <T>
     * @param c
     * @param defaultValueForEmpty
     * @return
     */
    public static <T> T lastOrDefaultIfEmpty(final Iterable<? extends T> c, final T defaultValueForEmpty) {
        if (isEmpty(c)) {
            return defaultValueForEmpty;
        }

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            return list.get(list.size() - 1);
        }

        final Iterator<T> descendingIterator = getDescendingIteratorIfPossible(c);

        if (descendingIterator != null) {
            return descendingIterator.next();
        }

        return lastOrDefaultIfEmpty(c.iterator(), defaultValueForEmpty);
    }

    /**
     * Last or default if empty.
     *
     * @param <T>
     * @param iter
     * @param defaultValueForEmpty
     * @return
     */
    public static <T> T lastOrDefaultIfEmpty(final Iterator<? extends T> iter, final T defaultValueForEmpty) {
        if (iter == null || !iter.hasNext()) {
            return defaultValueForEmpty;
        }

        T e = null;

        while (iter.hasNext()) {
            e = iter.next();
        }

        return e;
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param predicate
     * @return the nullable
     * @throws E the e
     */
    public static <T, E extends Exception> Nullable<T> findFirst(final T[] a, final Throwables.Predicate<? super T, E> predicate) throws E {
        if (N.isEmpty(a)) {
            return Nullable.empty();
        }

        for (T element : a) {
            if (predicate.test(element)) {
                return Nullable.of(element);
            }
        }

        return Nullable.empty();
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param predicate
     * @return the nullable
     * @throws E the e
     */
    public static <T, E extends Exception> Nullable<T> findFirst(final Iterable<? extends T> c, Throwables.Predicate<? super T, E> predicate) throws E {
        if (N.isEmpty(c)) {
            return Nullable.empty();
        }

        for (T e : c) {
            if (predicate.test(e)) {
                return Nullable.of(e);
            }
        }

        return Nullable.empty();
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param predicate
     * @return
     * @throws E
     */
    public static <T, E extends Exception> Nullable<T> findFirst(final Iterator<? extends T> iter, Throwables.Predicate<? super T, E> predicate) throws E {
        if (iter == null) {
            return Nullable.empty();
        }

        T next = null;

        while (iter.hasNext()) {
            next = iter.next();

            if (predicate.test(next)) {
                return Nullable.of(next);
            }
        }

        return Nullable.empty();
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param predicate
     * @return the nullable
     * @throws E the e
     */
    public static <T, E extends Exception> Nullable<T> findLast(final T[] a, final Throwables.Predicate<? super T, E> predicate) throws E {
        if (N.isEmpty(a)) {
            return Nullable.empty();
        }

        for (int len = a.length, i = len - 1; i >= 0; i--) {
            if (predicate.test(a[i])) {
                return Nullable.of(a[i]);
            }
        }

        return Nullable.empty();
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param predicate
     * @return the nullable
     * @throws E the e
     */
    public static <T, E extends Exception> Nullable<T> findLast(final Iterable<? extends T> c, Throwables.Predicate<? super T, E> predicate) throws E {
        return (Nullable<T>) findLast(c, predicate, false);
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param c
     * @param predicate
     * @param isForNonNull
     * @return the r
     * @throws E the e
     */
    private static <T, E extends Exception> Object findLast(final Iterable<? extends T> c, Throwables.Predicate<? super T, E> predicate, boolean isForNonNull)
            throws E {
        if (c == null) {
            return isForNonNull ? Optional.empty() : Nullable.empty();
        }

        T e = null;

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = list.size() - 1; i >= 0; i--) {
                e = list.get(i);

                if ((!isForNonNull || e != null) && predicate.test(e)) {
                    return isForNonNull ? Optional.of(e) : Nullable.of(e);
                }
            }

            return isForNonNull ? Optional.empty() : Nullable.empty();
        }

        final Iterator<T> descendingIterator = getDescendingIteratorIfPossible(c);

        if (descendingIterator != null) {
            while (descendingIterator.hasNext()) {
                e = descendingIterator.next();

                if ((!isForNonNull || e != null) && predicate.test(e)) {
                    return isForNonNull ? Optional.of(e) : Nullable.of(e);
                }
            }

            return isForNonNull ? Optional.empty() : Nullable.empty();
        }

        T[] a = null;

        if (c instanceof Collection) {
            a = (T[]) ((Collection<T>) c).toArray();
        } else {
            final List<T> tmp = new ArrayList<>();
            final Iterator<? extends T> iter = c.iterator();

            while (iter.hasNext()) {
                tmp.add(iter.next());
            }

            a = (T[]) tmp.toArray();
        }

        for (int i = a.length - 1; i >= 0; i--) {
            if ((!isForNonNull || a[i] != null) && predicate.test(a[i])) {
                return isForNonNull ? Optional.of(a[i]) : Nullable.of(a[i]);
            }
        }

        return isForNonNull ? Optional.empty() : Nullable.empty();
    }

    /**
     * Find first non null.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param predicate
     * @return the optional
     * @throws E the e
     */
    public static <T, E extends Exception> Optional<T> findFirstNonNull(final T[] a, final Throwables.Predicate<? super T, E> predicate) throws E {
        if (N.isEmpty(a)) {
            return Optional.empty();
        }

        for (T element : a) {
            if (element != null && predicate.test(element)) {
                return Optional.of(element);
            }
        }

        return Optional.empty();
    }

    /**
     * Find first non null.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param predicate
     * @return the optional
     * @throws E the e
     */
    public static <T, E extends Exception> Optional<T> findFirstNonNull(final Iterable<? extends T> c, Throwables.Predicate<? super T, E> predicate) throws E {
        if (N.isEmpty(c)) {
            return Optional.empty();
        }

        for (T e : c) {
            if (e != null && predicate.test(e)) {
                return Optional.of(e);
            }
        }

        return Optional.empty();
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param predicate
     * @return
     * @throws E
     */
    public static <T, E extends Exception> Optional<T> findFirstNonNull(final Iterator<? extends T> iter, Throwables.Predicate<? super T, E> predicate)
            throws E {
        if (iter == null) {
            return Optional.empty();
        }

        T next = null;

        while (iter.hasNext()) {
            next = iter.next();

            if (next != null && predicate.test(next)) {
                return Optional.of(next);
            }
        }

        return Optional.empty();
    }

    /**
     * Find last non null.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param predicate
     * @return the optional
     * @throws E the e
     */
    public static <T, E extends Exception> Optional<T> findLastNonNull(final T[] a, final Throwables.Predicate<? super T, E> predicate) throws E {
        if (N.isEmpty(a)) {
            return Optional.empty();
        }

        for (int len = a.length, i = len - 1; i >= 0; i--) {
            if (a[i] != null && predicate.test(a[i])) {
                return Optional.of(a[i]);
            }
        }

        return Optional.empty();
    }

    /**
     * Find last non null.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param predicate
     * @return the optional
     * @throws E the e
     */
    public static <T, E extends Exception> Optional<T> findLastNonNull(final Iterable<? extends T> c, Throwables.Predicate<? super T, E> predicate) throws E {
        return (Optional<T>) findLast(c, predicate, true);
    }

    /**
     * Returns the length of the specified {@code CharSequence}, or {@code 0} if it's empty or {@code null}.
     *
     * @param s
     * @return
     */
    public static int len(final CharSequence s) {
        return s == null ? 0 : s.length();
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map}, or {@code 0} if it's empty or {@code null}.
     *
     * @param a
     * @return
     */
    public static int len(final boolean[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map}, or {@code 0} if it's empty or {@code null}.
     *
     * @param a
     * @return
     */
    public static int len(final char[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map}, or {@code 0} if it's empty or {@code null}.
     *
     * @param a
     * @return
     */
    public static int len(final byte[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map}, or {@code 0} if it's empty or {@code null}.
     *
     * @param a
     * @return
     */
    public static int len(final short[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map}, or {@code 0} if it's empty or {@code null}.
     *
     * @param a
     * @return
     */
    public static int len(final int[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map}, or {@code 0} if it's empty or {@code null}.
     *
     * @param a
     * @return
     */
    public static int len(final long[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map}, or {@code 0} if it's empty or {@code null}.
     *
     * @param a
     * @return
     */
    public static int len(final float[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map}, or {@code 0} if it's empty or {@code null}.
     *
     * @param a
     * @return
     */
    public static int len(final double[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map}, or {@code 0} if it's empty or {@code null}.
     *
     * @param a
     * @return
     */
    public static int len(final Object[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map}, or {@code 0} if it's empty or {@code null}.
     *
     * @param c
     * @return
     */
    public static int size(final Collection<?> c) {
        return c == null ? 0 : c.size();
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map}, or {@code 0} if it's empty or {@code null}.
     *
     * @param m
     * @return
     */
    public static int size(final Map<?, ?> m) {
        return m == null ? 0 : m.size();
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map}, or {@code 0} if it's empty or {@code null}.
     *
     * @param c
     * @return
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static int size(final PrimitiveList c) {
        return c == null ? 0 : c.size();
    }

    /**
     *
     *
     * @param str
     * @return
     * @see Strings#nullToEmpty(String)
     */
    @Beta
    public static String nullToEmpty(final String str) {
        return str == null ? Strings.EMPTY_STRING : str;
    }

    /**
     * Returns an empty immutable list if the specified List is <code>null</code>, otherwise itself is returned.
     *
     * @param <T>
     * @param list
     * @return
     * @see #emptyList()
     */
    public static <T> List<T> nullToEmpty(final List<T> list) {
        return list == null ? emptyList() : list;
    }

    /**
     * Returns an empty immutable set if the specified Set is <code>null</code>, otherwise itself is returned.
     *
     * @param <T>
     * @param set
     * @return
     * @see #emptySet()
     */
    public static <T> Set<T> nullToEmpty(final Set<T> set) {
        return set == null ? emptySet() : set;
    }

    /**
     * Returns an empty immutable <code>SortedSet</code> if the specified SortedSet is <code>null</code>, otherwise itself is returned.
     *
     * @param <T>
     * @param set
     * @return
     * @see #emptySortedSet()
     */
    public static <T> SortedSet<T> nullToEmpty(final SortedSet<T> set) {
        return set == null ? emptySortedSet() : set;
    }

    /**
     * Returns an empty immutable <code>NavigableSet</code> if the specified NavigableSet is <code>null</code>, otherwise itself is returned.
     *
     * @param <T>
     * @param set
     * @return
     * @see #emptyNavigableSet()
     */
    public static <T> NavigableSet<T> nullToEmpty(final NavigableSet<T> set) {
        return set == null ? emptyNavigableSet() : set;
    }

    /**
     * Returns an empty immutable map if the specified Map is <code>null</code>, otherwise itself is returned.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     * @see #emptyMap()
     */
    public static <K, V> Map<K, V> nullToEmpty(final Map<K, V> map) {
        return map == null ? emptyMap() : map;
    }

    /**
     * Returns an empty immutable <code>SortedMap</code> if the specified SortedMap is <code>null</code>, otherwise itself is returned.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     * @see #emptySortedMap()
     */
    public static <K, V> SortedMap<K, V> nullToEmpty(final SortedMap<K, V> map) {
        return map == null ? emptySortedMap() : map;
    }

    /**
     * Returns an empty immutable <code>NavigableMap</code> if the specified NavigableMap is <code>null</code>, otherwise itself is returned.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     * @see #emptyNavigableMap()
     */
    public static <K, V> NavigableMap<K, V> nullToEmpty(final NavigableMap<K, V> map) {
        return map == null ? emptyNavigableMap() : map;
    }

    /**
     * Returns an empty immutable <code>Iterator</code> if the specified Iterator is <code>null</code>, otherwise itself is returned.
     *
     * @param <T>
     * @param iter
     * @return
     * @see #emptyIterator()
     */
    public static <T> Iterator<T> nullToEmpty(final Iterator<T> iter) {
        return iter == null ? emptyIterator() : iter;
    }

    /**
     * Returns an empty immutable <code>ListIterator</code> if the specified ListIterator is <code>null</code>, otherwise itself is returned.
     *
     * @param <T>
     * @param iter
     * @return
     * @see #emptyListIterator()
     */
    public static <T> ListIterator<T> nullToEmpty(final ListIterator<T> iter) {
        return iter == null ? emptyListIterator() : iter;
    }

    /**
     * Null to empty.
     *
     * @param a
     * @return
     */
    public static boolean[] nullToEmpty(final boolean[] a) {
        return a == null ? EMPTY_BOOLEAN_ARRAY : a;
    }

    /**
     * Null to empty.
     *
     * @param a
     * @return
     */
    public static char[] nullToEmpty(final char[] a) {
        return a == null ? EMPTY_CHAR_ARRAY : a;
    }

    /**
     * Null to empty.
     *
     * @param a
     * @return
     */
    public static byte[] nullToEmpty(final byte[] a) {
        return a == null ? EMPTY_BYTE_ARRAY : a;
    }

    /**
     * Null to empty.
     *
     * @param a
     * @return
     */
    public static short[] nullToEmpty(final short[] a) {
        return a == null ? EMPTY_SHORT_ARRAY : a;
    }

    /**
     * Null to empty.
     *
     * @param a
     * @return
     */
    public static int[] nullToEmpty(final int[] a) {
        return a == null ? EMPTY_INT_ARRAY : a;
    }

    /**
     * Null to empty.
     *
     * @param a
     * @return
     */
    public static long[] nullToEmpty(final long[] a) {
        return a == null ? EMPTY_LONG_ARRAY : a;
    }

    /**
     * Null to empty.
     *
     * @param a
     * @return
     */
    public static float[] nullToEmpty(final float[] a) {
        return a == null ? EMPTY_FLOAT_ARRAY : a;
    }

    /**
     * Null to empty.
     *
     * @param a
     * @return
     */
    public static double[] nullToEmpty(final double[] a) {
        return a == null ? EMPTY_DOUBLE_ARRAY : a;
    }

    /**
     * Null to empty.
     *
     * @param a
     * @return
     */
    public static BigInteger[] nullToEmpty(final BigInteger[] a) {
        return a == null ? EMPTY_BIG_INTEGER_ARRAY : a;
    }

    /**
     * Null to empty.
     *
     * @param a
     * @return
     */
    public static BigDecimal[] nullToEmpty(final BigDecimal[] a) {
        return a == null ? EMPTY_BIG_DECIMAL_ARRAY : a;
    }

    /**
     * Null to empty.
     *
     * @param a
     * @return
     * @see Strings#nullToEmpty(String)
     * @see Strings#nullToEmpty(String[])
     */
    public static String[] nullToEmpty(final String[] a) {
        //    if (a == null) {
        //        return EMPTY_STRING_ARRAY;
        //    }
        //
        //    for (int i = 0, len = a.length; i < len; i++) {
        //        a[i] = a[i] == null ? Strings.EMPTY_STRING : a[i];
        //    }
        //
        //    return a;

        return a == null ? EMPTY_STRING_ARRAY : a;
    }

    /**
     * Converts the specified String array to an empty {@code String[0]} if it's {@code null} and each {@code null} element String to empty String {@code ""}.
     *
     * @param a
     * @return
     * @see Strings#nullToEmpty(String)
     * @see Strings#nullToEmpty(String[])
     */
    @Beta
    public static String[] nullToEmptyForEach(final String[] a) { // nullToEmptyForAll is better?
        if (a == null) {
            return EMPTY_STRING_ARRAY;
        }

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = a[i] == null ? Strings.EMPTY_STRING : a[i];
        }

        return a;
    }

    //    /**
    //     * Converts the specified String array to an empty {@code String[0]} if it's {@code null} and each {@code null} element String to empty String {@code ""}.
    //     *
    //     * @param a
    //     * @return
    //     * @see Strings#nullToEmpty(String)
    //     * @see Strings#nullToEmpty(String[])
    //     */
    //    static String[] nullToEmptyForAll(final String[] a) { // nullToEmptyForAll is better?
    //        if (a == null) {
    //            return EMPTY_STRING_ARRAY;
    //        }
    //
    //        for (int i = 0, len = a.length; i < len; i++) {
    //            a[i] = a[i] == null ? Strings.EMPTY_STRING : a[i];
    //        }
    //
    //        return a;
    //    }

    /**
     * Null to empty.
     *
     * @param a
     * @return
     */
    public static java.util.Date[] nullToEmpty(final java.util.Date[] a) {
        return a == null ? EMPTY_JU_DATE_ARRAY : a;
    }

    /**
     * Null to empty.
     *
     * @param a
     * @return
     */
    public static java.sql.Date[] nullToEmpty(final java.sql.Date[] a) {
        return a == null ? EMPTY_DATE_ARRAY : a;
    }

    /**
     * Null to empty.
     *
     * @param a
     * @return
     */
    public static java.sql.Time[] nullToEmpty(final java.sql.Time[] a) {
        return a == null ? EMPTY_TIME_ARRAY : a;
    }

    /**
     * Null to empty.
     *
     * @param a
     * @return
     */
    public static java.sql.Timestamp[] nullToEmpty(final java.sql.Timestamp[] a) {
        return a == null ? EMPTY_TIMESTAMP_ARRAY : a;
    }

    /**
     * Null to empty.
     *
     * @param a
     * @return
     */
    public static Calendar[] nullToEmpty(final Calendar[] a) {
        return a == null ? EMPTY_CALENDAR_ARRAY : a;
    }

    /**
     * Null to empty.
     *
     * @param a
     * @return
     */
    public static Object[] nullToEmpty(final Object[] a) {
        return a == null ? EMPTY_OBJECT_ARRAY : a;
    }

    /**
     * Null to empty.
     *
     * @param <T>
     * @param a
     * @param arrayType
     * @return
     */
    public static <T> T[] nullToEmpty(final T[] a, final Class<T[]> arrayType) {
        return a == null ? (T[]) newArray(arrayType.getComponentType(), 0) : a;
    }

    /**
     * Returns an empty immutable Collection if the specified ImmutableCollection is <code>null</code>, otherwise itself is returned.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> ImmutableCollection<T> nullToEmpty(final ImmutableCollection<T> c) {
        return c == null ? ImmutableList.<T> empty() : c;
    }

    /**
     * Returns an empty immutable list if the specified ImmutableList is <code>null</code>, otherwise itself is returned.
     *
     * @param <T>
     * @param list
     * @return
     */
    public static <T> ImmutableList<T> nullToEmpty(final ImmutableList<T> list) {
        return list == null ? ImmutableList.<T> empty() : list;
    }

    /**
     * Returns an empty immutable list if the specified ImmutableSet is <code>null</code>, otherwise itself is returned.
     *
     * @param <T>
     * @param set
     * @return
     */
    public static <T> ImmutableSet<T> nullToEmpty(final ImmutableSet<T> set) {
        return set == null ? ImmutableSet.<T> empty() : set;
    }

    /**
     * Returns an empty immutable list if the specified ImmutableSortedSet is <code>null</code>, otherwise itself is returned.
     *
     * @param <T>
     * @param set
     * @return
     */
    public static <T> ImmutableSortedSet<T> nullToEmpty(final ImmutableSortedSet<T> set) {
        return set == null ? ImmutableSortedSet.<T> empty() : set;
    }

    /**
     * Returns an empty immutable list if the specified ImmutableNavigableSet is <code>null</code>, otherwise itself is returned.
     *
     * @param <T>
     * @param set
     * @return
     */
    public static <T> ImmutableNavigableSet<T> nullToEmpty(final ImmutableNavigableSet<T> set) {
        return set == null ? ImmutableNavigableSet.<T> empty() : set;
    }

    /**
     * Returns an empty immutable map if the specified ImmutableMap is <code>null</code>, otherwise itself is returned.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     */
    public static <K, V> ImmutableMap<K, V> nullToEmpty(final ImmutableMap<K, V> map) {
        return map == null ? ImmutableMap.<K, V> empty() : map;
    }

    /**
     * Returns an empty immutable map if the specified ImmutableSortedMap is <code>null</code>, otherwise itself is returned.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     */
    public static <K, V> ImmutableSortedMap<K, V> nullToEmpty(final ImmutableSortedMap<K, V> map) {
        return map == null ? ImmutableSortedMap.<K, V> empty() : map;
    }

    /**
     * Returns an empty immutable map if the specified ImmutableNavigableMap is <code>null</code>, otherwise itself is returned.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     */
    public static <K, V> ImmutableNavigableMap<K, V> nullToEmpty(final ImmutableNavigableMap<K, V> map) {
        return map == null ? ImmutableNavigableMap.<K, V> empty() : map;
    }

    /**
     * Returns an empty immutable map if the specified ImmutableBiMap is <code>null</code>, otherwise itself is returned.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     */
    public static <K, V> ImmutableBiMap<K, V> nullToEmpty(final ImmutableBiMap<K, V> map) {
        return map == null ? ImmutableBiMap.<K, V> empty() : map;
    }

    //    /**
    //     * Checks if is null or default. {@code null} is default value for all reference types, {@code false} is default value for primitive boolean, {@code 0} is the default value for primitive number type.
    //     *
    //     * @param s
    //     * @return true, if is null or default
    //     * @deprecated internal only
    //     */
    //    @Deprecated
    //    @Internal
    //    @Beta
    //    static boolean isNullOrDefault(final Object value) {
    //        return (value == null) || equals(value, defaultValueOf(value.getClass()));
    //    }

    /**
     * Checks if the specified {@code CharSequence} is null or empty.
     *
     * @param cs
     * @return
     */
    public static boolean isEmpty(final CharSequence cs) {
        return (cs == null) || (cs.length() == 0);
    }

    /**
     * Checks if is null or empty.
     *
     * @param a
     * @return true, if is null or empty
     */
    public static boolean isEmpty(final boolean[] a) {
        return (a == null) || (a.length == 0);
    }

    /**
     * Checks if is null or empty.
     *
     * @param a
     * @return true, if is null or empty
     */
    public static boolean isEmpty(final char[] a) {
        return (a == null) || (a.length == 0);
    }

    /**
     * Checks if is null or empty.
     *
     * @param a
     * @return true, if is null or empty
     */
    public static boolean isEmpty(final byte[] a) {
        return (a == null) || (a.length == 0);
    }

    /**
     * Checks if is null or empty.
     *
     * @param a
     * @return true, if is null or empty
     */
    public static boolean isEmpty(final short[] a) {
        return (a == null) || (a.length == 0);
    }

    /**
     * Checks if is null or empty.
     *
     * @param a
     * @return true, if is null or empty
     */
    public static boolean isEmpty(final int[] a) {
        return (a == null) || (a.length == 0);
    }

    /**
     * Checks if is null or empty.
     *
     * @param a
     * @return true, if is null or empty
     */
    public static boolean isEmpty(final long[] a) {
        return (a == null) || (a.length == 0);
    }

    /**
     * Checks if is null or empty.
     *
     * @param a
     * @return true, if is null or empty
     */
    public static boolean isEmpty(final float[] a) {
        return (a == null) || (a.length == 0);
    }

    /**
     * Checks if is null or empty.
     *
     * @param a
     * @return true, if is null or empty
     */
    public static boolean isEmpty(final double[] a) {
        return (a == null) || (a.length == 0);
    }

    /**
     * Checks if is null or empty.
     *
     * @param a
     * @return true, if is null or empty
     */
    public static boolean isEmpty(final Object[] a) {
        return (a == null) || (a.length == 0);
    }

    /**
     * Checks if is null or empty.
     *
     * @param c
     * @return true, if is null or empty
     */
    public static boolean isEmpty(final Collection<?> c) {
        return (c == null) || (c.isEmpty());
    }

    /**
     * Not null or empty.
     *
     * @param iter
     * @return
     */
    @Beta
    public static boolean isEmpty(final Iterable<?> iter) {
        if (iter == null) {
            return true;
        }

        if (iter instanceof Collection) {
            return isEmpty((Collection<?>) iter);
        } else {
            return isEmpty(iter.iterator());
        }
    }

    /**
     * Not null or empty.
     *
     * @param iter
     * @return
     */
    @Beta
    public static boolean isEmpty(final Iterator<?> iter) {
        return iter == null || (iter.hasNext() == false);
    }

    /**
     * Checks if is null or empty.
     *
     * @param m
     * @return true, if is null or empty
     */
    public static boolean isEmpty(final Map<?, ?> m) {
        return (m == null) || (m.isEmpty());
    }

    /**
     * Checks if is null or empty.
     *
     * @param list
     * @return true, if is null or empty
     */
    @SuppressWarnings("rawtypes")
    public static boolean isEmpty(final PrimitiveList list) {
        return (list == null) || (list.isEmpty());
    }

    /**
     * Checks if is null or empty.
     *
     * @param s
     * @return true, if is null or empty
     */
    public static boolean isEmpty(final Multiset<?> s) {
        return (s == null) || (s.isEmpty());
    }

    /**
     * Checks if is null or empty.
     *
     * @param m
     * @return true, if is null or empty
     */
    public static boolean isEmpty(final Multimap<?, ?, ?> m) {
        return (m == null) || (m.isEmpty());
    }

    /**
     * Checks if is null or empty.
     *
     * @param rs
     * @return true, if is null or empty
     */
    public static boolean isEmpty(final DataSet rs) {
        return (rs == null) || (rs.isEmpty());
    }

    /**
     * Checks if the specified {@code CharSequence} is null or blank.
     *
     *
     * @param cs
     * @return
     */
    public static boolean isBlank(final CharSequence cs) {
        if (isEmpty(cs)) {
            return true;
        }

        for (int i = 0, len = cs.length(); i < len; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param cs
     * @return
     */
    public static boolean notEmpty(final CharSequence cs) {
        return (cs != null) && (cs.length() > 0);
    }

    /**
     * Not null or empty.
     *
     * @param a
     * @return
     */
    public static boolean notEmpty(final boolean[] a) {
        return (a != null) && (a.length > 0);
    }

    /**
     * Not null or empty.
     *
     * @param a
     * @return
     */
    public static boolean notEmpty(final char[] a) {
        return (a != null) && (a.length > 0);
    }

    /**
     * Not null or empty.
     *
     * @param a
     * @return
     */
    public static boolean notEmpty(final byte[] a) {
        return (a != null) && (a.length > 0);
    }

    /**
     * Not null or empty.
     *
     * @param a
     * @return
     */
    public static boolean notEmpty(final short[] a) {
        return (a != null) && (a.length > 0);
    }

    /**
     * Not null or empty.
     *
     * @param a
     * @return
     */
    public static boolean notEmpty(final int[] a) {
        return (a != null) && (a.length > 0);
    }

    /**
     * Not null or empty.
     *
     * @param a
     * @return
     */
    public static boolean notEmpty(final long[] a) {
        return (a != null) && (a.length > 0);
    }

    /**
     * Not null or empty.
     *
     * @param a
     * @return
     */
    public static boolean notEmpty(final float[] a) {
        return (a != null) && (a.length > 0);
    }

    /**
     * Not null or empty.
     *
     * @param a
     * @return
     */
    public static boolean notEmpty(final double[] a) {
        return (a != null) && (a.length > 0);
    }

    /**
     * Not null or empty.
     *
     * @param a
     * @return
     */
    public static boolean notEmpty(final Object[] a) {
        return (a != null) && (a.length > 0);
    }

    /**
     * Not null or empty.
     *
     * @param c
     * @return
     */
    public static boolean notEmpty(final Collection<?> c) {
        return (c != null) && (c.size() > 0);
    }

    /**
     * Not null or empty.
     *
     * @param iter
     * @return
     */
    @Beta
    public static boolean notEmpty(final Iterable<?> iter) {
        if (iter == null) {
            return false;
        }

        if (iter instanceof Collection) {
            return notEmpty((Collection<?>) iter);
        } else {
            return notEmpty(iter.iterator());
        }
    }

    /**
     * Not null or empty.
     *
     * @param iter
     * @return
     */
    @Beta
    public static boolean notEmpty(final Iterator<?> iter) {
        return (iter != null) && (iter.hasNext());
    }

    /**
     * Not null or empty.
     *
     * @param m
     * @return
     */
    public static boolean notEmpty(final Map<?, ?> m) {
        return (m != null) && (m.size() > 0);
    }

    /**
     * Not null or empty.
     *
     * @param list
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static boolean notEmpty(final PrimitiveList list) {
        return (list != null) && (list.size() > 0);
    }

    /**
     * Not null or empty.
     *
     * @param s
     * @return
     */
    public static boolean notEmpty(final Multiset<?> s) {
        return (s != null) && (s.size() > 0);
    }

    /**
     * Not null or empty.
     *
     * @param m
     * @return
     */
    public static boolean notEmpty(final Multimap<?, ?, ?> m) {
        return (m != null) && (m.size() > 0);
    }

    /**
     * Not null or empty.
     *
     * @param rs
     * @return
     */
    public static boolean notEmpty(final DataSet rs) {
        return (rs != null) && (rs.size() > 0);
    }

    /**
     *
     * @param cs
     * @return
     */
    public static boolean notBlank(final CharSequence cs) {
        return !isBlank(cs);
    }

    /**
     * Checks if it's not null or default. {@code null} is default value for all reference types, {@code false} is default value for primitive boolean, {@code 0} is the default value for primitive number type.
     *
     *
     * @param s
     * @return true, if it's not null or default
     * @deprecated DO NOT call the methods defined in this class. it's for internal use only.
     */
    @Deprecated
    static boolean notNullOrDefault(final Object value) {
        return (value != null) && !N.equals(value, N.defaultValueOf(value.getClass()));
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param a
     * @param b
     * @return
     */
    public static <A, B> boolean anyNull(final A a, final B b) {
        return a == null || b == null;
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static <A, B, C> boolean anyNull(final A a, final B b, final C c) {
        return a == null || b == null || c == null;
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> boolean anyNull(final T... a) {
        if (isEmpty(a)) {
            return false;
        }

        for (T e : a) {
            if (e == null) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     *
     * @param c
     * @return
     */
    public static boolean anyNull(final Collection<?> c) {
        if (isEmpty(c)) {
            return false;
        }

        for (Object e : c) {
            if (e == null) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean anyEmpty(final CharSequence a, final CharSequence b) {
        return isEmpty(a) || isEmpty(b);
    }

    /**
     *
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static boolean anyEmpty(final CharSequence a, final CharSequence b, final CharSequence c) {
        return isEmpty(a) || isEmpty(b) || isEmpty(c);
    }

    /**
     * <p>Checks if any of the CharSequences are empty ("") or null.</p>
     *
     * <pre>
     * Strings.anyEmpty((String) null)    = true
     * Strings.anyEmpty((String[]) null)  = false
     * Strings.anyEmpty(null, "foo")      = true
     * Strings.anyEmpty("", "bar")        = true
     * Strings.anyEmpty("bob", "")        = true
     * Strings.anyEmpty("  bob  ", null)  = true
     * Strings.anyEmpty(" ", "bar")       = false
     * Strings.anyEmpty("foo", "bar")     = false
     * Strings.anyEmpty(new String[]{})   = false
     * Strings.anyEmpty(new String[]{""}) = true
     * </pre>
     *
     * @param css the CharSequences to check, may be null or empty
     * @return {@code true} if any of the CharSequences are empty or null
     * @see Strings#isAnyEmpty(CharSequence...)
     * @since 3.2
     */
    public static boolean anyEmpty(final CharSequence... css) {
        if (N.isEmpty(css)) {
            return false;
        }

        for (final CharSequence cs : css) {
            if (isEmpty(cs)) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @param css
     * @return
     * @see Strings#isAnyEmpty(CharSequence...)
     */
    public static boolean anyEmpty(final Collection<? extends CharSequence> css) {
        if (N.isEmpty(css)) {
            return false;
        }

        for (final CharSequence cs : css) {
            if (isEmpty(cs)) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param a
     * @param b
     * @return
     */
    public static <A, B> boolean anyEmpty(final A[] a, final B[] b) {
        return a == null || a.length == 0 || b == null || b.length == 0;
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static <A, B, C> boolean anyEmpty(final A[] a, final B[] b, final C[] c) {
        return a == null || a.length == 0 || b == null || b.length == 0 || c == null || c.length == 0;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean anyEmpty(final Collection<?> a, final Collection<?> b) {
        return a == null || a.size() == 0 || b == null || b.size() == 0;
    }

    /**
     *
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static boolean anyEmpty(final Collection<?> a, final Collection<?> b, final Collection<?> c) {
        return a == null || a.size() == 0 || b == null || b.size() == 0 || c == null || c.size() == 0;
    }

    /**
     *
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean anyBlank(final CharSequence a, final CharSequence b) {
        return isBlank(a) || isBlank(b);
    }

    /**
     *
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static boolean anyBlank(final CharSequence a, final CharSequence b, final CharSequence c) {
        return isBlank(a) || isBlank(b) || isBlank(c);
    }

    /**
     * <p>Checks if any of the CharSequences are empty ("") or null or whitespace only.</p>
     *
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
     *
     * <pre>
     * Strings.anyBlank((String) null)    = true
     * Strings.anyBlank((String[]) null)  = false
     * Strings.anyBlank(null, "foo")      = true
     * Strings.anyBlank(null, null)       = true
     * Strings.anyBlank("", "bar")        = true
     * Strings.anyBlank("bob", "")        = true
     * Strings.anyBlank("  bob  ", null)  = true
     * Strings.anyBlank(" ", "bar")       = true
     * Strings.anyBlank(new String[] {})  = false
     * Strings.anyBlank(new String[]{""}) = true
     * Strings.anyBlank("foo", "bar")     = false
     * </pre>
     *
     * @param css the CharSequences to check, may be null or empty
     * @return {@code true} if any of the CharSequences are empty or null or whitespace only
     * @see Strings#isAnyBlank(CharSequence...)
     * @since 3.2
     */
    public static boolean anyBlank(final CharSequence... css) {
        if (N.isEmpty(css)) {
            return false;
        }

        for (final CharSequence cs : css) {
            if (isBlank(cs)) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @param css
     * @return
     * @see Strings#isAnyBlank(Collection)
     */
    public static boolean anyBlank(final Collection<? extends CharSequence> css) {
        if (N.isEmpty(css)) {
            return false;
        }

        for (final CharSequence cs : css) {
            if (isBlank(cs)) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param a
     * @param b
     * @return
     */
    public static <A, B> boolean allNull(final A a, final B b) {
        return a == null && b == null;
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static <A, B, C> boolean allNull(final A a, final B b, final C c) {
        return a == null && b == null && c == null;
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> boolean allNull(final T... a) {
        if (isEmpty(a)) {
            return true;
        }

        for (T e : a) {
            if (e != null) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param c
     * @return
     */
    public static boolean allNull(final Collection<?> c) {
        if (isEmpty(c)) {
            return true;
        }

        for (Object e : c) {
            if (e != null) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean allEmpty(final CharSequence a, final CharSequence b) {
        return isEmpty(a) && isEmpty(b);
    }

    /**
     *
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static boolean allEmpty(final CharSequence a, final CharSequence b, final CharSequence c) {
        return isEmpty(a) && isEmpty(b) && isEmpty(c);
    }

    /**
     * <p>Checks if all of the CharSequences are empty ("") or null.</p>
     *
     * <pre>
     * Strings.allEmpty(null)             = true
     * Strings.allEmpty(null, "")         = true
     * Strings.allEmpty(new String[] {})  = true
     * Strings.allEmpty(null, "foo")      = false
     * Strings.allEmpty("", "bar")        = false
     * Strings.allEmpty("bob", "")        = false
     * Strings.allEmpty("  bob  ", null)  = false
     * Strings.allEmpty(" ", "bar")       = false
     * Strings.allEmpty("foo", "bar")     = false
     * </pre>
     *
     * @param css the CharSequences to check, may be null or empty
     * @return {@code true} if all of the CharSequences are empty or null
     * @see Strings#isAllEmpty(CharSequence...)
     * @since 3.6
     */
    public static boolean allEmpty(final CharSequence... css) {
        if (N.isEmpty(css)) {
            return true;
        }

        for (final CharSequence cs : css) {
            if (notEmpty(cs)) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param css
     * @return
     * @see Strings#isAllEmpty(Collection)
     */
    public static boolean allEmpty(final Collection<? extends CharSequence> css) {
        if (N.isEmpty(css)) {
            return true;
        }

        for (final CharSequence cs : css) {
            if (notEmpty(cs)) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param a
     * @param b
     * @return
     */
    public static <A, B> boolean allEmpty(final A[] a, final B[] b) {
        return isEmpty(a) && isEmpty(b);
    }

    /**
     * @param <A>
     * @param <B>
     * @param <C>
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static <A, B, C> boolean allEmpty(final A[] a, final B[] b, final C[] c) {
        return isEmpty(a) && isEmpty(b) && isEmpty(c);
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean allEmpty(final Collection<?> a, final Collection<?> b) {
        return isEmpty(a) && isEmpty(b);
    }

    /**
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static boolean allEmpty(final Collection<?> a, final Collection<?> b, final Collection<?> c) {
        return isEmpty(a) && isEmpty(b) && isEmpty(c);
    }

    /**
     *
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean allBlank(final CharSequence a, final CharSequence b) {
        return isBlank(a) && isBlank(b);
    }

    /**
     *
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static boolean allBlank(final CharSequence a, final CharSequence b, final CharSequence c) {
        return isBlank(a) && isBlank(b) && isBlank(c);
    }

    /**
     * <p>Checks if all of the CharSequences are empty (""), null or whitespace only.</p>
     *
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
     *
     * <pre>
     * Strings.allBlank(null)             = true
     * Strings.allBlank(null, "foo")      = false
     * Strings.allBlank(null, null)       = true
     * Strings.allBlank("", "bar")        = false
     * Strings.allBlank("bob", "")        = false
     * Strings.allBlank("  bob  ", null)  = false
     * Strings.allBlank(" ", "bar")       = false
     * Strings.allBlank("foo", "bar")     = false
     * Strings.allBlank(new String[] {})  = true
     * </pre>
     *
     * @param css the CharSequences to check, may be null or empty
     * @return {@code true} if all of the CharSequences are empty or null or whitespace only
     * @see Strings#isAllBlank(CharSequence...)
     * @since 3.6
     */
    public static boolean allBlank(final CharSequence... css) {
        if (N.isEmpty(css)) {
            return true;
        }

        for (final CharSequence cs : css) {
            if (notBlank(cs)) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param css
     * @return
     * @see Strings#isAllBlank(Collection)
     */
    public static boolean allBlank(final Collection<? extends CharSequence> css) {
        if (N.isEmpty(css)) {
            return true;
        }

        for (final CharSequence cs : css) {
            if (notBlank(cs)) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param index
     * @param length
     * @throws IndexOutOfBoundsException the index out of bounds exception
     */
    public static void checkIndex(final int index, final int length) throws IndexOutOfBoundsException {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index " + index + " is out-of-bounds for length " + length);
        }
    }

    /**
     * Check from to index.
     *
     * @param fromIndex
     * @param toIndex
     * @param length
     * @throws IndexOutOfBoundsException the index out of bounds exception
     */
    public static void checkFromToIndex(final int fromIndex, final int toIndex, final int length) throws IndexOutOfBoundsException {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length) {
            throw new IndexOutOfBoundsException("Index range [" + fromIndex + ", " + toIndex + "] is out-of-bounds for length " + length);
        }
    }

    /**
     * Check from index size.
     *
     * @param fromIndex the lower-bound (inclusive) of the sub-interval
     * @param size the size of the sub-range
     * @param length the upper-bound (exclusive) of the range
     * @throws IndexOutOfBoundsException the index out of bounds exception
     */
    public static void checkFromIndexSize(final int fromIndex, final int size, final int length) throws IndexOutOfBoundsException {
        if ((fromIndex < 0 || size < 0 || length < 0) || size > length - fromIndex) {
            throw new IndexOutOfBoundsException("Start Index " + fromIndex + " with size " + size + " is out-of-bounds for length " + length);
        }
    }

    /**
     * Check arg not null.
     *
     * @param <T>
     * @param obj
     * @return
     * @throws IllegalArgumentException if {@code obj} is {@code null}
     */
    public static <T> T checkArgNotNull(final T obj) throws IllegalArgumentException {
        if (obj == null) {
            throw new IllegalArgumentException();
        }

        return obj;
    }

    /**
     * Check arg not null.
     *
     * @param <T>
     * @param obj
     * @param errorMessage
     * @return
     * @throws IllegalArgumentException if {@code obj} is {@code null}
     */
    public static <T> T checkArgNotNull(final T obj, final String errorMessage) throws IllegalArgumentException {
        if (obj == null) {
            if (isArgNameOnly(errorMessage)) {
                throw new IllegalArgumentException("'" + errorMessage + "' can not be null");
            } else {
                throw new IllegalArgumentException(errorMessage);
            }
        }

        return obj;
    }

    private static boolean isArgNameOnly(final String argNameOrErrorMsg) {
        // shortest message: "it is null"
        return !(argNameOrErrorMsg.length() > 9 && argNameOrErrorMsg.indexOf(WD._SPACE) > 0); //NOSONAR
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param <T>
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static <T extends CharSequence> T checkArgNotEmpty(final T arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (Strings.isEmpty(arg)) {
            throwIllegalArgumentExceptionForNllOrEmptyCheck(argNameOrErrorMsg);
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static boolean[] checkArgNotEmpty(final boolean[] arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (isEmpty(arg)) {
            throwIllegalArgumentExceptionForNllOrEmptyCheck(argNameOrErrorMsg);
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static char[] checkArgNotEmpty(final char[] arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (isEmpty(arg)) {
            throwIllegalArgumentExceptionForNllOrEmptyCheck(argNameOrErrorMsg);
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static byte[] checkArgNotEmpty(final byte[] arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (isEmpty(arg)) {
            throwIllegalArgumentExceptionForNllOrEmptyCheck(argNameOrErrorMsg);
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static short[] checkArgNotEmpty(final short[] arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (isEmpty(arg)) {
            throwIllegalArgumentExceptionForNllOrEmptyCheck(argNameOrErrorMsg);
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static int[] checkArgNotEmpty(final int[] arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (isEmpty(arg)) {
            throwIllegalArgumentExceptionForNllOrEmptyCheck(argNameOrErrorMsg);
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static long[] checkArgNotEmpty(final long[] arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (isEmpty(arg)) {
            throwIllegalArgumentExceptionForNllOrEmptyCheck(argNameOrErrorMsg);
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static float[] checkArgNotEmpty(final float[] arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (isEmpty(arg)) {
            throwIllegalArgumentExceptionForNllOrEmptyCheck(argNameOrErrorMsg);
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static double[] checkArgNotEmpty(final double[] arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (isEmpty(arg)) {
            throwIllegalArgumentExceptionForNllOrEmptyCheck(argNameOrErrorMsg);
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param <T>
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static <T> T[] checkArgNotEmpty(final T[] arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (isEmpty(arg)) {
            throwIllegalArgumentExceptionForNllOrEmptyCheck(argNameOrErrorMsg);
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param <T>
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static <T extends Collection<?>> T checkArgNotEmpty(final T arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (isEmpty(arg)) {
            throwIllegalArgumentExceptionForNllOrEmptyCheck(argNameOrErrorMsg);
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param <T>
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    @Beta
    public static <T extends Iterable<?>> T checkArgNotEmpty(final T arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (isEmpty(arg)) {
            throwIllegalArgumentExceptionForNllOrEmptyCheck(argNameOrErrorMsg);
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param <T>
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    @Beta
    public static <T extends Iterator<?>> T checkArgNotEmpty(final T arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (isEmpty(arg)) {
            throwIllegalArgumentExceptionForNllOrEmptyCheck(argNameOrErrorMsg);
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param <T>
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static <T extends Map<?, ?>> T checkArgNotEmpty(final T arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (isEmpty(arg)) {
            throwIllegalArgumentExceptionForNllOrEmptyCheck(argNameOrErrorMsg);
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param <T>
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static <T extends PrimitiveList<?, ?, ?>> T checkArgNotEmpty(final T arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (isEmpty(arg)) {
            throwIllegalArgumentExceptionForNllOrEmptyCheck(argNameOrErrorMsg);
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param <T>
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static <T> Multiset<T> checkArgNotEmpty(final Multiset<T> arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (isEmpty(arg)) {
            throwIllegalArgumentExceptionForNllOrEmptyCheck(argNameOrErrorMsg);
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param <T>
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static <T extends Multimap<?, ?, ?>> T checkArgNotEmpty(final T arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (isEmpty(arg)) {
            throwIllegalArgumentExceptionForNllOrEmptyCheck(argNameOrErrorMsg);
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param <T>
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static <T extends DataSet> T checkArgNotEmpty(final T arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (isEmpty(arg)) {
            throwIllegalArgumentExceptionForNllOrEmptyCheck(argNameOrErrorMsg);
        }

        return arg;
    }

    private static void throwIllegalArgumentExceptionForNllOrEmptyCheck(final String errorMessage) {
        if (isArgNameOnly(errorMessage)) {
            throw new IllegalArgumentException("'" + errorMessage + "' can not be null or empty");
        } else {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * <p>Checks if a CharSequence is NOT {@code null}, empty ("") or whitespace only.</p>
     *
     * @param <T>
     * @param arg
     * @param msg name of parameter or error message
     * @return
     * @throws IllegalArgumentException if the specified parameter is {@code null}, empty ("") or whitespace only.
     * @see Strings#isBlank(CharSequence)
     * @see Strings#isNotBlank(CharSequence)
     */
    // DON'T change 'OrEmptyOrBlank' to 'OrBlank' because of the occurring order in the auto-completed context menu.
    public static <T extends CharSequence> T checkArgNotBlank(final T arg, final String msg) throws IllegalArgumentException {
        if (Strings.isBlank(arg)) {
            if (isArgNameOnly(msg)) {
                throw new IllegalArgumentException("'" + msg + "' can not be null or empty or blank");
            } else {
                throw new IllegalArgumentException(msg);
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is not negative, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is negative.
     */
    public static byte checkArgNotNegative(final byte arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (arg < 0) {
            if (isArgNameOnly(argNameOrErrorMsg)) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be negative: " + arg); //NOSONAR
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is not negative, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is negative.
     */
    public static short checkArgNotNegative(final short arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (arg < 0) {
            if (isArgNameOnly(argNameOrErrorMsg)) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be negative: " + arg); //NOSONAR
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is not negative, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is negative.
     */
    public static int checkArgNotNegative(final int arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (arg < 0) {
            if (isArgNameOnly(argNameOrErrorMsg)) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be negative: " + arg); //NOSONAR
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is not negative, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is negative.
     */
    public static long checkArgNotNegative(final long arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (arg < 0) {
            if (isArgNameOnly(argNameOrErrorMsg)) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be negative: " + arg);
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is not negative, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is negative.
     */
    public static float checkArgNotNegative(final float arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (arg < 0) {
            if (isArgNameOnly(argNameOrErrorMsg)) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be negative: " + arg);
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is not negative, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is negative.
     */
    public static double checkArgNotNegative(final double arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (arg < 0) {
            if (isArgNameOnly(argNameOrErrorMsg)) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be negative: " + arg);
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is positive, and throws {@code IllegalArgumentException} if it is not.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is negative.
     */
    public static byte checkArgPositive(final byte arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (arg <= 0) {
            if (isArgNameOnly(argNameOrErrorMsg)) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be zero or negative: " + arg);
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is positive, and throws {@code IllegalArgumentException} if it is not.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is negative.
     */
    public static short checkArgPositive(final short arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (arg <= 0) {
            if (isArgNameOnly(argNameOrErrorMsg)) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be zero or negative: " + arg);
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is positive, and throws {@code IllegalArgumentException} if it is not.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is negative.
     */
    public static int checkArgPositive(final int arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (arg <= 0) {
            if (isArgNameOnly(argNameOrErrorMsg)) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be zero or negative: " + arg);
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is positive, and throws {@code IllegalArgumentException} if it is not.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is negative.
     */
    public static long checkArgPositive(final long arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (arg <= 0) {
            if (isArgNameOnly(argNameOrErrorMsg)) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be zero or negative: " + arg);
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is positive, and throws {@code IllegalArgumentException} if it is not.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is negative.
     */
    public static float checkArgPositive(final float arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (arg <= 0) {
            if (isArgNameOnly(argNameOrErrorMsg)) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be zero or negative: " + arg);
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is positive, and throws {@code IllegalArgumentException} if it is not.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is negative.
     */
    public static double checkArgPositive(final double arg, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (arg <= 0) {
            if (isArgNameOnly(argNameOrErrorMsg)) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be zero or negative: " + arg);
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Check if the specified {@code Array} contains any {@code null} element.
     *
     * @param a
     * @throws IllegalArgumentException if {@code null} element found in {@code a}
     */
    public static void checkElementNotNull(final Object[] a) throws IllegalArgumentException {
        if (N.isEmpty(a)) {
            return;
        }

        for (Object e : a) {
            if (e == null) {
                throw new IllegalArgumentException("null element is found in collection");
            }
        }
    }

    /**
     * Check if the specified {@code Array} contains any {@code null} element.
     *
     * @param a
     * @param argNameOrErrorMsg
     * @throws IllegalArgumentException if {@code null} element found in {@code a}
     */
    public static void checkElementNotNull(final Object[] a, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (N.isEmpty(a)) {
            return;
        }

        for (Object e : a) {
            if (e == null) {
                if (isArgNameOnly(argNameOrErrorMsg)) {
                    throw new IllegalArgumentException("null element is found in " + argNameOrErrorMsg);
                } else {
                    throw new IllegalArgumentException(argNameOrErrorMsg);
                }
            }
        }
    }

    /**
     * Check if the specified {@code Collection} contains any {@code null} element.
     *
     * @param c
     * @throws IllegalArgumentException if {@code null} element found in {@code c}
     */
    public static void checkElementNotNull(final Collection<?> c) throws IllegalArgumentException {
        if (N.isEmpty(c)) {
            return;
        }

        for (Object e : c) {
            if (e == null) {
                throw new IllegalArgumentException("null element is found in collection");
            }
        }
    }

    /**
     * Check if the specified {@code Collection} contains any {@code null} element.
     *
     * @param c
     * @param argNameOrErrorMsg
     * @throws IllegalArgumentException if {@code null} element found in {@code c}
     */
    public static void checkElementNotNull(final Collection<?> c, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (N.isEmpty(c)) {
            return;
        }

        for (Object e : c) {
            if (e == null) {
                if (isArgNameOnly(argNameOrErrorMsg)) {
                    throw new IllegalArgumentException("null element is found in " + argNameOrErrorMsg);
                } else {
                    throw new IllegalArgumentException(argNameOrErrorMsg);
                }
            }
        }
    }

    /**
     * Check if the specified {@code Map} contains any {@code null} key.
     *
     * @param m
     * @throws IllegalArgumentException if {@code null} key found in {@code m}
     */
    public static void checkKeyNotNull(final Map<?, ?> m) throws IllegalArgumentException {
        if (N.isEmpty(m)) {
            return;
        }

        for (Object e : m.keySet()) {
            if (e == null) {
                throw new IllegalArgumentException("null key is found in Map");
            }
        }
    }

    /**
     * Check if the specified {@code Map} contains any {@code null} key.
     *
     * @param m
     * @param argNameOrErrorMsg
     * @throws IllegalArgumentException if {@code null} key found in {@code m}
     */
    public static void checkKeyNotNull(final Map<?, ?> m, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (N.isEmpty(m)) {
            return;
        }

        for (Object e : m.keySet()) {
            if (e == null) {
                if (isArgNameOnly(argNameOrErrorMsg)) {
                    throw new IllegalArgumentException("null key is found in " + argNameOrErrorMsg);
                } else {
                    throw new IllegalArgumentException(argNameOrErrorMsg);
                }
            }
        }
    }

    /**
     * Check if the specified {@code Map} contains any {@code null} value.
     *
     * @param m
     * @throws IllegalArgumentException if {@code null} value found in {@code m}
     */
    public static void checkValueNotNull(final Map<?, ?> m) throws IllegalArgumentException {
        if (N.isEmpty(m)) {
            return;
        }

        for (Object e : m.values()) {
            if (e == null) {
                throw new IllegalArgumentException("null value is found in Map");
            }
        }
    }

    /**
     * Check if the specified {@code Map} contains any {@code null} value.
     *
     * @param m
     * @param argNameOrErrorMsg
     * @throws IllegalArgumentException if {@code null} value found in {@code m}
     */
    public static void checkValueNotNull(final Map<?, ?> m, final String argNameOrErrorMsg) throws IllegalArgumentException {
        if (N.isEmpty(m)) {
            return;
        }

        for (Object e : m.values()) {
            if (e == null) {
                if (isArgNameOnly(argNameOrErrorMsg)) {
                    throw new IllegalArgumentException("null value is found in " + argNameOrErrorMsg);
                } else {
                    throw new IllegalArgumentException(argNameOrErrorMsg);
                }
            }
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression a boolean expression
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkArgument(boolean expression) throws IllegalArgumentException {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression a boolean expression
     * @param errorMessage the exception message to use if the check fails; will be converted to a
     *     string using {@link String#valueOf(Object)}
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkArgument(boolean expression, String errorMessage) throws IllegalArgumentException {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p
     * @throws IllegalArgumentException
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, boolean p) throws IllegalArgumentException {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     * @throws IllegalArgumentException
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, boolean p1, boolean p2) throws IllegalArgumentException {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     * @param p3
     * @throws IllegalArgumentException
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, boolean p1, boolean p2, boolean p3) throws IllegalArgumentException {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2, p3));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p
     * @throws IllegalArgumentException
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, char p) throws IllegalArgumentException {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p
     * @throws IllegalArgumentException
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, byte p) throws IllegalArgumentException {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p
     * @throws IllegalArgumentException
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, short p) throws IllegalArgumentException {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p
     * @throws IllegalArgumentException
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, int p) throws IllegalArgumentException {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     * @throws IllegalArgumentException
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, int p1, int p2) throws IllegalArgumentException {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     * @param p3
     * @throws IllegalArgumentException
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, int p1, int p2, int p3) throws IllegalArgumentException {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2, p3));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p
     * @throws IllegalArgumentException
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, long p) throws IllegalArgumentException {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     * @throws IllegalArgumentException
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, long p1, long p2) throws IllegalArgumentException {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     * @param p3
     * @throws IllegalArgumentException
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, long p1, long p2, long p3) throws IllegalArgumentException {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2, p3));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p
     * @throws IllegalArgumentException
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, float p) throws IllegalArgumentException {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     * @throws IllegalArgumentException
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, float p1, float p2) throws IllegalArgumentException {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     * @param p3
     * @throws IllegalArgumentException
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, float p1, float p2, float p3) throws IllegalArgumentException {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2, p3));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p
     * @throws IllegalArgumentException
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, double p) throws IllegalArgumentException {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     * @throws IllegalArgumentException
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, double p1, double p2) throws IllegalArgumentException {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     * @param p3
     * @throws IllegalArgumentException
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, double p1, double p2, double p3) throws IllegalArgumentException {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2, p3));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p
     * @throws IllegalArgumentException
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, Object p) throws IllegalArgumentException {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     * @throws IllegalArgumentException
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, Object p1, Object p2) throws IllegalArgumentException {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     * @param p3
     * @throws IllegalArgumentException
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, Object p1, Object p2, Object p3) throws IllegalArgumentException {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2, p3));
        }
    }

    /**
     *
     *
     * @param b
     * @param errorMessageSupplier
     * @throws IllegalArgumentException
     */
    public static void checkArgument(boolean b, Supplier<String> errorMessageSupplier) throws IllegalArgumentException {
        if (!b) {
            throw new IllegalArgumentException(errorMessageSupplier.get());
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * @param expression a boolean expression
     * @throws IllegalStateException if {@code expression} is false
     */
    public static void checkState(boolean expression) {
        if (!expression) {
            throw new IllegalStateException();
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * @param expression a boolean expression
     * @param errorMessage the exception message to use if the check fails; will be converted to a
     *     string using {@link String#valueOf(Object)}
     * @throws IllegalStateException if {@code expression} is false
     */
    public static void checkState(boolean expression, String errorMessage) {
        if (!expression) {
            throw new IllegalStateException(errorMessage);
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * <p>See {@link #checkState(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p
     */
    public static void checkState(boolean b, String errorMessageTemplate, int p) {
        if (!b) {
            throw new IllegalStateException(format(errorMessageTemplate, p));
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * <p>See {@link #checkState(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     */
    public static void checkState(boolean b, String errorMessageTemplate, int p1, int p2) {
        if (!b) {
            throw new IllegalStateException(format(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * <p>See {@link #checkState(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     * @param p3
     */
    public static void checkState(boolean b, String errorMessageTemplate, int p1, int p2, int p3) {
        if (!b) {
            throw new IllegalStateException(format(errorMessageTemplate, p1, p2, p3));
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * <p>See {@link #checkState(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p
     */
    public static void checkState(boolean b, String errorMessageTemplate, long p) {
        if (!b) {
            throw new IllegalStateException(format(errorMessageTemplate, p));
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * <p>See {@link #checkState(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     */
    public static void checkState(boolean b, String errorMessageTemplate, long p1, long p2) {
        if (!b) {
            throw new IllegalStateException(format(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * <p>See {@link #checkState(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     * @param p3
     */
    public static void checkState(boolean b, String errorMessageTemplate, long p1, long p2, long p3) {
        if (!b) {
            throw new IllegalStateException(format(errorMessageTemplate, p1, p2, p3));
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * <p>See {@link #checkState(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p
     */
    public static void checkState(boolean b, String errorMessageTemplate, float p) {
        if (!b) {
            throw new IllegalStateException(format(errorMessageTemplate, p));
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * <p>See {@link #checkState(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     */
    public static void checkState(boolean b, String errorMessageTemplate, float p1, float p2) {
        if (!b) {
            throw new IllegalStateException(format(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * <p>See {@link #checkState(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     * @param p3
     */
    public static void checkState(boolean b, String errorMessageTemplate, float p1, float p2, float p3) {
        if (!b) {
            throw new IllegalStateException(format(errorMessageTemplate, p1, p2, p3));
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * <p>See {@link #checkState(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p
     */
    public static void checkState(boolean b, String errorMessageTemplate, double p) {
        if (!b) {
            throw new IllegalStateException(format(errorMessageTemplate, p));
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * <p>See {@link #checkState(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     */
    public static void checkState(boolean b, String errorMessageTemplate, double p1, double p2) {
        if (!b) {
            throw new IllegalStateException(format(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * <p>See {@link #checkState(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     * @param p3
     */
    public static void checkState(boolean b, String errorMessageTemplate, double p1, double p2, double p3) {
        if (!b) {
            throw new IllegalStateException(format(errorMessageTemplate, p1, p2, p3));
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * <p>See {@link #checkState(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p
     */
    public static void checkState(boolean b, String errorMessageTemplate, Object p) {
        if (!b) {
            throw new IllegalStateException(format(errorMessageTemplate, p));
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * <p>See {@link #checkState(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     */
    public static void checkState(boolean b, String errorMessageTemplate, Object p1, Object p2) {
        if (!b) {
            throw new IllegalStateException(format(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling instance, but not
     * involving any parameters to the calling method.
     *
     * <p>See {@link #checkState(boolean, String, Object...)} for details.
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     * @param p3
     */
    public static void checkState(boolean b, String errorMessageTemplate, Object p1, Object p2, Object p3) {
        if (!b) {
            throw new IllegalStateException(format(errorMessageTemplate, p1, p2, p3));
        }
    }

    /**
     *
     *
     * @param b
     * @param errorMessageSupplier
     */
    public static void checkState(boolean b, Supplier<String> errorMessageSupplier) {
        if (!b) {
            throw new IllegalStateException(errorMessageSupplier.get());
        }
    }

    /**
     *
     * @param template
     * @param arg
     * @return
     */
    static String format(String template, Object arg) {
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '%s' placeholders
        final StringBuilder sb = Objectory.createStringBuilder(template.length() + 16);

        String placeholder = "{}";
        int placeholderStart = template.indexOf(placeholder);

        if (placeholderStart < 0) {
            placeholder = "%s";
            placeholderStart = template.indexOf(placeholder);
        }

        if (placeholderStart >= 0) {
            sb.append(template, 0, placeholderStart);
            sb.append(toString(arg));
            sb.append(template, placeholderStart + 2, template.length());
        } else {
            sb.append(" [");
            sb.append(toString(arg));
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @return
     */
    static String format(String template, Object arg1, Object arg2) {
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '%s' placeholders
        final StringBuilder sb = Objectory.createStringBuilder(template.length() + 32);

        String placeholder = "{}";
        int placeholderStart = template.indexOf(placeholder);

        if (placeholderStart < 0) {
            placeholder = "%s";
            placeholderStart = template.indexOf(placeholder);
        }

        int templateStart = 0;
        int cnt = 0;

        if (placeholderStart >= 0) {
            cnt++;
            sb.append(template, templateStart, placeholderStart);
            sb.append(toString(arg1));
            templateStart = placeholderStart + 2;
            placeholderStart = template.indexOf(placeholder, templateStart);

            if (placeholderStart >= 0) {
                cnt++;
                sb.append(template, templateStart, placeholderStart);
                sb.append(toString(arg2));
                templateStart = placeholderStart + 2;
            }

            sb.append(template, templateStart, template.length());
        }

        if (cnt == 0) {
            sb.append(" [");
            sb.append(toString(arg1));
            sb.append(", ");
            sb.append(toString(arg2));
            sb.append(']');
        } else if (cnt == 1) {
            sb.append(" [");
            sb.append(toString(arg2));
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }

    /**
     *
     * @param template
     * @param arg1
     * @param arg2
     * @param arg3
     * @return
     */
    static String format(String template, Object arg1, Object arg2, Object arg3) {
        template = String.valueOf(template); // null -> "null"

        // start substituting the arguments into the '%s' placeholders
        final StringBuilder sb = Objectory.createStringBuilder(template.length() + 48);

        String placeholder = "{}";
        int placeholderStart = template.indexOf(placeholder);

        if (placeholderStart < 0) {
            placeholder = "%s";
            placeholderStart = template.indexOf(placeholder);
        }

        int templateStart = 0;
        int cnt = 0;

        if (placeholderStart >= 0) {
            cnt++;
            sb.append(template, templateStart, placeholderStart);
            sb.append(toString(arg1));
            templateStart = placeholderStart + 2;
            placeholderStart = template.indexOf(placeholder, templateStart);

            if (placeholderStart >= 0) {
                cnt++;
                sb.append(template, templateStart, placeholderStart);
                sb.append(toString(arg2));
                templateStart = placeholderStart + 2;
                placeholderStart = template.indexOf(placeholder, templateStart);

                if (placeholderStart >= 0) {
                    cnt++;
                    sb.append(template, templateStart, placeholderStart);
                    sb.append(toString(arg3));
                    templateStart = placeholderStart + 2;
                }
            }

            sb.append(template, templateStart, template.length());
        }

        if (cnt == 0) {
            sb.append(" [");
            sb.append(toString(arg1));
            sb.append(", ");
            sb.append(toString(arg2));
            sb.append(", ");
            sb.append(toString(arg3));
            sb.append(']');
        } else if (cnt == 1) {
            sb.append(" [");
            sb.append(toString(arg2));
            sb.append(", ");
            sb.append(toString(arg3));
            sb.append(']');
        } else if (cnt == 2) {
            sb.append(" [");
            sb.append(toString(arg3));
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }

    /**
     * Substitutes each {@code %s} in {@code template} with an argument. These are matched by
     * position: the first {@code %s} gets {@code args[0]}, etc. If there are more arguments than
     * placeholders, the unmatched arguments will be appended to the end of the formatted message in
     * square braces.
     *
     * @param template a non-null string containing 0 or more {@code %s} placeholders.
     * @param args the arguments to be substituted into the message template. Arguments are converted
     *     to strings using {@link String#valueOf(Object)}. Arguments can be null.
     * @return
     */
    // Note that this is somewhat-improperly used from Verify.java as well.
    static String format(String template, Object... args) {
        template = String.valueOf(template); // null -> "null"

        if (isEmpty(args)) {
            return template;
        }

        // start substituting the arguments into the '%s' placeholders
        final StringBuilder sb = Objectory.createStringBuilder(template.length() + 16 * args.length);
        int templateStart = 0;
        int i = 0;

        String placeholder = "{}";
        int placeholderStart = template.indexOf(placeholder);

        if (placeholderStart < 0) {
            placeholder = "%s";
            placeholderStart = template.indexOf(placeholder);
        }

        while (placeholderStart >= 0 && i < args.length) {
            sb.append(template, templateStart, placeholderStart);
            sb.append(toString(args[i++]));
            templateStart = placeholderStart + 2;
            placeholderStart = template.indexOf(placeholder, templateStart);
        }

        sb.append(template, templateStart, template.length());

        // if we run out of placeholders, append the extra args in square braces
        if (i < args.length) {
            sb.append(" [");
            sb.append(toString(args[i++]));
            while (i < args.length) {
                sb.append(", ");
                sb.append(toString(args[i++]));
            }
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static int compare(final boolean a, final boolean b) {
        return (a == b) ? 0 : (a ? 1 : -1);
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static int compare(final byte a, final byte b) {
        return (a < b) ? -1 : ((a == b) ? 0 : 1);
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static int compare(final short a, final short b) {
        return (a < b) ? -1 : ((a == b) ? 0 : 1);
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static int compare(final int a, final int b) {
        return (a < b) ? -1 : ((a == b) ? 0 : 1);
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static int compare(final long a, final long b) {
        return (a < b) ? -1 : ((a == b) ? 0 : 1);
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static int compare(final float a, final float b) {
        return Float.compare(a, b);
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static int compare(final double a, final double b) {
        return Double.compare(a, b);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    public static <T extends Comparable<? super T>> int compare(final T a, final T b) {
        return a == null ? (b == null ? 0 : -1) : (b == null ? 1 : a.compareTo(b));
    }

    /**
     * Returns 0 if the arguments are identical and {@code c.compare(a, b)}
     * otherwise. Consequently, if both arguments are {@code null} 0 is
     * returned.
     *
     * <p>
     * Note that if one of the arguments is {@code null}, a
     * {@code NullPointerException} may or may not be thrown depending on what
     * ordering policy, if any, the {@link Comparator Comparator} chooses to
     * have for {@code null} values.
     *
     * @param <T>
     *            the type of the objects being compared
     * @param a
     *            an object
     * @param b
     *            an object to be compared with {@code a}
     * @param cmp
     *            the {@code Comparator} to compare the first two arguments
     * @return 0 if the arguments are identical and {@code c.compare(a, b)}
     *         otherwise.
     * @see Comparable
     * @see Comparator
     */
    public static <T> int compare(final T a, final T b, final Comparator<? super T> cmp) {
        if (cmp == null) {
            return NATURAL_COMPARATOR.compare(a, b);
        }

        return cmp.compare(a, b);
    }

    /**
     * Continue to compare the pairs of values <code>(a1, b1), (a2, b2)</code> until they're not equal.
     * <code>0</code> is returned if all of the pairs of values are equal.
     *
     * @param <T1>
     * @param <T2>
     * @param a1
     * @param b1
     * @param a2
     * @param b2
     * @return
     */
    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>> int compare(T1 a1, T1 b1, T2 a2, T2 b2) {
        int ret = compare(a1, b1);

        return ret == 0 ? compare(a2, b2) : ret;
    }

    /**
     * Continue to compare the pairs of values <code>(a1, b1), (a2, b2), (a3, b3)</code> until they're not equal.
     * <code>0</code> is returned if all of the pairs of values are equal.
     *
     * @param <T1>
     * @param <T2>
     * @param <T3>
     * @param a1
     * @param b1
     * @param a2
     * @param b2
     * @param a3
     * @param b3
     * @return
     */
    @SuppressWarnings("java:S1871")
    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> int compare(T1 a1, T1 b1, T2 a2, T2 b2, T3 a3, T3 b3) {
        int ret = 0;

        if ((ret = compare(a1, b1)) != 0) {
            return ret;
        } else if ((ret = compare(a2, b2)) != 0) {
            return ret;
        }

        return compare(a3, b3);
    }

    /**
     * Continue to compare the pairs of values <code>(a1, b1), (a2, b2), (a3, b3), (a4, b4)</code> until they're not equal.
     * <code>0</code> is returned if all of the pairs of values are equal.
     *
     * @param <T1>
     * @param <T2>
     * @param <T3>
     * @param <T4>
     * @param a1
     * @param b1
     * @param a2
     * @param b2
     * @param a3
     * @param b3
     * @param a4
     * @param b4
     * @return
     * @see Builder#compare(Comparable, Comparable)
     * @deprecated please use {@code Builder.ComparisonBuilder}
     */
    @Deprecated
    @SuppressWarnings("java:S1871")
    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> int compare(T1 a1, T1 b1, T2 a2,
            T2 b2, T3 a3, T3 b3, T4 a4, T4 b4) {
        int ret = 0;

        if ((ret = compare(a1, b1)) != 0) {
            return ret;
        } else if ((ret = compare(a2, b2)) != 0) {
            return ret;
        } else if ((ret = compare(a3, b3)) != 0) {
            return ret;
        }

        return compare(a4, b4);
    }

    /**
     * Continue to compare the pairs of values <code>(a1, b1), (a2, b2), (a3, b3), (a4, b4), (a5, b5)</code> until they're not equal.
     * <code>0</code> is returned if all of the pairs of values are equal.
     *
     * @param <T1>
     * @param <T2>
     * @param <T3>
     * @param <T4>
     * @param <T5>
     * @param a1
     * @param b1
     * @param a2
     * @param b2
     * @param a3
     * @param b3
     * @param a4
     * @param b4
     * @param a5
     * @param b5
     * @return
     * @see Builder#compare(Comparable, Comparable)
     * @deprecated please use {@code Builder.ComparisonBuilder}
     */
    @Deprecated
    @SuppressWarnings("java:S1871")
    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> int compare(
            T1 a1, T1 b1, T2 a2, T2 b2, T3 a3, T3 b3, T4 a4, T4 b4, T5 a5, T5 b5) {
        int ret = 0;

        if ((ret = compare(a1, b1)) != 0) {
            return ret;
        } else if ((ret = compare(a2, b2)) != 0) {
            return ret;
        } else if ((ret = compare(a3, b3)) != 0) {
            return ret;
        } else if ((ret = compare(a4, b4)) != 0) {
            return ret;
        }

        return compare(a5, b5);
    }

    /**
     * Continue to compare the pairs of values <code>(a1, b1), (a2, b2), (a3, b3), (a4, b4), (a5, b5), (a6, b6)</code> until they're not equal.
     * <code>0</code> is returned if all of the pairs of values are equal.
     *
     * @param <T1>
     * @param <T2>
     * @param <T3>
     * @param <T4>
     * @param <T5>
     * @param <T6>
     * @param a1
     * @param b1
     * @param a2
     * @param b2
     * @param a3
     * @param b3
     * @param a4
     * @param b4
     * @param a5
     * @param b5
     * @param a6
     * @param b6
     * @return
     * @see Builder#compare(Comparable, Comparable)
     * @deprecated please use {@code Builder.ComparisonBuilder}
     */
    @Deprecated
    @SuppressWarnings("java:S1871")
    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>, T6 extends Comparable<T6>> int compare(
            T1 a1, T1 b1, T2 a2, T2 b2, T3 a3, T3 b3, T4 a4, T4 b4, T5 a5, T5 b5, T6 a6, T6 b6) {
        int ret = 0;

        if ((ret = compare(a1, b1)) != 0) {
            return ret;
        } else if ((ret = compare(a2, b2)) != 0) {
            return ret;
        } else if ((ret = compare(a3, b3)) != 0) {
            return ret;
        } else if ((ret = compare(a4, b4)) != 0) {
            return ret;
        } else if ((ret = compare(a5, b5)) != 0) {
            return ret;
        }

        return compare(a6, b6);
    }

    /**
     * Continue to compare the pairs of values <code>(a1, b1), (a2, b2), (a3, b3), (a4, b4), (a5, b5), (a6, b6), (a7, b7)</code> until they're not equal.
     * <code>0</code> is returned if all of the pairs of values are equal.
     *
     * @param <T1>
     * @param <T2>
     * @param <T3>
     * @param <T4>
     * @param <T5>
     * @param <T6>
     * @param <T7>
     * @param a1
     * @param b1
     * @param a2
     * @param b2
     * @param a3
     * @param b3
     * @param a4
     * @param b4
     * @param a5
     * @param b5
     * @param a6
     * @param b6
     * @param a7
     * @param b7
     * @return
     * @see Builder#compare(Comparable, Comparable)
     * @deprecated please use {@code Builder.ComparisonBuilder}
     */
    @Deprecated
    @SuppressWarnings("java:S1871")
    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>, T6 extends Comparable<T6>, T7 extends Comparable<T7>> int compare(
            T1 a1, T1 b1, T2 a2, T2 b2, T3 a3, T3 b3, T4 a4, T4 b4, T5 a5, T5 b5, T6 a6, T6 b6, T7 a7, T7 b7) {
        int ret = 0;

        if ((ret = compare(a1, b1)) != 0) {
            return ret;
        } else if ((ret = compare(a2, b2)) != 0) {
            return ret;
        } else if ((ret = compare(a3, b3)) != 0) {
            return ret;
        } else if ((ret = compare(a4, b4)) != 0) {
            return ret;
        } else if ((ret = compare(a5, b5)) != 0) {
            return ret;
        } else if ((ret = compare(a6, b6)) != 0) {
            return ret;
        }

        return compare(a7, b7);
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static int compare(final boolean[] a, final boolean[] b) {
        if (isEmpty(a)) {
            return isEmpty(b) ? 0 : -1;
        } else if (isEmpty(b)) {
            return 1;
        }

        for (int i = 0, minLen = N.min(a.length, b.length); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] ? 1 : -1;
            }
        }

        return Integer.compare(a.length, b.length);
    }

    /**
     *
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static int compare(final boolean[] a, final int fromIndexA, final boolean[] b, final int fromIndexB, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNegative(len, "len");
        checkFromIndexSize(fromIndexA, len, len(a));
        checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return 0;
        }

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if (a[i] != b[j]) {
                return a[i] ? 1 : -1;
            }
        }

        return 0;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static int compare(final char[] a, final char[] b) {
        if (isEmpty(a)) {
            return isEmpty(b) ? 0 : -1;
        } else if (isEmpty(b)) {
            return 1;
        }

        for (int i = 0, minLen = N.min(a.length, b.length); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return Integer.compare(a.length, b.length);
    }

    /**
     *
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static int compare(final char[] a, final int fromIndexA, final char[] b, final int fromIndexB, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNegative(len, "len");
        checkFromIndexSize(fromIndexA, len, len(a));
        checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return 0;
        }

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if (a[i] != b[j]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return 0;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static int compare(final byte[] a, final byte[] b) {
        if (isEmpty(a)) {
            return isEmpty(b) ? 0 : -1;
        } else if (isEmpty(b)) {
            return 1;
        }

        for (int i = 0, minLen = N.min(a.length, b.length); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return Integer.compare(a.length, b.length);
    }

    /**
     *
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static int compare(final byte[] a, final int fromIndexA, final byte[] b, final int fromIndexB, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNegative(len, "len");
        checkFromIndexSize(fromIndexA, len, len(a));
        checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return 0;
        }

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if (a[i] != b[j]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return 0;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static int compare(final short[] a, final short[] b) {
        if (isEmpty(a)) {
            return isEmpty(b) ? 0 : -1;
        } else if (isEmpty(b)) {
            return 1;
        }

        for (int i = 0, minLen = N.min(a.length, b.length); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return Integer.compare(a.length, b.length);
    }

    /**
     *
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static int compare(final short[] a, final int fromIndexA, final short[] b, final int fromIndexB, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNegative(len, "len");
        checkFromIndexSize(fromIndexA, len, len(a));
        checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return 0;
        }

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if (a[i] != b[j]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return 0;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static int compare(final int[] a, final int[] b) {
        if (isEmpty(a)) {
            return isEmpty(b) ? 0 : -1;
        } else if (isEmpty(b)) {
            return 1;
        }

        for (int i = 0, minLen = N.min(a.length, b.length); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return Integer.compare(a.length, b.length);
    }

    /**
     *
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static int compare(final int[] a, final int fromIndexA, final int[] b, final int fromIndexB, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNegative(len, "len");
        checkFromIndexSize(fromIndexA, len, len(a));
        checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return 0;
        }

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if (a[i] != b[j]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return 0;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static int compare(final long[] a, final long[] b) {
        if (isEmpty(a)) {
            return isEmpty(b) ? 0 : -1;
        } else if (isEmpty(b)) {
            return 1;
        }

        for (int i = 0, minLen = N.min(a.length, b.length); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return Integer.compare(a.length, b.length);
    }

    /**
     *
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static int compare(final long[] a, final int fromIndexA, final long[] b, final int fromIndexB, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNegative(len, "len");
        checkFromIndexSize(fromIndexA, len, len(a));
        checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return 0;
        }

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if (a[i] != b[j]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return 0;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static int compare(final float[] a, final float[] b) {
        if (isEmpty(a)) {
            return isEmpty(b) ? 0 : -1;
        } else if (isEmpty(b)) {
            return 1;
        }

        int ret = 0;

        for (int i = 0, minLen = N.min(a.length, b.length); i < minLen; i++) {
            if ((ret = Float.compare(a[i], b[i])) != 0) {
                return ret;
            }
        }

        return Integer.compare(a.length, b.length);
    }

    /**
     *
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static int compare(final float[] a, final int fromIndexA, final float[] b, final int fromIndexB, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNegative(len, "len");
        checkFromIndexSize(fromIndexA, len, len(a));
        checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return 0;
        }

        int ret = 0;

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if ((ret = Float.compare(a[i], b[j])) != 0) {
                return ret;
            }
        }

        return 0;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static int compare(final double[] a, final double[] b) {
        if (isEmpty(a)) {
            return isEmpty(b) ? 0 : -1;
        } else if (isEmpty(b)) {
            return 1;
        }

        int ret = 0;

        for (int i = 0, minLen = N.min(a.length, b.length); i < minLen; i++) {
            if ((ret = Double.compare(a[i], b[i])) != 0) {
                return ret;
            }
        }

        return Integer.compare(a.length, b.length);
    }

    /**
     *
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static int compare(final double[] a, final int fromIndexA, final double[] b, final int fromIndexB, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNegative(len, "len");
        checkFromIndexSize(fromIndexA, len, len(a));
        checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return 0;
        }

        int ret = 0;

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if ((ret = Double.compare(a[i], b[j])) != 0) {
                return ret;
            }
        }

        return 0;
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    public static <T extends Comparable<? super T>> int compare(final T[] a, final T[] b) {
        final Comparator<T> cmp = NATURAL_COMPARATOR;

        return compare(a, b, cmp);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     */
    public static <T extends Comparable<? super T>> int compare(final T[] a, final int fromIndexA, final T[] b, final int fromIndexB, final int len) {
        final Comparator<T> cmp = NATURAL_COMPARATOR;

        return compare(a, fromIndexA, b, fromIndexB, len, cmp);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @param cmp
     * @return
     */
    public static <T> int compare(final T[] a, final T[] b, Comparator<? super T> cmp) {
        if (isEmpty(a)) {
            return isEmpty(b) ? 0 : -1;
        } else if (isEmpty(b)) {
            return 1;
        }

        cmp = checkComparator(cmp);

        int ret = 0;

        for (int i = 0, minLen = N.min(a.length, b.length); i < minLen; i++) {
            if ((ret = cmp.compare(a[i], b[i])) != 0) {
                return ret;
            }
        }

        return Integer.compare(a.length, b.length);
    }

    /**
     *
     *
     * @param <T>
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @param cmp
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static <T> int compare(final T[] a, final int fromIndexA, final T[] b, final int fromIndexB, final int len, Comparator<? super T> cmp)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNegative(len, "len");
        checkFromIndexSize(fromIndexA, len, len(a));
        checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return 0;
        }

        cmp = checkComparator(cmp);
        int ret = 0;

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if ((ret = cmp.compare(a[i], b[j])) != 0) {
                return ret;
            }
        }

        return 0;
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     */
    public static <T> int compare(final Collection<T> a, int fromIndexA, final Collection<T> b, int fromIndexB, final int len) {
        final Comparator<T> cmp = NATURAL_COMPARATOR;

        return compare(a, fromIndexA, b, fromIndexB, len, cmp);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    public static <T extends Comparable<? super T>> int compare(final Iterable<T> a, final Iterable<T> b) {
        final Comparator<T> cmp = NATURAL_COMPARATOR;

        return compare(a, b, cmp);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    public static <T extends Comparable<? super T>> int compare(final Iterator<T> a, final Iterator<T> b) {
        final Comparator<T> cmp = NATURAL_COMPARATOR;

        return compare(a, b, cmp);
    }

    /**
     *
     *
     * @param <T>
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @param cmp
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static <T> int compare(final Collection<T> a, int fromIndexA, final Collection<T> b, int fromIndexB, final int len, Comparator<? super T> cmp)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNegative(len, "len");
        checkFromIndexSize(fromIndexA, len, size(a));
        checkFromIndexSize(fromIndexB, len, size(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return 0;
        }

        cmp = checkComparator(cmp);
        final Iterator<T> iterA = a.iterator();
        final Iterator<T> iterB = b.iterator();

        while (fromIndexA-- > 0) {
            iterA.next();
        }

        while (fromIndexB-- > 0) {
            iterB.next();
        }

        int ret = 0;

        for (int i = 0; i < len; i++) {
            if ((ret = cmp.compare(iterA.next(), iterB.next())) != 0) {
                return ret;
            }
        }

        return 0;
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @param cmp
     * @return
     */
    public static <T> int compare(final Iterable<T> a, final Iterable<T> b, Comparator<? super T> cmp) {
        if (isEmpty(a)) {
            return isEmpty(b) ? 0 : -1;
        } else if (isEmpty(b)) {
            return 1;
        }

        cmp = checkComparator(cmp);

        return compare(a.iterator(), b.iterator(), cmp);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @param cmp
     * @return
     */
    public static <T> int compare(final Iterator<T> a, final Iterator<T> b, Comparator<? super T> cmp) {
        cmp = checkComparator(cmp);

        final Iterator<T> iterA = a == null ? ObjIterator.empty() : a;
        final Iterator<T> iterB = b == null ? ObjIterator.empty() : b;
        int ret = 0;

        while (iterA.hasNext() && iterB.hasNext()) {
            if ((ret = cmp.compare(iterA.next(), iterB.next())) != 0) {
                return ret;
            }
        }

        return iterA.hasNext() ? 1 : (iterB.hasNext() ? -1 : 0);
    }

    /**
     * Compare ignore case.
     *
     * @param a
     * @param b
     * @return
     */
    public static int compareIgnoreCase(final String a, final String b) {
        return a == null ? (b == null ? 0 : -1) : (b == null ? 1 : a.compareToIgnoreCase(b));
    }

    /**
     * Compare ignore case.
     *
     * @param a
     * @param b
     * @return
     */
    public static int compareIgnoreCase(final String[] a, final String[] b) {
        return compare(a, b, Comparators.comparingIgnoreCase());
    }

    /**
     * Returns {@code true} is {@code a < b}, otherwise {@code false} is returned.
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    public static <T extends Comparable<? super T>> boolean lessThan(final T a, final T b) {
        return compare(a, b) < 0;
    }

    /**
     * Returns default Comparator {@code NATURAL_COMPARATOR} if the specified {@code cmp} is null. Otherwise returns {@code cmp}.
     *
     * @param <T>
     * @param cmp
     * @return
     */
    static <T> Comparator<T> checkComparator(Comparator<T> cmp) {
        return cmp == null ? NATURAL_COMPARATOR : cmp;
    }

    /**
     * Returns {@code true} is {@code a < b}, otherwise {@code false} is returned.
     *
     * @param <T>
     * @param a
     * @param b
     * @param cmp
     * @return
     */
    public static <T> boolean lessThan(final T a, final T b, Comparator<? super T> cmp) {
        cmp = checkComparator(cmp);

        return cmp.compare(a, b) < 0;
    }

    /**
     * Returns {@code true} is {@code a <= b}, otherwise {@code false} is returned.
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    public static <T extends Comparable<? super T>> boolean lessEqual(final T a, final T b) {
        return compare(a, b) <= 0;
    }

    /**
     * Returns {@code true} is {@code a <= b}, otherwise {@code false} is returned.
     *
     * @param <T>
     * @param a
     * @param b
     * @param cmp
     * @return
     */
    public static <T> boolean lessEqual(final T a, final T b, Comparator<? super T> cmp) {
        cmp = checkComparator(cmp);

        return cmp.compare(a, b) <= 0;
    }

    /**
     * Returns {@code true} is {@code a > b}, otherwise {@code false} is returned.
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    public static <T extends Comparable<? super T>> boolean greaterThan(final T a, final T b) {
        return compare(a, b) > 0;
    }

    /**
     * Returns {@code true} is {@code a > b}, otherwise {@code false} is returned.
     *
     * @param <T>
     * @param a
     * @param b
     * @param cmp
     * @return
     */
    public static <T> boolean greaterThan(final T a, final T b, Comparator<? super T> cmp) {
        cmp = checkComparator(cmp);

        return cmp.compare(a, b) > 0;
    }

    /**
     * Returns {@code true} is {@code a >= b}, otherwise {@code false} is returned.
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    public static <T extends Comparable<? super T>> boolean greaterEqual(final T a, final T b) {
        return compare(a, b) >= 0;
    }

    /**
     * Returns {@code true} is {@code a >= b}, otherwise {@code false} is returned.
     *
     * @param <T>
     * @param a
     * @param b
     * @param cmp
     * @return
     */
    public static <T> boolean greaterEqual(final T a, final T b, Comparator<? super T> cmp) {
        cmp = checkComparator(cmp);

        return cmp.compare(a, b) >= 0;
    }

    /**
     * Returns {@code true} is {@code min < value < max}, otherwise {@code false} is returned.
     *
     * @param <T>
     * @param value
     * @param min
     * @param max
     * @return
     */
    public static <T extends Comparable<? super T>> boolean gtAndLt(final T value, final T min, final T max) {
        if (compare(value, min) <= 0) {
            return false;
        }

        return compare(value, max) < 0;
    }

    /**
     * Returns {@code true} is {@code min < value < max}, otherwise {@code false} is returned.
     *
     * @param <T>
     * @param value
     * @param min
     * @param max
     * @param cmp
     * @return
     */
    public static <T> boolean gtAndLt(final T value, final T min, final T max, Comparator<? super T> cmp) {
        cmp = checkComparator(cmp);

        if (cmp.compare(value, min) <= 0) {
            return false;
        }

        return cmp.compare(value, max) < 0;
    }

    /**
     * Returns {@code true} is {@code min <= value < max}, otherwise {@code false} is returned.
     *
     * @param <T>
     * @param value
     * @param min
     * @param max
     * @return
     */
    public static <T extends Comparable<? super T>> boolean geAndLt(final T value, final T min, final T max) {
        if (compare(value, min) < 0) {
            return false;
        }

        return compare(value, max) < 0;
    }

    /**
     * Returns {@code true} is {@code min <= value < max}, otherwise {@code false} is returned.
     *
     * @param <T>
     * @param value
     * @param min
     * @param max
     * @param cmp
     * @return
     */
    public static <T> boolean geAndLt(final T value, final T min, final T max, Comparator<? super T> cmp) {
        cmp = checkComparator(cmp);

        if (cmp.compare(value, min) < 0) {
            return false;
        }

        return cmp.compare(value, max) < 0;
    }

    /**
     * Returns {@code true} is {@code min <= value <= max}, otherwise {@code false} is returned.
     *
     * @param <T>
     * @param value
     * @param min
     * @param max
     * @return
     */
    public static <T extends Comparable<? super T>> boolean geAndLe(final T value, final T min, final T max) {
        if (compare(value, min) < 0) {
            return false;
        }

        return compare(value, max) <= 0;
    }

    /**
     * Returns {@code true} is {@code min <= value <= max}, otherwise {@code false} is returned.
     *
     * @param <T>
     * @param value
     * @param min
     * @param max
     * @param cmp
     * @return
     */
    public static <T> boolean geAndLe(final T value, final T min, final T max, Comparator<? super T> cmp) {
        cmp = checkComparator(cmp);

        if (cmp.compare(value, min) < 0) {
            return false;
        }

        return cmp.compare(value, max) <= 0;
    }

    /**
     * Returns {@code true} is {@code min < value <= max}, otherwise {@code false} is returned.
     *
     * @param <T>
     * @param value
     * @param min
     * @param max
     * @return
     */
    public static <T extends Comparable<? super T>> boolean gtAndLe(final T value, final T min, final T max) {
        if (compare(value, min) <= 0) {
            return false;
        }

        return compare(value, max) <= 0;
    }

    /**
     * Returns {@code true} is {@code min < value <= max}, otherwise {@code false} is returned.
     *
     * @param <T>
     * @param value
     * @param min
     * @param max
     * @param cmp
     * @return
     */
    public static <T> boolean gtAndLe(final T value, final T min, final T max, Comparator<? super T> cmp) {
        cmp = checkComparator(cmp);

        if (cmp.compare(value, min) <= 0) {
            return false;
        }

        return cmp.compare(value, max) <= 0;
    }

    /**
     *
     * @param <T>
     * @param value
     * @param min
     * @param max
     * @return
     * @deprecated replaced by {@link #gtAndLt(Comparable, Comparable, Comparable)}
     */
    @Deprecated
    public static <T extends Comparable<? super T>> boolean isBetween(final T value, final T min, final T max) {
        return gtAndLt(value, min, max);
    }

    /**
     *
     * @param <T>
     * @param value
     * @param min
     * @param max
     * @param cmp
     * @return
     * @deprecated replaced by {@link #gtAndLt(Comparable, Comparable, Comparable)}
     */
    @Deprecated
    public static <T> boolean isBetween(final T value, final T min, final T max, final Comparator<? super T> cmp) {
        return gtAndLt(value, min, max, cmp);
    }

    /**
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final boolean a, final boolean b) {
        return a == b;
    }

    /**
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final char a, final char b) {
        return a == b;
    }

    /**
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final byte a, final byte b) {
        return a == b;
    }

    /**
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final short a, final short b) {
        return a == b;
    }

    /**
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final int a, final int b) {
        return a == b;
    }

    /**
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final long a, final long b) {
        return a == b;
    }

    /**
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final float a, final float b) {
        return Float.compare(a, b) == 0;
    }

    /**
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final double a, final double b) {
        return Double.compare(a, b) == 0;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean equals(final String a, final String b) {
        return (a == null) ? b == null : (b == null ? false : a.length() == b.length() && a.equals(b));
    }

    /**
     * Equals ignore case.
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean equalsIgnoreCase(final String a, final String b) {
        return (a == null) ? b == null : (b == null ? false : a.equalsIgnoreCase(b));
    }

    /**
     * compare {@code a} and {@code b} by
     * {@link Arrays#equals(Object[], Object[])} if both of them are array.
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final Object a, final Object b) {
        if ((a == null) ? b == null : (b == null ? false : a.equals(b))) {
            return true;
        }

        if ((a != null) && (b != null)) {
            final Type<Object> typeA = typeOf(a.getClass());

            if (typeA.isPrimitiveArray()) {
                final Type<Object> typeB = typeOf(b.getClass());

                return typeA.clazz().equals(typeB.clazz()) && typeA.equals(a, b);
            } else if (typeA.isObjectArray()) {
                final Type<Object> typeB = typeOf(b.getClass());

                return typeB.isObjectArray() && typeA.equals(a, b);
            }
        }

        return false;
    }

    /**
     * compare {@code a} and {@code b} by
     * {@link Arrays#equals(Object[], Object[])} if both of them are array.
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean deepEquals(final Object a, final Object b) {
        if ((a == null) ? b == null : (b == null ? false : a.equals(b))) {
            return true;
        }

        if ((a != null) && (b != null) && a.getClass().isArray() && a.getClass().equals(b.getClass())) {
            return typeOf(a.getClass()).deepEquals(a, b);
        }

        return false;
    }

    /**
     *
     * @param a
     * @param b
     * @return boolean
     * @see Arrays#equals(boolean[], boolean[])
     */
    public static boolean equals(final boolean[] a, final boolean[] b) {
        return Arrays.equals(a, b);
    }

    /**
     *
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static boolean equals(final boolean[] a, final int fromIndexA, final boolean[] b, final int fromIndexB, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNegative(len, "len");
        checkFromIndexSize(fromIndexA, len, len(a));
        checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return true;
        }

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if (a[i] != b[j]) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param a
     * @param b
     * @return boolean
     * @see Arrays#equals(char[], char[])
     */
    public static boolean equals(final char[] a, final char[] b) {
        return Arrays.equals(a, b);
    }

    /**
     *
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static boolean equals(final char[] a, final int fromIndexA, final char[] b, final int fromIndexB, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNegative(len, "len");
        checkFromIndexSize(fromIndexA, len, len(a));
        checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return true;
        }

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if (a[i] != b[j]) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param a
     * @param b
     * @return boolean
     * @see Arrays#equals(byte[], byte[])
     */
    public static boolean equals(final byte[] a, final byte[] b) {
        return Arrays.equals(a, b);
    }

    /**
     *
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static boolean equals(final byte[] a, final int fromIndexA, final byte[] b, final int fromIndexB, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNegative(len, "len");
        checkFromIndexSize(fromIndexA, len, len(a));
        checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return true;
        }

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if (a[i] != b[j]) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param a
     * @param b
     * @return boolean
     * @see Arrays#equals(short[], short[])
     */
    public static boolean equals(final short[] a, final short[] b) {
        return Arrays.equals(a, b);
    }

    /**
     *
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static boolean equals(final short[] a, final int fromIndexA, final short[] b, final int fromIndexB, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNegative(len, "len");
        checkFromIndexSize(fromIndexA, len, len(a));
        checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return true;
        }

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if (a[i] != b[j]) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param a
     * @param b
     * @return boolean
     * @see Arrays#equals(int[], int[])
     */
    public static boolean equals(final int[] a, final int[] b) {
        return Arrays.equals(a, b);
    }

    /**
     *
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static boolean equals(final int[] a, final int fromIndexA, final int[] b, final int fromIndexB, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNegative(len, "len");
        checkFromIndexSize(fromIndexA, len, len(a));
        checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return true;
        }

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if (a[i] != b[j]) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param a
     * @param b
     * @return boolean
     * @see Arrays#equals(long[], long[])
     */
    public static boolean equals(final long[] a, final long[] b) {
        return Arrays.equals(a, b);
    }

    /**
     *
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static boolean equals(final long[] a, final int fromIndexA, final long[] b, final int fromIndexB, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNegative(len, "len");
        checkFromIndexSize(fromIndexA, len, len(a));
        checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return true;
        }

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if (a[i] != b[j]) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param a
     * @param b
     * @return boolean
     * @see Arrays#equals(float[], float[])
     */
    public static boolean equals(final float[] a, final float[] b) {
        return Arrays.equals(a, b);
    }

    /**
     *
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static boolean equals(final float[] a, final int fromIndexA, final float[] b, final int fromIndexB, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNegative(len, "len");
        checkFromIndexSize(fromIndexA, len, len(a));
        checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return true;
        }

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if (Float.compare(a[i], b[j]) != 0) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param a
     * @param b
     * @return boolean
     * @see Arrays#equals(double[], double[])
     */
    public static boolean equals(final double[] a, final double[] b) {
        return Arrays.equals(a, b);
    }

    /**
     *
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static boolean equals(final double[] a, final int fromIndexA, final double[] b, final int fromIndexB, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNegative(len, "len");
        checkFromIndexSize(fromIndexA, len, len(a));
        checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return true;
        }

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if (Double.compare(a[i], b[j]) != 0) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param a
     * @param b
     * @return boolean
     * @see Arrays#equals(Object[], Object[])
     */
    public static boolean equals(final Object[] a, final Object[] b) {
        return a == b || (a != null && b != null && a.length == b.length && equals(a, 0, b, 0, a.length));
    }

    /**
     *
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static boolean equals(final Object[] a, final int fromIndexA, final Object[] b, final int fromIndexB, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNegative(len, "len");
        checkFromIndexSize(fromIndexA, len, len(a));
        checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return true;
        } else if (!a.getClass().equals(b.getClass())) {
            return false;
        }

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if (!equals(a[i], b[j])) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see Arrays#deepEquals(Object[], Object[])
     */
    public static boolean deepEquals(final Object[] a, final Object[] b) {
        return a == b || (a != null && b != null && a.length == b.length && deepEquals(a, 0, b, 0, a.length));
    }

    /**
     *
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static boolean deepEquals(final Object[] a, final int fromIndexA, final Object[] b, final int fromIndexB, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNegative(len, "len");
        checkFromIndexSize(fromIndexA, len, len(a));
        checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return true;
        } else if (!a.getClass().equals(b.getClass())) {
            return false;
        }

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if (!deepEquals(a[i], b[j])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Equals ignore case.
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean equalsIgnoreCase(final String[] a, final String[] b) {
        return (a == null || b == null) ? a == b : (a.length == b.length && equalsIgnoreCase(a, 0, b, 0, a.length));
    }

    /**
     * Equals ignore case.
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     * @throws IllegalArgumentException
     * @throws IndexOutOfBoundsException
     */
    public static boolean equalsIgnoreCase(final String[] a, final int fromIndexA, final String[] b, final int fromIndexB, final int len)
            throws IllegalArgumentException, IndexOutOfBoundsException {
        checkArgNotNegative(len, "len");
        checkFromIndexSize(fromIndexA, len, len(a));
        checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return true;
        } else if (!a.getClass().equals(b.getClass())) {
            return false;
        }

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if (((a[i] == null || b[j] == null) ? (a != b) : !a[i].equalsIgnoreCase(b[j]))) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param bean1
     * @param bean2
     * @return
     * @throws IllegalArgumentException
     * @see Difference.MapDifference#of(Object, Object)
     */
    public static boolean equalsByCommonProps(final Object bean1, final Object bean2) throws IllegalArgumentException {
        N.checkArgNotNull(bean1);
        N.checkArgNotNull(bean2);
        N.checkArgument(ClassUtil.isBeanClass(bean1.getClass()), "{} is not a bean class", bean1.getClass());
        N.checkArgument(ClassUtil.isBeanClass(bean2.getClass()), "{} is not a bean class", bean2.getClass());

        BeanInfo beanInfo1 = ParserUtil.getBeanInfo(bean1.getClass());
        BeanInfo beanInfo2 = ParserUtil.getBeanInfo(bean2.getClass());

        PropInfo propInfo2 = null;
        Object propValue1 = null;

        for (PropInfo propInfo1 : beanInfo1.propInfoList) {
            propInfo2 = beanInfo2.getPropInfo(propInfo1);

            if (propInfo2 != null) {
                propValue1 = propInfo1.getPropValue(bean1);

                if (!equals(propValue1, propInfo2.getPropValue(bean2))) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     *
     *
     * @param bean1
     * @param bean2
     * @param propNamesToCompare
     * @return
     * @throws IllegalArgumentException
     * @see Difference.MapDifference#of(Object, Object)
     */
    public static boolean equalsByCommonProps(final Object bean1, final Object bean2, final Collection<String> propNamesToCompare)
            throws IllegalArgumentException {
        N.checkArgNotNull(bean1);
        N.checkArgNotNull(bean2);
        N.checkArgument(ClassUtil.isBeanClass(bean1.getClass()), "{} is not a bean class", bean1.getClass());
        N.checkArgument(ClassUtil.isBeanClass(bean2.getClass()), "{} is not a bean class", bean2.getClass());

        if (N.isEmpty(propNamesToCompare)) {
            return true;
        }

        BeanInfo beanInfo1 = ParserUtil.getBeanInfo(bean1.getClass());
        BeanInfo beanInfo2 = ParserUtil.getBeanInfo(bean2.getClass());

        PropInfo propInfo1 = null;
        PropInfo propInfo2 = null;
        Object propValue1 = null;

        for (String propName : propNamesToCompare) {
            propInfo1 = beanInfo1.getPropInfo(propName);

            if (propInfo1 != null) {
                propInfo2 = beanInfo2.getPropInfo(propInfo1);

                if (propInfo2 == null) {
                    throw new IllegalArgumentException("No field found in class: " + bean2.getClass() + " by name: " + propName);
                }
            } else {
                propInfo2 = beanInfo2.getPropInfo(propName);

                if (propInfo2 != null) {
                    propInfo1 = beanInfo1.getPropInfo(propInfo2);
                }

                if (propInfo1 == null) {
                    throw new IllegalArgumentException("No field found in class: " + bean1.getClass() + " by name: " + propName);
                }
            }

            propValue1 = propInfo1.getPropValue(bean1);

            if (!equals(propValue1, propInfo2.getPropValue(bean2))) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param value
     * @return int
     */
    public static int hashCode(final boolean value) {
        return value ? 1231 : 1237;
    }

    /**
     *
     * @param value
     * @return int
     */
    public static int hashCode(final char value) {
        return value;
    }

    /**
     *
     * @param value
     * @return int
     */
    public static int hashCode(final byte value) {
        return value;
    }

    /**
     *
     * @param value
     * @return int
     */
    public static int hashCode(final short value) {
        return value;
    }

    /**
     *
     * @param value
     * @return int
     */
    public static int hashCode(final int value) {
        return value;
    }

    /**
     *
     * @param value
     * @return int
     */
    public static int hashCode(final long value) {
        return (int) (value ^ (value >>> 32));
    }

    /**
     *
     * @param value
     * @return int
     */
    public static int hashCode(final float value) {
        return Float.floatToIntBits(value);
    }

    /**
     *
     * @param value
     * @return int
     */
    public static int hashCode(final double value) {
        long bits = Double.doubleToLongBits(value);

        return (int) (bits ^ (bits >>> 32));
    }

    /**
     *
     * @param obj
     * @return int
     */
    public static int hashCode(final Object obj) {
        if (obj == null) {
            return 0;
        }

        if (obj.getClass().isArray()) {
            return typeOf(obj.getClass()).hashCode(obj);
        }

        return obj.hashCode();
    }

    /**
     *
     * @param obj
     * @return int
     */
    public static int deepHashCode(final Object obj) {
        if (obj == null) {
            return 0;
        }

        if (obj.getClass().isArray()) {
            return typeOf(obj.getClass()).deepHashCode(obj);
        }

        return obj.hashCode();
    }

    /**
     *
     * @param a
     * @return
     * @see Arrays#hashCode(boolean[])
     */
    public static int hashCode(final boolean[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static int hashCode(final boolean[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return 0;
        }

        int result = 1;

        for (int i = fromIndex; i < toIndex; i++) {
            result = 31 * result + (a[i] ? 1231 : 1237);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     * @see Arrays#hashCode(char[])
     */
    public static int hashCode(final char[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static int hashCode(final char[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return 0;
        }

        int result = 1;

        for (int i = fromIndex; i < toIndex; i++) {
            result = 31 * result + a[i];
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     * @see Arrays#hashCode(byte[])
     */
    public static int hashCode(final byte[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static int hashCode(final byte[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return 0;
        }

        int result = 1;

        for (int i = fromIndex; i < toIndex; i++) {
            result = 31 * result + a[i];
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     * @see Arrays#hashCode(short[])
     */
    public static int hashCode(final short[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static int hashCode(final short[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return 0;
        }

        int result = 1;

        for (int i = fromIndex; i < toIndex; i++) {
            result = 31 * result + a[i];
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     * @see Arrays#hashCode(int[])
     */
    public static int hashCode(final int[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static int hashCode(final int[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return 0;
        }

        int result = 1;

        for (int i = fromIndex; i < toIndex; i++) {
            result = 31 * result + a[i];
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     * @see Arrays#hashCode(long[])
     */
    public static int hashCode(final long[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static int hashCode(final long[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return 0;
        }

        int result = 1;

        for (int i = fromIndex; i < toIndex; i++) {
            result = 31 * result + (int) (a[i] ^ (a[i] >>> 32));
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     * @see Arrays#hashCode(float[])
     */
    public static int hashCode(final float[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static int hashCode(final float[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return 0;
        }

        int result = 1;

        for (int i = fromIndex; i < toIndex; i++) {
            result = 31 * result + Float.floatToIntBits(a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     * @see Arrays#hashCode(double[])
     */
    public static int hashCode(final double[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static int hashCode(final double[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return 0;
        }

        int result = 1;

        for (int i = fromIndex; i < toIndex; i++) {
            long bits = Double.doubleToLongBits(a[i]);
            result = 31 * result + (int) (bits ^ (bits >>> 32));
        }

        return result;
    }

    /**
     *
     * @param a
     * @return
     * @see Arrays#hashCode(Object[])
     */
    public static int hashCode(final Object[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static int hashCode(final Object[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return 0;
        }

        int result = 1;

        for (int i = fromIndex; i < toIndex; i++) {
            result = 31 * result + (a[i] == null ? 0 : a[i].hashCode());
        }

        return result;
    }

    /**
     * Deep hash code.
     *
     * @param a
     * @return
     * @see Arrays#deepHashCode(Object[])
     */
    public static int deepHashCode(final Object[] a) {
        return a == null ? 0 : deepHashCode(a, 0, a.length);
    }

    /**
     * Deep hash code.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static int deepHashCode(final Object[] a, final int fromIndex, final int toIndex) {
        if (a == null) {
            return 0;
        }

        int result = 1;

        for (int i = fromIndex; i < toIndex; i++) {
            result = 31 * result + (a[i] == null ? 0 : deepHashCode(a[i]));
        }

        return result;
    }

    /**
     *
     * @param value
     * @return int
     */
    public static String toString(final boolean value) {
        return stringOf(value);
    }

    /**
     *
     * @param value
     * @return int
     */
    public static String toString(final char value) {
        return stringOf(value);
    }

    /**
     *
     * @param value
     * @return int
     */
    public static String toString(final byte value) {
        return stringOf(value);
    }

    /**
     *
     * @param value
     * @return int
     */
    public static String toString(final short value) {
        return stringOf(value);
    }

    /**
     *
     * @param value
     * @return int
     */
    public static String toString(final int value) {
        return stringOf(value);
    }

    /**
     *
     * @param value
     * @return int
     */
    public static String toString(final long value) {
        return stringOf(value);
    }

    /**
     *
     * @param value
     * @return int
     */
    public static String toString(final float value) {
        return stringOf(value);
    }

    /**
     *
     * @param value
     * @return int
     */
    public static String toString(final double value) {
        return stringOf(value);
    }

    /**
     *
     * @param obj
     * @return int
     */
    public static String toString(final Object obj) {
        if (obj == null) {
            return Strings.NULL_STRING;
        } else if (obj instanceof CharSequence) {
            return obj.toString();
        }

        if (obj.getClass().isArray()) {
            return typeOf(obj.getClass()).toString(obj);
        } else if (obj instanceof Iterator) {
            final Iterator<?> iter = (Iterator<?>) obj;
            final Joiner joiner = Joiner.with(", ", "[", "]").reuseCachedBuffer();

            while (iter.hasNext()) {
                joiner.append(toString(iter.next()));
            }

            return joiner.toString();
        }

        final Integer typeIdx = CLASS_TYPE_ENUM.get(obj.getClass());

        if (typeIdx == null) {
            return obj.toString();
        }

        switch (typeIdx) {
            case 21:
                return toString(((Boolean) obj).booleanValue());

            case 22:
                return toString(((Character) obj).charValue());

            case 23:
                return toString(((Byte) obj).byteValue());

            case 24:
                return toString(((Short) obj).shortValue());

            case 25:
                return toString(((Integer) obj).intValue());

            case 26:
                return toString(((Long) obj).longValue());

            //    case 27:
            //        return toString(((Float) obj).floatValue());
            //
            //    case 28:
            //        return toString(((Double) obj).doubleValue());

            default:
                return obj.toString();
        }
    }

    /**
     *
     * @param a
     * @param defaultIfNull
     * @return
     */
    public static String toString(final Object a, final String defaultIfNull) {
        return a == null ? defaultIfNull : toString(a);
    }

    /**
     *
     * @param a
     * @return
     * @see Arrays#toString(boolean[])
     */
    public static String toString(final boolean[] a) {
        if (a == null) {
            return Strings.NULL_STRING;
        }

        if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static String toString(final boolean[] a, final int fromIndex, final int toIndex) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, fromIndex, toIndex);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     *
     * @param sb
     * @param a
     */
    static void toString(final StringBuilder sb, final boolean[] a) {
        if (a == null) {
            sb.append(Strings.NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    /**
     *
     * @param sb
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    static void toString(final StringBuilder sb, final boolean[] a, final int fromIndex, final int toIndex) {
        sb.append(WD._BRACKET_L);

        for (int i = fromIndex; i < toIndex; i++) {
            if (i > fromIndex) {
                sb.append(WD.COMMA_SPACE);
            }

            sb.append(a[i]);
        }

        sb.append(WD._BRACKET_R);
    }

    /**
     *
     * @param a
     * @return
     * @see Arrays#toString(char[])
     */
    public static String toString(final char[] a) {
        if (a == null) {
            return Strings.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static String toString(final char[] a, final int fromIndex, final int toIndex) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, fromIndex, toIndex);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     *
     * @param sb
     * @param a
     */
    static void toString(final StringBuilder sb, final char[] a) {
        if (a == null) {
            sb.append(Strings.NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    /**
     *
     * @param sb
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    static void toString(final StringBuilder sb, final char[] a, final int fromIndex, final int toIndex) {
        sb.append(WD._BRACKET_L);

        for (int i = fromIndex; i < toIndex; i++) {
            if (i > fromIndex) {
                sb.append(WD.COMMA_SPACE);
            }

            sb.append(a[i]);
        }

        sb.append(WD._BRACKET_R);
    }

    /**
     *
     * @param a
     * @return
     * @see Arrays#toString(byte[])
     */
    public static String toString(final byte[] a) {
        if (a == null) {
            return Strings.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static String toString(final byte[] a, final int fromIndex, final int toIndex) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, fromIndex, toIndex);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     *
     * @param sb
     * @param a
     */
    static void toString(final StringBuilder sb, final byte[] a) {
        if (a == null) {
            sb.append(Strings.NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    /**
     *
     * @param sb
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    static void toString(final StringBuilder sb, final byte[] a, final int fromIndex, final int toIndex) {
        sb.append(WD._BRACKET_L);

        for (int i = fromIndex; i < toIndex; i++) {
            if (i > fromIndex) {
                sb.append(WD.COMMA_SPACE);
            }

            sb.append(a[i]);
        }

        sb.append(WD._BRACKET_R);
    }

    /**
     *
     * @param a
     * @return
     * @see Arrays#toString(short[])
     */
    public static String toString(final short[] a) {
        if (a == null) {
            return Strings.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static String toString(final short[] a, final int fromIndex, final int toIndex) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, fromIndex, toIndex);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     *
     * @param sb
     * @param a
     */
    static void toString(final StringBuilder sb, final short[] a) {
        if (a == null) {
            sb.append(Strings.NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    /**
     *
     * @param sb
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    static void toString(final StringBuilder sb, final short[] a, final int fromIndex, final int toIndex) {
        sb.append(WD._BRACKET_L);

        for (int i = fromIndex; i < toIndex; i++) {
            if (i > fromIndex) {
                sb.append(WD.COMMA_SPACE);
            }

            sb.append(a[i]);
        }

        sb.append(WD._BRACKET_R);
    }

    /**
     *
     * @param a
     * @return
     * @see Arrays#toString(int[])
     */
    public static String toString(final int[] a) {
        if (a == null) {
            return Strings.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static String toString(final int[] a, final int fromIndex, final int toIndex) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, fromIndex, toIndex);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     *
     * @param sb
     * @param a
     */
    static void toString(final StringBuilder sb, final int[] a) {
        if (a == null) {
            sb.append(Strings.NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    /**
     *
     * @param sb
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    static void toString(final StringBuilder sb, final int[] a, final int fromIndex, final int toIndex) {
        sb.append(WD._BRACKET_L);

        for (int i = fromIndex; i < toIndex; i++) {
            if (i > fromIndex) {
                sb.append(WD.COMMA_SPACE);
            }

            sb.append(a[i]);
        }

        sb.append(WD._BRACKET_R);
    }

    /**
     *
     * @param a
     * @return
     * @see Arrays#toString(long[])
     */
    public static String toString(final long[] a) {
        if (a == null) {
            return Strings.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static String toString(final long[] a, final int fromIndex, final int toIndex) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, fromIndex, toIndex);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     *
     * @param sb
     * @param a
     */
    static void toString(final StringBuilder sb, final long[] a) {
        if (a == null) {
            sb.append(Strings.NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    /**
     *
     * @param sb
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    static void toString(final StringBuilder sb, final long[] a, final int fromIndex, final int toIndex) {
        sb.append(WD._BRACKET_L);

        for (int i = fromIndex; i < toIndex; i++) {
            if (i > fromIndex) {
                sb.append(WD.COMMA_SPACE);
            }

            sb.append(a[i]);
        }

        sb.append(WD._BRACKET_R);
    }

    /**
     *
     * @param a
     * @return
     * @see Arrays#toString(float[])
     */
    public static String toString(final float[] a) {
        if (a == null) {
            return Strings.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static String toString(final float[] a, final int fromIndex, final int toIndex) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, fromIndex, toIndex);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     *
     * @param sb
     * @param a
     */
    static void toString(final StringBuilder sb, final float[] a) {
        if (a == null) {
            sb.append(Strings.NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    /**
     *
     * @param sb
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    static void toString(final StringBuilder sb, final float[] a, final int fromIndex, final int toIndex) {
        sb.append(WD._BRACKET_L);

        for (int i = fromIndex; i < toIndex; i++) {
            if (i > fromIndex) {
                sb.append(WD.COMMA_SPACE);
            }

            sb.append(a[i]);
        }

        sb.append(WD._BRACKET_R);
    }

    /**
     *
     * @param a
     * @return
     * @see Arrays#toString(double[])
     */
    public static String toString(final double[] a) {
        if (a == null) {
            return Strings.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static String toString(final double[] a, final int fromIndex, final int toIndex) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, fromIndex, toIndex);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     *
     * @param sb
     * @param a
     */
    static void toString(final StringBuilder sb, final double[] a) {
        if (a == null) {
            sb.append(Strings.NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    /**
     *
     * @param sb
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    static void toString(final StringBuilder sb, final double[] a, final int fromIndex, final int toIndex) {
        sb.append(WD._BRACKET_L);

        for (int i = fromIndex; i < toIndex; i++) {
            if (i > fromIndex) {
                sb.append(WD.COMMA_SPACE);
            }

            sb.append(a[i]);
        }

        sb.append(WD._BRACKET_R);
    }

    /**
     *
     * @param a
     * @return
     * @see Arrays#toString(Object[])
     */
    public static String toString(final Object[] a) {
        if (a == null) {
            return Strings.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static String toString(final Object[] a, final int fromIndex, final int toIndex) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, fromIndex, toIndex);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     *
     * @param sb
     * @param a
     */
    static void toString(final StringBuilder sb, final Object[] a) {
        if (a == null) {
            sb.append(Strings.NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    /**
     *
     * @param sb
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    static void toString(final StringBuilder sb, final Object[] a, final int fromIndex, final int toIndex) {
        sb.append(WD._BRACKET_L);

        for (int i = fromIndex; i < toIndex; i++) {
            if (i > fromIndex) {
                sb.append(WD.COMMA_SPACE);
            }

            sb.append(toString(a[i]));
        }

        sb.append(WD._BRACKET_R);
    }

    /**
     *
     * @param obj
     * @return int
     */
    public static String deepToString(final Object obj) {
        if (obj == null) {
            return Strings.NULL_STRING;
        }

        if (obj.getClass().isArray()) {
            return typeOf(obj.getClass()).deepToString(obj);
        }

        return obj.toString();
    }

    /**
     * Deep to string.
     *
     * @param a
     * @return
     * @see Arrays#deepToString(Object[])
     */
    public static String deepToString(final Object[] a) {
        if (a == null) {
            return Strings.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return deepToString(a, 0, a.length);
    }

    /**
     * Deep to string.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static String deepToString(final Object[] a, final int fromIndex, final int toIndex) {
        final StringBuilder sb = Objectory.createStringBuilder();
        final Set<Object[]> set = Objectory.createSet();

        try {
            deepToString(sb, a, fromIndex, toIndex, set);

            return sb.toString();
        } finally {
            Objectory.recycle(set);
            Objectory.recycle(sb);
        }
    }

    /**
     * Deep to string.
     *
     * @param sb
     * @param a
     * @param processedElements
     */
    static void deepToString(final StringBuilder sb, final Object[] a, final Set<Object[]> processedElements) {
        deepToString(sb, a, 0, a.length, processedElements);
    }

    /**
     * Deep to string.
     *
     * @param sb
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param processedElements
     */
    static void deepToString(final StringBuilder sb, final Object[] a, final int fromIndex, final int toIndex, final Set<Object[]> processedElements) {
        processedElements.add(a);

        sb.append(WD._BRACKET_L);

        Object element = null;
        Class<?> eClass = null;
        for (int i = fromIndex; i < toIndex; i++) {
            element = a[i];

            if (i > fromIndex) {
                sb.append(WD.COMMA_SPACE);
            }

            if (element == null) {
                sb.append(Strings.NULL_CHAR_ARRAY);

                continue;
            }

            eClass = element.getClass();

            if (eClass.isArray()) {
                Integer enumInt = CLASS_TYPE_ENUM.get(eClass);

                int num = enumInt == null ? 0 : enumInt;

                switch (num) {
                    case 11:
                        toString(sb, (boolean[]) element);
                        break;

                    case 12:
                        toString(sb, (char[]) element);
                        break;

                    case 13:
                        toString(sb, (byte[]) element);
                        break;

                    case 14:
                        toString(sb, (short[]) element);
                        break;

                    case 15:
                        toString(sb, (int[]) element);
                        break;

                    case 16:
                        toString(sb, (long[]) element);
                        break;

                    case 17:
                        toString(sb, (float[]) element);
                        break;

                    case 18:
                        toString(sb, (double[]) element);
                        break;

                    case 19:
                        toString(sb, (String[]) element);
                        break;

                    default:
                        if (processedElements.contains(element)) {
                            sb.append("[...]");
                        } else {
                            deepToString(sb, (Object[]) element, processedElements);
                        }
                }
            } else { // element is non-null and not an array
                sb.append(element.toString());
            }
        }

        sb.append(WD._BRACKET_R);

        processedElements.remove(a);
    }

    /**
     * Deep to string.
     *
     * @param a
     * @param defaultIfNull
     * @return
     */
    public static String deepToString(final Object[] a, final String defaultIfNull) {
        return a == null ? defaultIfNull : deepToString(a);
    }

    /**
     * <p>
     * Reverses the order of the given array.
     * </p>
     *
     * @param a
     */
    public static void reverse(final boolean[] a) {
        if (isEmpty(a)) {
            return;
        }

        reverse(a, 0, a.length);
    }

    /**
     * <p>
     * Reverses the order of the given array in the given range.
     * </p>
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void reverse(final boolean[] a, int fromIndex, int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || a.length == 1) {
            return;
        }

        boolean tmp = false;

        for (int i = fromIndex, j = toIndex - 1; i < j; i++, j--) {
            tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
    }

    /**
     * <p>
     * Reverses the order of the given array.
     * </p>
     *
     * @param a
     */
    public static void reverse(final char[] a) {
        if (isEmpty(a)) {
            return;
        }

        reverse(a, 0, a.length);
    }

    /**
     * <p>
     * Reverses the order of the given array in the given range.
     * </p>
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void reverse(final char[] a, int fromIndex, int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || a.length == 1) {
            return;
        }

        char tmp = 0;

        for (int i = fromIndex, j = toIndex - 1; i < j; i++, j--) {
            tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
    }

    /**
     * <p>
     * Reverses the order of the given array.
     * </p>
     *
     * @param a
     */
    public static void reverse(final byte[] a) {
        if (isEmpty(a)) {
            return;
        }

        reverse(a, 0, a.length);
    }

    /**
     * <p>
     * Reverses the order of the given array in the given range.
     * </p>
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void reverse(final byte[] a, int fromIndex, int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || a.length == 1) {
            return;
        }

        byte tmp = 0;

        for (int i = fromIndex, j = toIndex - 1; i < j; i++, j--) {
            tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
    }

    /**
     * <p>
     * Reverses the order of the given array.
     * </p>
     *
     * @param a
     */
    public static void reverse(final short[] a) {
        if (isEmpty(a)) {
            return;
        }

        reverse(a, 0, a.length);
    }

    /**
     * <p>
     * Reverses the order of the given array in the given range.
     * </p>
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void reverse(final short[] a, int fromIndex, int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || a.length == 1) {
            return;
        }

        short tmp = 0;

        for (int i = fromIndex, j = toIndex - 1; i < j; i++, j--) {
            tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
    }

    /**
     * <p>
     * Reverses the order of the given array.
     * </p>
     *
     * @param a
     */
    public static void reverse(final int[] a) {
        if (isEmpty(a)) {
            return;
        }

        reverse(a, 0, a.length);
    }

    /**
     * <p>
     * Reverses the order of the given array in the given range.
     * </p>
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void reverse(final int[] a, int fromIndex, int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || a.length == 1) {
            return;
        }

        int tmp = 0;

        for (int i = fromIndex, j = toIndex - 1; i < j; i++, j--) {
            tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
    }

    /**
     * <p>
     * Reverses the order of the given array.
     * </p>
     *
     * @param a
     */
    public static void reverse(final long[] a) {
        if (isEmpty(a)) {
            return;
        }

        reverse(a, 0, a.length);
    }

    /**
     * <p>
     * Reverses the order of the given array in the given range.
     * </p>
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void reverse(final long[] a, int fromIndex, int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || a.length == 1) {
            return;
        }

        long tmp = 0L; //NOSONAR

        for (int i = fromIndex, j = toIndex - 1; i < j; i++, j--) {
            tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
    }

    /**
     * <p>
     * Reverses the order of the given array.
     * </p>
     *
     * @param a
     */
    public static void reverse(final float[] a) {
        if (isEmpty(a)) {
            return;
        }

        reverse(a, 0, a.length);
    }

    /**
     * <p>
     * Reverses the order of the given array in the given range.
     * </p>
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void reverse(final float[] a, int fromIndex, int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || a.length == 1) {
            return;
        }

        float tmp = 0f; //NOSONAR

        for (int i = fromIndex, j = toIndex - 1; i < j; i++, j--) {
            tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
    }

    /**
     * <p>
     * Reverses the order of the given array.
     * </p>
     *
     * @param a
     */
    public static void reverse(final double[] a) {
        if (isEmpty(a)) {
            return;
        }

        reverse(a, 0, a.length);
    }

    /**
     * <p>
     * Reverses the order of the given array in the given range.
     * </p>
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void reverse(final double[] a, int fromIndex, int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || a.length == 1) {
            return;
        }

        double tmp = 0d; //NOSONAR

        for (int i = fromIndex, j = toIndex - 1; i < j; i++, j--) {
            tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
    }

    // Reverse
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Reverses the order of the given array.
     * </p>
     *
     * <p>
     * There is no special handling for multi-dimensional arrays.
     * </p>
     *
     * @param a
     */
    public static void reverse(final Object[] a) {
        if (isEmpty(a)) {
            return;
        }

        reverse(a, 0, a.length);
    }

    /**
     * <p>
     * Reverses the order of the given array in the given range.
     * </p>
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void reverse(final Object[] a, int fromIndex, int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || a.length == 1) {
            return;
        }

        Object tmp = null;

        for (int i = fromIndex, j = toIndex - 1; i < j; i++, j--) {
            tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
    }

    /**
     *
     * @param list
     */
    public static void reverse(final List<?> list) {
        if (isEmpty(list)) {
            return;
        }

        reverse(list, 0, list.size());
    }

    /**
     *
     *
     * @param list
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void reverse(final List<?> list, int fromIndex, int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, size(list));

        if (isEmpty(list) || list.size() == 1) {
            return;
        }

        final List<Object> l = (List<Object>) list;

        if (toIndex - fromIndex < REVERSE_THRESHOLD || list instanceof RandomAccess) {
            for (int i = fromIndex, j = toIndex - 1; i < j; i++, j--) {
                l.set(i, l.set(j, l.get(i)));
            }
        } else {
            final ListIterator<Object> fwd = l.listIterator(fromIndex);
            final ListIterator<Object> rev = l.listIterator(toIndex);

            for (int i = 0, mid = (toIndex - fromIndex) / 2; i < mid; i++) {
                Object tmp = fwd.next();
                fwd.set(rev.previous());
                rev.set(tmp);
            }
        }
    }

    /**
     *
     * @param c
     */
    @SuppressWarnings("rawtypes")
    public static void reverse(final Collection<?> c) {
        if (isEmpty(c) || c.size() < 2) {
            return;
        }

        if (c instanceof List) {
            reverse((List) c);
        } else {
            final Object[] tmp = c.toArray();
            reverse(tmp);
            c.clear();
            c.addAll((List) Arrays.asList(tmp));
        }
    }

    /**
     *
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> List<T> reverseToList(final Collection<? extends T> c) {
        if (isEmpty(c)) {
            return new ArrayList<>(0);
        }

        final List<T> result = new ArrayList<>(c);

        reverse(result);

        return result;
    }

    /**
     *
     * @param a
     * @param distance
     */
    public static void rotate(final boolean[] a, int distance) {
        if (a == null || a.length <= 1 || distance % a.length == 0) {
            return;
        }

        final int len = a.length;
        distance = distance % len;

        if (distance < 0) {
            distance += len;
        }

        if (distance == 0) {
            return;
        }

        for (int i = 0, count = 0; count < len; i++) {
            boolean tmp = a[i];
            int curr = i;
            int next = curr < distance ? curr - distance + len : curr - distance;

            while (next != i) {
                a[curr] = a[next];
                curr = next;
                next = curr < distance ? curr - distance + len : curr - distance;
                count++;
            }

            a[curr] = tmp;
            count++;
        }
    }

    /**
     *
     * @param a
     * @param distance
     */
    public static void rotate(final char[] a, int distance) {
        if (a == null || a.length <= 1 || distance % a.length == 0) {
            return;
        }

        final int len = a.length;
        distance = distance % len;

        if (distance < 0) {
            distance += len;
        }

        if (distance == 0) {
            return;
        }

        for (int i = 0, count = 0; count < len; i++) {
            char tmp = a[i];
            int curr = i;
            int next = curr < distance ? curr - distance + len : curr - distance;

            while (next != i) {
                a[curr] = a[next];
                curr = next;
                next = curr < distance ? curr - distance + len : curr - distance;
                count++;
            }

            a[curr] = tmp;
            count++;
        }
    }

    /**
     *
     * @param a
     * @param distance
     */
    public static void rotate(final byte[] a, int distance) {
        if (a == null || a.length <= 1 || distance % a.length == 0) {
            return;
        }

        final int len = a.length;
        distance = distance % len;

        if (distance < 0) {
            distance += len;
        }

        if (distance == 0) {
            return;
        }

        for (int i = 0, count = 0; count < len; i++) {
            byte tmp = a[i];
            int curr = i;
            int next = curr < distance ? curr - distance + len : curr - distance;

            while (next != i) {
                a[curr] = a[next];
                curr = next;
                next = curr < distance ? curr - distance + len : curr - distance;
                count++;
            }

            a[curr] = tmp;
            count++;
        }
    }

    /**
     *
     * @param a
     * @param distance
     */
    public static void rotate(final short[] a, int distance) {
        if (a == null || a.length <= 1 || distance % a.length == 0) {
            return;
        }

        final int len = a.length;
        distance = distance % len;

        if (distance < 0) {
            distance += len;
        }

        if (distance == 0) {
            return;
        }

        for (int i = 0, count = 0; count < len; i++) {
            short tmp = a[i];
            int curr = i;
            int next = curr < distance ? curr - distance + len : curr - distance;

            while (next != i) {
                a[curr] = a[next];
                curr = next;
                next = curr < distance ? curr - distance + len : curr - distance;
                count++;
            }

            a[curr] = tmp;
            count++;
        }
    }

    /**
     *
     * @param a
     * @param distance
     */
    public static void rotate(final int[] a, int distance) {
        if (a == null || a.length <= 1 || distance % a.length == 0) {
            return;
        }

        final int len = a.length;
        distance = distance % len;

        if (distance < 0) {
            distance += len;
        }

        if (distance == 0) {
            return;
        }

        for (int i = 0, count = 0; count < len; i++) {
            int tmp = a[i];
            int curr = i;
            int next = curr < distance ? curr - distance + len : curr - distance;

            while (next != i) {
                a[curr] = a[next];
                curr = next;
                next = curr < distance ? curr - distance + len : curr - distance;
                count++;
            }

            a[curr] = tmp;
            count++;
        }
    }

    /**
     *
     * @param a
     * @param distance
     */
    public static void rotate(final long[] a, int distance) {
        if (a == null || a.length <= 1 || distance % a.length == 0) {
            return;
        }

        final int len = a.length;
        distance = distance % len;

        if (distance < 0) {
            distance += len;
        }

        if (distance == 0) {
            return;
        }

        for (int i = 0, count = 0; count < len; i++) {
            long tmp = a[i];
            int curr = i;
            int next = curr < distance ? curr - distance + len : curr - distance;

            while (next != i) {
                a[curr] = a[next];
                curr = next;
                next = curr < distance ? curr - distance + len : curr - distance;
                count++;
            }

            a[curr] = tmp;
            count++;
        }
    }

    /**
     *
     * @param a
     * @param distance
     */
    public static void rotate(final float[] a, int distance) {
        if (a == null || a.length <= 1 || distance % a.length == 0) {
            return;
        }

        final int len = a.length;
        distance = distance % len;

        if (distance < 0) {
            distance += len;
        }

        if (distance == 0) {
            return;
        }

        for (int i = 0, count = 0; count < len; i++) {
            float tmp = a[i];
            int curr = i;
            int next = curr < distance ? curr - distance + len : curr - distance;

            while (next != i) {
                a[curr] = a[next];
                curr = next;
                next = curr < distance ? curr - distance + len : curr - distance;
                count++;
            }

            a[curr] = tmp;
            count++;
        }
    }

    /**
     *
     * @param a
     * @param distance
     */
    public static void rotate(final double[] a, int distance) {
        if (a == null || a.length <= 1 || distance % a.length == 0) {
            return;
        }

        final int len = a.length;
        distance = distance % len;

        if (distance < 0) {
            distance += len;
        }

        if (distance == 0) {
            return;
        }

        for (int i = 0, count = 0; count < len; i++) {
            double tmp = a[i];
            int curr = i;
            int next = curr < distance ? curr - distance + len : curr - distance;

            while (next != i) {
                a[curr] = a[next];
                curr = next;
                next = curr < distance ? curr - distance + len : curr - distance;
                count++;
            }

            a[curr] = tmp;
            count++;
        }
    }

    /**
     *
     * @param a
     * @param distance
     */
    public static void rotate(final Object[] a, int distance) {
        if (a == null || a.length <= 1 || distance % a.length == 0) {
            return;
        }

        final int len = a.length;
        distance = distance % len;

        if (distance < 0) {
            distance += len;
        }

        if (distance == 0) {
            return;
        }

        for (int i = 0, count = 0; count < len; i++) {
            Object tmp = a[i];
            int curr = i;
            int next = curr < distance ? curr - distance + len : curr - distance;

            while (next != i) {
                a[curr] = a[next];
                curr = next;
                next = curr < distance ? curr - distance + len : curr - distance;
                count++;
            }

            a[curr] = tmp;
            count++;
        }
    }

    /**
     *
     * @param list
     * @param distance
     * @see java.util.Collections#rotate(List, int)
     */
    public static void rotate(final List<?> list, final int distance) {
        if (list == null || list.size() <= 1 || distance % list.size() == 0) {
            return;
        }

        Collections.rotate(list, distance);
    }

    /**
     *
     * @param c
     * @param distance
     */
    @SuppressWarnings("rawtypes")
    public static void rotate(final Collection<?> c, final int distance) {
        if (isEmpty(c) || c.size() < 2) {
            return;
        }

        if (c instanceof List) {
            rotate((List) c, distance);
        } else {
            final Object[] tmp = c.toArray();
            rotate(tmp, distance);
            c.clear();
            c.addAll((List) Arrays.asList(tmp));
        }
    }

    /**
     *
     * @param a
     */
    public static void shuffle(final boolean[] a) {
        shuffle(a, RAND);
    }

    /**
     *
     * @param a
     * @param rnd
     */
    public static void shuffle(final boolean[] a, final Random rnd) {
        if (isEmpty(a) || a.length == 1) {
            return;
        }

        for (int i = a.length; i > 1; i--) {
            swap(a, i - 1, rnd.nextInt(i));
        }
    }

    /**
     *
     * @param a
     */
    public static void shuffle(final char[] a) {
        shuffle(a, RAND);
    }

    /**
     *
     * @param a
     * @param rnd
     */
    public static void shuffle(final char[] a, final Random rnd) {
        if (isEmpty(a) || a.length == 1) {
            return;
        }

        for (int i = a.length; i > 1; i--) {
            swap(a, i - 1, rnd.nextInt(i));
        }
    }

    /**
     *
     * @param a
     */
    public static void shuffle(final byte[] a) {
        shuffle(a, RAND);
    }

    /**
     *
     * @param a
     * @param rnd
     */
    public static void shuffle(final byte[] a, final Random rnd) {
        if (isEmpty(a) || a.length == 1) {
            return;
        }

        for (int i = a.length; i > 1; i--) {
            swap(a, i - 1, rnd.nextInt(i));
        }
    }

    /**
     *
     * @param a
     */
    public static void shuffle(final short[] a) {
        shuffle(a, RAND);
    }

    /**
     *
     * @param a
     * @param rnd
     */
    public static void shuffle(final short[] a, final Random rnd) {
        if (isEmpty(a) || a.length == 1) {
            return;
        }

        for (int i = a.length; i > 1; i--) {
            swap(a, i - 1, rnd.nextInt(i));
        }
    }

    /**
     *
     * @param a
     */
    public static void shuffle(final int[] a) {
        shuffle(a, RAND);
    }

    /**
     *
     * @param a
     * @param rnd
     */
    public static void shuffle(final int[] a, final Random rnd) {
        if (isEmpty(a) || a.length == 1) {
            return;
        }

        for (int i = a.length; i > 1; i--) {
            swap(a, i - 1, rnd.nextInt(i));
        }
    }

    /**
     *
     * @param a
     */
    public static void shuffle(final long[] a) {
        shuffle(a, RAND);
    }

    /**
     *
     * @param a
     * @param rnd
     */
    public static void shuffle(final long[] a, final Random rnd) {
        if (isEmpty(a) || a.length == 1) {
            return;
        }

        for (int i = a.length; i > 1; i--) {
            swap(a, i - 1, rnd.nextInt(i));
        }
    }

    /**
     *
     * @param a
     */
    public static void shuffle(final float[] a) {
        shuffle(a, RAND);
    }

    /**
     *
     * @param a
     * @param rnd
     */
    public static void shuffle(final float[] a, final Random rnd) {
        if (isEmpty(a) || a.length == 1) {
            return;
        }

        for (int i = a.length; i > 1; i--) {
            swap(a, i - 1, rnd.nextInt(i));
        }
    }

    /**
     *
     * @param a
     */
    public static void shuffle(final double[] a) {
        shuffle(a, RAND);
    }

    /**
     *
     * @param a
     * @param rnd
     */
    public static void shuffle(final double[] a, final Random rnd) {
        if (isEmpty(a) || a.length == 1) {
            return;
        }

        for (int i = a.length; i > 1; i--) {
            swap(a, i - 1, rnd.nextInt(i));
        }
    }

    /**
     *
     * @param <T>
     * @param a
     */
    public static <T> void shuffle(final T[] a) {
        shuffle(a, RAND);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param rnd
     */
    public static <T> void shuffle(final T[] a, final Random rnd) {
        if (isEmpty(a) || a.length == 1) {
            return;
        }

        for (int i = a.length; i > 1; i--) {
            swap(a, i - 1, rnd.nextInt(i));
        }
    }

    /**
     *
     * @param list
     */
    public static void shuffle(final List<?> list) {
        shuffle(list, RAND);
    }

    /**
     *
     * @param list
     * @param rnd
     * @see java.util.Collections#shuffle(List, Random)
     */
    public static void shuffle(final List<?> list, final Random rnd) {
        if (isEmpty(list) || list.size() == 1) {
            return;
        }

        Collections.shuffle(list, rnd);
    }

    /**
     *
     * @param c
     */
    @SuppressWarnings("rawtypes")
    public static void shuffle(final Collection<?> c) {
        if (isEmpty(c) || c.size() < 2) {
            return;
        }

        if (c instanceof List) {
            shuffle((List) c);
        } else {
            final Object[] tmp = c.toArray();
            shuffle(tmp);
            c.clear();
            c.addAll((List) Arrays.asList(tmp));
        }
    }

    /**
     *
     * @param c
     * @param rnd
     */
    @SuppressWarnings("rawtypes")
    public static void shuffle(final Collection<?> c, final Random rnd) {
        if (isEmpty(c) || c.size() < 2) {
            return;
        }

        if (c instanceof List) {
            shuffle((List) c, rnd);
        } else {
            final Object[] tmp = c.toArray();
            shuffle(tmp, rnd);
            c.clear();
            c.addAll((List) Arrays.asList(tmp));
        }
    }

    /**
     *
     * @param a
     * @param i
     * @param j
     */
    public static void swap(final boolean[] a, final int i, final int j) {
        final boolean tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    /**
     *
     * @param a
     * @param i
     * @param j
     */
    public static void swap(final char[] a, final int i, final int j) {
        final char tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    /**
     *
     * @param a
     * @param i
     * @param j
     */
    public static void swap(final byte[] a, final int i, final int j) {
        final byte tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    /**
     *
     * @param a
     * @param i
     * @param j
     */
    public static void swap(final short[] a, final int i, final int j) {
        final short tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    /**
     *
     * @param a
     * @param i
     * @param j
     */
    public static void swap(final int[] a, final int i, final int j) {
        final int tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    /**
     *
     * @param a
     * @param i
     * @param j
     */
    public static void swap(final long[] a, final int i, final int j) {
        final long tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    /**
     *
     * @param a
     * @param i
     * @param j
     */
    public static void swap(final float[] a, final int i, final int j) {
        final float tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    /**
     *
     * @param a
     * @param i
     * @param j
     */
    public static void swap(final double[] a, final int i, final int j) {
        final double tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    /**
     *
     * @param a
     * @param i
     * @param j
     */
    public static void swap(final Object[] a, final int i, final int j) {
        final Object tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    /**
     *
     * @param list
     * @param i
     * @param j
     */
    public static void swap(final List<?> list, final int i, final int j) {
        Collections.swap(list, i, j);
    }

    /**
     *
     * @param <T>
     * @param pair
     */
    public static <T> void swap(final Pair<T, T> pair) {
        pair.set(pair.right, pair.left);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param pair
     * @param predicate
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> boolean swapIf(final Pair<T, T> pair, Throwables.Predicate<? super Pair<T, T>, E> predicate) throws E {
        if (predicate.test(pair)) {
            pair.set(pair.right, pair.left);
            return true;
        }

        return false;
    }

    /**
     *
     * @param <T>
     * @param <M>
     * @param triple
     */
    public static <T, M> void swap(final Triple<T, M, T> triple) {
        final T left = triple.left;
        triple.setLeft(triple.right);
        triple.setRight(left);
    }

    /**
     *
     * @param <T>
     * @param <M>
     * @param <E>
     * @param triple
     * @param predicate
     * @return
     * @throws E the e
     */
    public static <T, M, E extends Exception> boolean swapIf(final Triple<T, M, T> triple, Throwables.Predicate<? super Triple<T, M, T>, E> predicate)
            throws E {
        if (predicate.test(triple)) {
            final T left = triple.left;
            triple.setLeft(triple.right);
            triple.setRight(left);
            return true;
        }

        return false;
    }

    /**
     *
     * @param a
     * @param val
     */
    public static void fill(final boolean[] a, final boolean val) {
        Arrays.fill(a, val);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param val
     */
    public static void fill(final boolean[] a, final int fromIndex, final int toIndex, final boolean val) {
        Arrays.fill(a, fromIndex, toIndex, val);
    }

    /**
     *
     * @param a
     * @param val
     */
    public static void fill(final char[] a, final char val) {
        Arrays.fill(a, val);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param val
     */
    public static void fill(final char[] a, final int fromIndex, final int toIndex, final char val) {
        Arrays.fill(a, fromIndex, toIndex, val);
    }

    /**
     *
     * @param a
     * @param val
     */
    public static void fill(final byte[] a, final byte val) {
        Arrays.fill(a, val);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param val
     */
    public static void fill(final byte[] a, final int fromIndex, final int toIndex, final byte val) {
        Arrays.fill(a, fromIndex, toIndex, val);
    }

    /**
     *
     * @param a
     * @param val
     */
    public static void fill(final short[] a, final short val) {
        Arrays.fill(a, val);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param val
     */
    public static void fill(final short[] a, final int fromIndex, final int toIndex, final short val) {
        Arrays.fill(a, fromIndex, toIndex, val);
    }

    /**
     *
     * @param a
     * @param val
     */
    public static void fill(final int[] a, final int val) {
        Arrays.fill(a, val);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param val
     */
    public static void fill(final int[] a, final int fromIndex, final int toIndex, final int val) {
        Arrays.fill(a, fromIndex, toIndex, val);
    }

    /**
     *
     * @param a
     * @param val
     */
    public static void fill(final long[] a, final long val) {
        Arrays.fill(a, val);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param val
     */
    public static void fill(final long[] a, final int fromIndex, final int toIndex, final long val) {
        Arrays.fill(a, fromIndex, toIndex, val);
    }

    /**
     *
     * @param a
     * @param val
     */
    public static void fill(final float[] a, final float val) {
        Arrays.fill(a, val);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param val
     */
    public static void fill(final float[] a, final int fromIndex, final int toIndex, final float val) {
        Arrays.fill(a, fromIndex, toIndex, val);
    }

    /**
     *
     * @param a
     * @param val
     */
    public static void fill(final double[] a, final double val) {
        Arrays.fill(a, val);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param val
     */
    public static void fill(final double[] a, final int fromIndex, final int toIndex, final double val) {
        Arrays.fill(a, fromIndex, toIndex, val);
    }

    /**
     *
     * @param a
     * @param val
     */
    public static void fill(final Object[] a, final Object val) {
        Arrays.fill(a, val);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param val
     */
    public static void fill(final Object[] a, final int fromIndex, final int toIndex, final Object val) {
        Arrays.fill(a, fromIndex, toIndex, val);
    }

    /**
     *
     * @param <T>
     * @param list
     * @param val
     * @see #padLeft(List, int, Object)
     * @see #padRight(Collection, int, Object)
     */
    public static <T> void fill(final List<? super T> list, final T val) {
        fill(list, 0, list.size(), val);
    }

    /**
     * The specified value will be added/inserted into the specified List.
     * The List will be extended automatically if the size of the List is less than the specified toIndex.
     *
     * @param <T>
     * @param list
     * @param fromIndex
     * @param toIndex
     * @param val
     * @throws IndexOutOfBoundsException
     * @see #padLeft(List, int, Object)
     * @see #padRight(Collection, int, Object)
     */
    public static <T> void fill(final List<? super T> list, final int fromIndex, final int toIndex, final T val) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, Integer.MAX_VALUE);

        int size = list.size();

        if (size < toIndex) {
            if (fromIndex < size) {
                for (int i = fromIndex; i < size; i++) {
                    list.set(i, val);
                }
            } else {
                for (int i = size; i < fromIndex; i++) {
                    list.add(null);
                }
            }

            for (int i = 0, len = toIndex - list.size(); i < len; i++) {
                list.add(val);
            }
        } else {
            if (toIndex - fromIndex < FILL_THRESHOLD || list instanceof RandomAccess) {
                for (int i = fromIndex; i < toIndex; i++) {
                    list.set(i, val);
                }
            } else {
                final ListIterator<? super T> itr = list.listIterator(fromIndex);

                for (int i = fromIndex; i < toIndex; i++) {
                    itr.next();

                    itr.set(val);
                }
            }
        }
    }

    /**
     * Fill the properties of the bean with random values.
     *
     * @param bean a bean object with getter/setter method
     */
    public static void fill(Object bean) {
        TestUtil.fill(bean);
    }

    /**
     * Fill the properties of the bean with random values.
     *
     * @param <T>
     * @param beanClass bean class with getter/setter methods
     * @return
     */
    public static <T> T fill(Class<? extends T> beanClass) {
        return TestUtil.fill(beanClass);
    }

    /**
     * Fill the properties of the bean with random values.
     *
     * @param <T>
     * @param beanClass bean class with getter/setter methods
     * @param count
     * @return
     */
    public static <T> List<T> fill(Class<? extends T> beanClass, int count) {
        return TestUtil.fill(beanClass, count);
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param c
    //     * @param valueToAdd
    //     * @param minSize
    //     * @return
    //     */
    //    @Beta
    //    public static <T> boolean append(final Collection<T> c, final T valueToAdd, final int minSize) {
    //        return N.padRight(c, minSize, valueToAdd);
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param list
    //     * @param valueToAdd
    //     * @param fromIndex
    //     * @param minSize
    //     * @return
    //     */
    //    @Beta
    //    public static <T> boolean append(final List<T> list, final T valueToAdd, final int fromIndex, final int minSize) {
    //        return N.padLeft(list, minSize, valueToAdd);
    //    }

    /**
     *
     *
     * @param <T>
     * @param list
     * @param minSize
     * @param objToAdd
     * @return
     * @throws IllegalArgumentException
     * @see #fill(List, Object)
     * @see #fill(List, int, int, Object)
     */
    @SuppressWarnings("rawtypes")
    public static <T> boolean padLeft(final List<T> list, final int minSize, final T objToAdd) throws IllegalArgumentException {
        N.checkArgNotNegative(minSize, "minSize");

        final int size = N.size(list);

        if (size < minSize) {
            final int elementCountToAdd = minSize - size;
            final Object[] a = new Object[elementCountToAdd];

            if (objToAdd != null) {
                N.fill(a, objToAdd);
            }

            list.addAll(0, (List) Arrays.asList(a));

            return true;
        }

        return false;
    }

    /**
     *
     *
     * @param <T>
     * @param c
     * @param minSize
     * @param objToAdd
     * @return
     * @throws IllegalArgumentException
     * @see #fill(List, Object)
     * @see #fill(List, int, int, Object)
     */
    @SuppressWarnings("rawtypes")
    public static <T> boolean padRight(final Collection<T> c, final int minSize, final T objToAdd) throws IllegalArgumentException {
        N.checkArgNotNegative(minSize, "minSize");

        final int size = N.size(c);

        if (size < minSize) {
            final int elementCountToAdd = minSize - size;
            final Object[] a = new Object[elementCountToAdd];

            if (objToAdd != null) {
                N.fill(a, objToAdd);
            }

            c.addAll((Collection) Arrays.asList(a));

            return true;
        }

        return false;
    }

    /**
     *
     *
     * @param <T>
     * @param value
     * @param n
     * @return
     * @throws IllegalArgumentException
     * @see Iterators#repeat(Object, int)
     */
    public static <T> List<T> repeat(final T value, final int n) throws IllegalArgumentException {
        checkArgNotNegative(n, "n");

        final List<T> res = new ArrayList<>(n);
        fill(res, 0, n, value);
        return res;
    }

    /**
     * Repeats the elements in the specified Collection one by one.
     *
     * <pre>
     * <code>
     * repeatElements(N.asList(1, 2, 3), 2) => [1, 1, 2, 2, 3, 3]
     * </code>
     * </pre>
     *
     * @param <T>
     * @param c
     * @param n
     * @return
     * @throws IllegalArgumentException
     * @see Iterators#repeatElements(Collection, long)
     */
    public static <T> List<T> repeatElements(final Collection<? extends T> c, final int n) throws IllegalArgumentException {
        checkArgNotNegative(n, "n");

        if (n == 0 || isEmpty(c)) {
            return new ArrayList<>();
        }

        final List<T> result = new ArrayList<>(c.size() * n);

        for (T e : c) {
            for (int i = 0; i < n; i++) {
                result.add(e);
            }
        }

        return result;
    }

    /**
     * Repeats the whole specified Collection {@code n} times.
     *
     * <pre>
     * <code>
     * repeatCollection(N.asList(1, 2, 3), 2) => [1, 2, 3, 1, 2, 3]
     * </code>
     * </pre>
     *
     * @param <T>
     * @param c
     * @param n
     * @return
     * @throws IllegalArgumentException
     * @see Iterators#repeatCollection(Collection, long)
     */
    public static <T> List<T> repeatCollection(final Collection<T> c, final int n) throws IllegalArgumentException {
        checkArgNotNegative(n, "n");

        if (n == 0 || isEmpty(c)) {
            return new ArrayList<>();
        }

        final List<T> result = new ArrayList<>(c.size() * n);

        for (int i = 0; i < n; i++) {
            result.addAll(c);
        }

        return result;
    }

    /**
     * Repeats the elements in the specified Collection one by one till reach the specified size.
     *
     * <pre>
     * <code>
     * repeatElementsToSize(N.asList(1, 2, 3), 5) => [1, 1, 2, 2, 3]
     * </code>
     * </pre>
     *
     * @param <T>
     * @param c
     * @param size
     * @return
     * @throws IllegalArgumentException
     * @see Iterators#repeatElementsToSize(Collection, long)
     */
    public static <T> List<T> repeatElementsToSize(final Collection<T> c, final int size) throws IllegalArgumentException {
        checkArgNotNegative(size, "size");
        checkArgument(size == 0 || notEmpty(c), "Collection can not be empty or null when size > 0");

        if (size == 0 || isEmpty(c)) {
            return new ArrayList<>();
        }

        final int n = size / c.size();
        int mod = size % c.size();

        final List<T> result = new ArrayList<>(size);

        for (T e : c) {
            for (int i = 0, cnt = mod-- > 0 ? n + 1 : n; i < cnt; i++) {
                result.add(e);
            }

            if (result.size() == size) {
                break;
            }
        }

        return result;
    }

    /**
     * Repeats the whole specified Collection till reach the specified size.
     *
     * <pre>
     * <code>
     * repeatCollectionToSize(N.asList(1, 2, 3), 5) => [1, 2, 3, 1, 2]
     * </code>
     * </pre>
     *
     * @param <T>
     * @param c
     * @param size
     * @return
     * @throws IllegalArgumentException
     * @see Iterators#repeatCollectionToSize(Collection, long)
     */
    public static <T> List<T> repeatCollectionToSize(final Collection<? extends T> c, final int size) throws IllegalArgumentException {
        checkArgNotNegative(size, "size");
        checkArgument(size == 0 || notEmpty(c), "Collection can not be empty or null when size > 0");

        if (size == 0 || isEmpty(c)) {
            return new ArrayList<>();
        }

        final List<T> result = new ArrayList<>(size);

        while (result.size() < size) {
            if (c.size() <= size - result.size()) {
                result.addAll(c);
            } else {
                final Iterator<? extends T> iter = c.iterator();

                for (int i = 0, cnt = size - result.size(); i < cnt; i++) {
                    result.add(iter.next());
                }
            }
        }

        return result;
    }

    /**
     * Copies all of the elements from one list into another.  After the
     * operation, the index of each copied element in the destination list
     * will be identical to its index in the source list.  The destination
     * list must be at least as long as the source list.  If it is longer, the
     * remaining elements in the destination list are unaffected. <p>
     *
     * This method runs in linear time.
     *
     * @param <T>
     * @param src
     * @param dest
     * @throws IndexOutOfBoundsException if the destination list is too small
     *         to contain the entire source List.
     * @throws UnsupportedOperationException if the destination list's
     *         list-iterator does not support the <tt>set</tt> operation.
     * @see java.util.Collections#copy(List, List)
     */
    public static <T> void copy(final List<? extends T> src, final List<? super T> dest) {
        if (N.isEmpty(src)) {
            return;
        }

        if (src.size() > dest.size()) {
            throw new IndexOutOfBoundsException("Source does not fit in dest");
        }

        Collections.copy(dest, src);
    }

    /**
     *
     *
     * @param <T>
     * @param src
     * @param srcPos
     * @param dest
     * @param destPos
     * @param length
     * @throws IndexOutOfBoundsException
     */
    public static <T> void copy(final List<? extends T> src, final int srcPos, final List<? super T> dest, final int destPos, final int length)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(srcPos, srcPos + length, size(src));
        N.checkFromToIndex(destPos, destPos + length, size(dest));

        if (N.isEmpty(src) && srcPos == 0 && length == 0) {
            return;
        }

        if (src instanceof RandomAccess && dest instanceof RandomAccess) {
            for (int i = 0; i < length; i++) {
                dest.set(destPos + i, src.get(srcPos + i));
            }
        } else {
            final ListIterator<? extends T> srcIterator = src.listIterator();
            final ListIterator<? super T> destIterator = dest.listIterator();

            int idx = 0;
            while (idx < srcPos) {
                srcIterator.next();
                idx++;
            }

            idx = 0;
            while (idx < destPos) {
                destIterator.next();
                idx++;
            }

            for (int i = 0; i < length; i++) {
                destIterator.next();
                destIterator.set(srcIterator.next());
            }
        }
    }

    /**
     *
     *
     * @param src
     * @param srcPos
     * @param dest
     * @param destPos
     * @param length
     * @throws IndexOutOfBoundsException
     */
    public static void copy(final boolean[] src, final int srcPos, final boolean[] dest, final int destPos, final int length) throws IndexOutOfBoundsException {
        N.checkFromToIndex(srcPos, srcPos + length, len(src));
        N.checkFromToIndex(destPos, destPos + length, len(dest));

        if (N.isEmpty(src) && srcPos == 0 && length == 0) {
            return;
        }

        if (length < MIN_SIZE_FOR_COPY_ALL) {
            // for same array copy.
            if (destPos > srcPos) {
                for (int i = length - 1; i >= 0; i--) {
                    dest[destPos + i] = src[srcPos + i];
                }
            } else {
                for (int i = 0; i < length; i++) {
                    dest[destPos + i] = src[srcPos + i];
                }
            }
        } else {
            System.arraycopy(src, srcPos, dest, destPos, length);
        }
    }

    /**
     *
     *
     * @param src
     * @param srcPos
     * @param dest
     * @param destPos
     * @param length
     * @throws IndexOutOfBoundsException
     */
    public static void copy(final char[] src, final int srcPos, final char[] dest, final int destPos, final int length) throws IndexOutOfBoundsException {
        N.checkFromToIndex(srcPos, srcPos + length, len(src));
        N.checkFromToIndex(destPos, destPos + length, len(dest));

        if (N.isEmpty(src) && srcPos == 0 && length == 0) {
            return;
        }

        if (length < MIN_SIZE_FOR_COPY_ALL) {
            // for same array copy.
            if (destPos > srcPos) {
                for (int i = length - 1; i >= 0; i--) {
                    dest[destPos + i] = src[srcPos + i];
                }
            } else {
                for (int i = 0; i < length; i++) {
                    dest[destPos + i] = src[srcPos + i];
                }
            }
        } else {
            System.arraycopy(src, srcPos, dest, destPos, length);
        }
    }

    /**
     *
     *
     * @param src
     * @param srcPos
     * @param dest
     * @param destPos
     * @param length
     * @throws IndexOutOfBoundsException
     */
    public static void copy(final byte[] src, final int srcPos, final byte[] dest, final int destPos, final int length) throws IndexOutOfBoundsException {
        N.checkFromToIndex(srcPos, srcPos + length, len(src));
        N.checkFromToIndex(destPos, destPos + length, len(dest));

        if (N.isEmpty(src) && srcPos == 0 && length == 0) {
            return;
        }

        if (length < MIN_SIZE_FOR_COPY_ALL) {
            // for same array copy.
            if (destPos > srcPos) {
                for (int i = length - 1; i >= 0; i--) {
                    dest[destPos + i] = src[srcPos + i];
                }
            } else {
                for (int i = 0; i < length; i++) {
                    dest[destPos + i] = src[srcPos + i];
                }
            }
        } else {
            System.arraycopy(src, srcPos, dest, destPos, length);
        }
    }

    /**
     *
     *
     * @param src
     * @param srcPos
     * @param dest
     * @param destPos
     * @param length
     * @throws IndexOutOfBoundsException
     */
    public static void copy(final short[] src, final int srcPos, final short[] dest, final int destPos, final int length) throws IndexOutOfBoundsException {
        N.checkFromToIndex(srcPos, srcPos + length, len(src));
        N.checkFromToIndex(destPos, destPos + length, len(dest));

        if (N.isEmpty(src) && srcPos == 0 && length == 0) {
            return;
        }

        if (length < MIN_SIZE_FOR_COPY_ALL) {
            // for same array copy.
            if (destPos > srcPos) {
                for (int i = length - 1; i >= 0; i--) {
                    dest[destPos + i] = src[srcPos + i];
                }
            } else {
                for (int i = 0; i < length; i++) {
                    dest[destPos + i] = src[srcPos + i];
                }
            }
        } else {
            System.arraycopy(src, srcPos, dest, destPos, length);
        }
    }

    /**
     *
     *
     * @param src
     * @param srcPos
     * @param dest
     * @param destPos
     * @param length
     * @throws IndexOutOfBoundsException
     */
    public static void copy(final int[] src, final int srcPos, final int[] dest, final int destPos, final int length) throws IndexOutOfBoundsException {
        N.checkFromToIndex(srcPos, srcPos + length, len(src));
        N.checkFromToIndex(destPos, destPos + length, len(dest));

        if (N.isEmpty(src) && srcPos == 0 && length == 0) {
            return;
        }

        if (length < MIN_SIZE_FOR_COPY_ALL) {
            // for same array copy.
            if (destPos > srcPos) {
                for (int i = length - 1; i >= 0; i--) {
                    dest[destPos + i] = src[srcPos + i];
                }
            } else {
                for (int i = 0; i < length; i++) {
                    dest[destPos + i] = src[srcPos + i];
                }
            }
        } else {
            System.arraycopy(src, srcPos, dest, destPos, length);
        }
    }

    /**
     *
     *
     * @param src
     * @param srcPos
     * @param dest
     * @param destPos
     * @param length
     * @throws IndexOutOfBoundsException
     */
    public static void copy(final long[] src, final int srcPos, final long[] dest, final int destPos, final int length) throws IndexOutOfBoundsException {
        N.checkFromToIndex(srcPos, srcPos + length, len(src));
        N.checkFromToIndex(destPos, destPos + length, len(dest));

        if (N.isEmpty(src) && srcPos == 0 && length == 0) {
            return;
        }

        if (length < MIN_SIZE_FOR_COPY_ALL) {
            // for same array copy.
            if (destPos > srcPos) {
                for (int i = length - 1; i >= 0; i--) {
                    dest[destPos + i] = src[srcPos + i];
                }
            } else {
                for (int i = 0; i < length; i++) {
                    dest[destPos + i] = src[srcPos + i];
                }
            }
        } else {
            System.arraycopy(src, srcPos, dest, destPos, length);
        }
    }

    /**
     *
     *
     * @param src
     * @param srcPos
     * @param dest
     * @param destPos
     * @param length
     * @throws IndexOutOfBoundsException
     */
    public static void copy(final float[] src, final int srcPos, final float[] dest, final int destPos, final int length) throws IndexOutOfBoundsException {
        N.checkFromToIndex(srcPos, srcPos + length, len(src));
        N.checkFromToIndex(destPos, destPos + length, len(dest));

        if (N.isEmpty(src) && srcPos == 0 && length == 0) {
            return;
        }

        if (length < MIN_SIZE_FOR_COPY_ALL) {
            // for same array copy.
            if (destPos > srcPos) {
                for (int i = length - 1; i >= 0; i--) {
                    dest[destPos + i] = src[srcPos + i];
                }
            } else {
                for (int i = 0; i < length; i++) {
                    dest[destPos + i] = src[srcPos + i];
                }
            }
        } else {
            System.arraycopy(src, srcPos, dest, destPos, length);
        }
    }

    /**
     *
     *
     * @param src
     * @param srcPos
     * @param dest
     * @param destPos
     * @param length
     * @throws IndexOutOfBoundsException
     */
    public static void copy(final double[] src, final int srcPos, final double[] dest, final int destPos, final int length) throws IndexOutOfBoundsException {
        N.checkFromToIndex(srcPos, srcPos + length, len(src));
        N.checkFromToIndex(destPos, destPos + length, len(dest));

        if (N.isEmpty(src) && srcPos == 0 && length == 0) {
            return;
        }

        if (length < MIN_SIZE_FOR_COPY_ALL) {
            // for same array copy.
            if (destPos > srcPos) {
                for (int i = length - 1; i >= 0; i--) {
                    dest[destPos + i] = src[srcPos + i];
                }
            } else {
                for (int i = 0; i < length; i++) {
                    dest[destPos + i] = src[srcPos + i];
                }
            }
        } else {
            System.arraycopy(src, srcPos, dest, destPos, length);
        }
    }

    /**
     *
     *
     * @param src
     * @param srcPos
     * @param dest
     * @param destPos
     * @param length
     * @throws IndexOutOfBoundsException
     */
    public static void copy(final Object[] src, final int srcPos, final Object[] dest, final int destPos, final int length) throws IndexOutOfBoundsException {
        N.checkFromToIndex(srcPos, srcPos + length, len(src));
        N.checkFromToIndex(destPos, destPos + length, len(dest));

        if (N.isEmpty(src) && srcPos == 0 && length == 0) {
            return;
        }

        if (length < MIN_SIZE_FOR_COPY_ALL) {
            // for same array copy.
            if (destPos > srcPos) {
                for (int i = length - 1; i >= 0; i--) {
                    dest[destPos + i] = src[srcPos + i];
                }
            } else {
                for (int i = 0; i < length; i++) {
                    dest[destPos + i] = src[srcPos + i];
                }
            }
        } else {
            System.arraycopy(src, srcPos, dest, destPos, length);
        }
    }

    /**
     *
     *
     * @param src
     * @param srcPos
     * @param dest
     * @param destPos
     * @param length
     * @throws IndexOutOfBoundsException
     * @see System#arraycopy(Object, int, Object, int, int) is called
     */
    public static void copy(final Object src, final int srcPos, final Object dest, final int destPos, final int length) throws IndexOutOfBoundsException {
        N.checkFromToIndex(srcPos, srcPos + length, Array.getLength(src));
        N.checkFromToIndex(destPos, destPos + length, Array.getLength(dest));

        System.arraycopy(src, srcPos, dest, destPos, length);
    }

    /**
     *
     * @param original
     * @param newLength
     * @return
     * @see Arrays#copyOf(boolean[], int)
     */
    public static boolean[] copyOf(final boolean[] original, final int newLength) {
        if (newLength == original.length) {
            return original.clone();
        }

        final boolean[] copy = new boolean[newLength];

        if (notEmpty(original)) {
            copy(original, 0, copy, 0, Math.min(original.length, newLength));
        }

        return copy;
    }

    /**
     *
     * @param original
     * @param newLength
     * @return
     * @see Arrays#copyOf(char[], int)
     */
    public static char[] copyOf(final char[] original, final int newLength) {
        if (newLength == original.length) {
            return original.clone();
        }

        final char[] copy = new char[newLength];

        if (notEmpty(original)) {
            copy(original, 0, copy, 0, Math.min(original.length, newLength));
        }

        return copy;
    }

    /**
     *
     * @param original
     * @param newLength
     * @return
     * @see Arrays#copyOf(byte[], int)
     */
    public static byte[] copyOf(final byte[] original, final int newLength) {
        if (newLength == original.length) {
            return original.clone();
        }

        final byte[] copy = new byte[newLength];

        if (notEmpty(original)) {
            copy(original, 0, copy, 0, Math.min(original.length, newLength));
        }

        return copy;
    }

    /**
     *
     * @param original
     * @param newLength
     * @return
     * @see Arrays#copyOf(short[], int)
     */
    public static short[] copyOf(final short[] original, final int newLength) {
        if (newLength == original.length) {
            return original.clone();
        }

        final short[] copy = new short[newLength];

        if (notEmpty(original)) {
            copy(original, 0, copy, 0, Math.min(original.length, newLength));
        }

        return copy;
    }

    /**
     *
     * @param original
     * @param newLength
     * @return
     * @see Arrays#copyOf(int[], int)
     */
    public static int[] copyOf(final int[] original, final int newLength) {
        if (newLength == original.length) {
            return original.clone();
        }

        final int[] copy = new int[newLength];

        if (notEmpty(original)) {
            copy(original, 0, copy, 0, Math.min(original.length, newLength));
        }

        return copy;
    }

    /**
     *
     * @param original
     * @param newLength
     * @return
     * @see Arrays#copyOf(long[], int)
     */
    public static long[] copyOf(final long[] original, final int newLength) {
        if (newLength == original.length) {
            return original.clone();
        }

        final long[] copy = new long[newLength];

        if (notEmpty(original)) {
            copy(original, 0, copy, 0, Math.min(original.length, newLength));
        }

        return copy;
    }

    /**
     *
     * @param original
     * @param newLength
     * @return
     * @see Arrays#copyOf(float[], int)
     */
    public static float[] copyOf(final float[] original, final int newLength) {
        if (newLength == original.length) {
            return original.clone();
        }

        final float[] copy = new float[newLength];

        if (notEmpty(original)) {
            copy(original, 0, copy, 0, Math.min(original.length, newLength));
        }

        return copy;
    }

    /**
     *
     * @param original
     * @param newLength
     * @return
     * @see Arrays#copyOf(double[], int)
     */
    public static double[] copyOf(final double[] original, final int newLength) {
        if (newLength == original.length) {
            return original.clone();
        }

        final double[] copy = new double[newLength];

        if (notEmpty(original)) {
            copy(original, 0, copy, 0, Math.min(original.length, newLength));
        }

        return copy;
    }

    /**
     *
     * @param <T>
     * @param original
     * @param newLength
     * @return
     * @see Arrays#copyOf(Object[], int)
     */
    public static <T> T[] copyOf(final T[] original, final int newLength) {
        if (newLength == original.length) {
            return original.clone();
        }

        return (T[]) copyOf(original, newLength, original.getClass());
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param original
     * @param newLength
     * @param newType
     * @return
     * @see Arrays#copyOf(Object[], int, Class)
     */
    public static <T, U> T[] copyOf(final U[] original, final int newLength, final Class<? extends T[]> newType) {
        final T[] copy = Object[].class.equals(newType) ? (T[]) new Object[newLength] : (T[]) newArray(newType.getComponentType(), newLength);

        if (notEmpty(original)) {
            copy(original, 0, copy, 0, Math.min(original.length, newLength));
        }

        return copy;
    }

    /**
     * Copy of range.
     *
     * @param original
     * @param fromIndex
     * @param toIndex
     * @return
     * @see Arrays#copyOfRange(boolean[], int, int)
     */
    public static boolean[] copyOfRange(final boolean[] original, final int fromIndex, final int toIndex) {
        if (fromIndex == 0 && toIndex == original.length) {
            return original.clone();
        }

        final int newLength = toIndex - fromIndex;
        final boolean[] copy = new boolean[newLength];
        copy(original, fromIndex, copy, 0, Math.min(original.length - fromIndex, newLength));
        return copy;
    }

    /**
     * Copy all the elements in <code>original</code>, through <code>to</code>-<code>from</code>, by <code>step</code>.
     *
     * @param original
     * @param fromIndex
     * @param toIndex
     * @param step
     * @return
     * @throws IndexOutOfBoundsException
     * @see copyOfRange(int[], int, int, int)
     */
    public static boolean[] copyOfRange(final boolean[] original, int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (fromIndex == toIndex || fromIndex < toIndex != step > 0) {
            return EMPTY_BOOLEAN_ARRAY;
        }

        if (step == 1) {
            return copyOfRange(original, fromIndex, toIndex);
        }

        fromIndex = fromIndex > toIndex ? N.min(original.length - 1, fromIndex) : fromIndex;
        final int len = (toIndex - fromIndex) / step + ((toIndex - fromIndex) % step == 0 ? 0 : 1);
        final boolean[] copy = new boolean[len];

        for (int i = 0, j = fromIndex; i < len; i++, j += step) {
            copy[i] = original[j];
        }

        return copy;
    }

    /**
     * Copy of range.
     *
     * @param original
     * @param fromIndex
     * @param toIndex
     * @return
     * @see Arrays#copyOfRange(char[], int, int)
     */
    public static char[] copyOfRange(final char[] original, final int fromIndex, final int toIndex) {
        if (fromIndex == 0 && toIndex == original.length) {
            return original.clone();
        }

        final int newLength = toIndex - fromIndex;
        final char[] copy = new char[newLength];
        copy(original, fromIndex, copy, 0, Math.min(original.length - fromIndex, newLength));
        return copy;
    }

    /**
     * Copy all the elements in <code>original</code>, through <code>to</code>-<code>from</code>, by <code>step</code>.
     *
     * @param original
     * @param fromIndex
     * @param toIndex
     * @param step
     * @return
     * @throws IndexOutOfBoundsException
     * @see copyOfRange(int[], int, int, int)
     */
    public static char[] copyOfRange(final char[] original, int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (fromIndex == toIndex || fromIndex < toIndex != step > 0) {
            return EMPTY_CHAR_ARRAY;
        }

        if (step == 1) {
            return copyOfRange(original, fromIndex, toIndex);
        }

        fromIndex = fromIndex > toIndex ? N.min(original.length - 1, fromIndex) : fromIndex;
        final int len = (toIndex - fromIndex) / step + ((toIndex - fromIndex) % step == 0 ? 0 : 1);
        final char[] copy = new char[len];

        for (int i = 0, j = fromIndex; i < len; i++, j += step) {
            copy[i] = original[j];
        }

        return copy;
    }

    /**
     * Copy of range.
     *
     * @param original
     * @param fromIndex
     * @param toIndex
     * @return
     * @see Arrays#copyOfRange(byte[], int, int)
     */
    public static byte[] copyOfRange(final byte[] original, final int fromIndex, final int toIndex) {
        if (fromIndex == 0 && toIndex == original.length) {
            return original.clone();
        }

        final int newLength = toIndex - fromIndex;
        final byte[] copy = new byte[newLength];
        copy(original, fromIndex, copy, 0, Math.min(original.length - fromIndex, newLength));
        return copy;
    }

    /**
     * Copy all the elements in <code>original</code>, through <code>to</code>-<code>from</code>, by <code>step</code>.
     *
     * @param original
     * @param fromIndex
     * @param toIndex
     * @param step
     * @return
     * @throws IndexOutOfBoundsException
     * @see copyOfRange(int[], int, int, int)
     */
    public static byte[] copyOfRange(final byte[] original, int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (fromIndex == toIndex || fromIndex < toIndex != step > 0) {
            return EMPTY_BYTE_ARRAY;
        }

        if (step == 1) {
            return copyOfRange(original, fromIndex, toIndex);
        }

        fromIndex = fromIndex > toIndex ? N.min(original.length - 1, fromIndex) : fromIndex;
        final int len = (toIndex - fromIndex) / step + ((toIndex - fromIndex) % step == 0 ? 0 : 1);
        final byte[] copy = new byte[len];

        for (int i = 0, j = fromIndex; i < len; i++, j += step) {
            copy[i] = original[j];
        }

        return copy;
    }

    /**
     * Copy of range.
     *
     * @param original
     * @param fromIndex
     * @param toIndex
     * @return
     * @see Arrays#copyOfRange(short[], int, int)
     */
    public static short[] copyOfRange(final short[] original, final int fromIndex, final int toIndex) {
        if (fromIndex == 0 && toIndex == original.length) {
            return original.clone();
        }

        final int newLength = toIndex - fromIndex;
        final short[] copy = new short[newLength];
        copy(original, fromIndex, copy, 0, Math.min(original.length - fromIndex, newLength));
        return copy;
    }

    /**
     * Copy all the elements in <code>original</code>, through <code>to</code>-<code>from</code>, by <code>step</code>.
     *
     * @param original
     * @param fromIndex
     * @param toIndex
     * @param step
     * @return
     * @throws IndexOutOfBoundsException
     * @see copyOfRange(int[], int, int, int)
     */
    public static short[] copyOfRange(final short[] original, int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (fromIndex == toIndex || fromIndex < toIndex != step > 0) {
            return EMPTY_SHORT_ARRAY;
        }

        if (step == 1) {
            return copyOfRange(original, fromIndex, toIndex);
        }

        fromIndex = fromIndex > toIndex ? N.min(original.length - 1, fromIndex) : fromIndex;
        final int len = (toIndex - fromIndex) / step + ((toIndex - fromIndex) % step == 0 ? 0 : 1);
        final short[] copy = new short[len];

        for (int i = 0, j = fromIndex; i < len; i++, j += step) {
            copy[i] = original[j];
        }

        return copy;
    }

    /**
     * Copy of range.
     *
     * @param original
     * @param fromIndex
     * @param toIndex
     * @return
     * @see Arrays#copyOfRange(int[], int, int)
     */
    public static int[] copyOfRange(final int[] original, final int fromIndex, final int toIndex) {
        if (fromIndex == 0 && toIndex == original.length) {
            return original.clone();
        }

        final int newLength = toIndex - fromIndex;
        final int[] copy = new int[newLength];
        copy(original, fromIndex, copy, 0, Math.min(original.length - fromIndex, newLength));
        return copy;
    }

    /**
     * Copy all the elements in <code>original</code>, through <code>to</code>-<code>from</code>, by <code>step</code>.
     *
     * <pre>
     * <code>
     * int[] a = { 0, 1, 2, 3, 4, 5 };
     * N.copyOfRange(a, 1, 5, 1)); // [1, 2, 3, 4]
     * N.copyOfRange(a, 1, 5, 2); // [1, 3]
     *
     * N.copyOfRange(a, 5, 1, -1); // [5, 4, 3, 2]
     * N.copyOfRange(a, 5, 1, -2); // [5, 3]
     * N.copyOfRange(a, 5, -1, -1); // [5, 4, 3, 2, 1, 0]
     * N.copyOfRange(a, 6, -1, -1); // [5, 4, 3, 2, 1, 0]
     * </code>
     * </pre>
     *
     * @param original
     * @param fromIndex
     * @param toIndex
     * @param step
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static int[] copyOfRange(final int[] original, int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (fromIndex == toIndex || fromIndex < toIndex != step > 0) {
            return EMPTY_INT_ARRAY;
        }

        if (step == 1) {
            return copyOfRange(original, fromIndex, toIndex);
        }

        fromIndex = fromIndex > toIndex ? N.min(original.length - 1, fromIndex) : fromIndex;
        final int len = (toIndex - fromIndex) / step + ((toIndex - fromIndex) % step == 0 ? 0 : 1);
        final int[] copy = new int[len];

        for (int i = 0, j = fromIndex; i < len; i++, j += step) {
            copy[i] = original[j];
        }

        return copy;
    }

    /**
     * Copy of range.
     *
     * @param original
     * @param fromIndex
     * @param toIndex
     * @return
     * @see Arrays#copyOfRange(long[], int, int)
     */
    public static long[] copyOfRange(final long[] original, final int fromIndex, final int toIndex) {
        if (fromIndex == 0 && toIndex == original.length) {
            return original.clone();
        }

        final int newLength = toIndex - fromIndex;
        final long[] copy = new long[newLength];
        copy(original, fromIndex, copy, 0, Math.min(original.length - fromIndex, newLength));
        return copy;
    }

    /**
     * Copy all the elements in <code>original</code>, through <code>to</code>-<code>from</code>, by <code>step</code>.
     *
     * @param original
     * @param fromIndex
     * @param toIndex
     * @param step
     * @return
     * @throws IndexOutOfBoundsException
     * @see copyOfRange(int[], int, int, int)
     */
    public static long[] copyOfRange(final long[] original, int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (fromIndex == toIndex || fromIndex < toIndex != step > 0) {
            return EMPTY_LONG_ARRAY;
        }

        if (step == 1) {
            return copyOfRange(original, fromIndex, toIndex);
        }

        fromIndex = fromIndex > toIndex ? N.min(original.length - 1, fromIndex) : fromIndex;
        final int len = (toIndex - fromIndex) / step + ((toIndex - fromIndex) % step == 0 ? 0 : 1);
        final long[] copy = new long[len];

        for (int i = 0, j = fromIndex; i < len; i++, j += step) {
            copy[i] = original[j];
        }

        return copy;
    }

    /**
     * Copy of range.
     *
     * @param original
     * @param fromIndex
     * @param toIndex
     * @return
     * @see Arrays#copyOfRange(float[], int, int)
     */
    public static float[] copyOfRange(final float[] original, final int fromIndex, final int toIndex) {
        if (fromIndex == 0 && toIndex == original.length) {
            return original.clone();
        }

        final int newLength = toIndex - fromIndex;
        final float[] copy = new float[newLength];
        copy(original, fromIndex, copy, 0, Math.min(original.length - fromIndex, newLength));
        return copy;
    }

    /**
     * Copy all the elements in <code>original</code>, through <code>to</code>-<code>from</code>, by <code>step</code>.
     *
     * @param original
     * @param fromIndex
     * @param toIndex
     * @param step
     * @return
     * @throws IndexOutOfBoundsException
     * @see copyOfRange(int[], int, int, int)
     */
    public static float[] copyOfRange(final float[] original, int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (fromIndex == toIndex || fromIndex < toIndex != step > 0) {
            return EMPTY_FLOAT_ARRAY;
        }

        if (step == 1) {
            return copyOfRange(original, fromIndex, toIndex);
        }

        fromIndex = fromIndex > toIndex ? N.min(original.length - 1, fromIndex) : fromIndex;
        final int len = (toIndex - fromIndex) / step + ((toIndex - fromIndex) % step == 0 ? 0 : 1);
        final float[] copy = new float[len];

        for (int i = 0, j = fromIndex; i < len; i++, j += step) {
            copy[i] = original[j];
        }

        return copy;
    }

    /**
     * Copy of range.
     *
     * @param original
     * @param fromIndex
     * @param toIndex
     * @return
     * @see Arrays#copyOfRange(double[], int, int)
     */
    public static double[] copyOfRange(final double[] original, final int fromIndex, final int toIndex) {
        if (fromIndex == 0 && toIndex == original.length) {
            return original.clone();
        }

        final int newLength = toIndex - fromIndex;
        final double[] copy = new double[newLength];
        copy(original, fromIndex, copy, 0, Math.min(original.length - fromIndex, newLength));
        return copy;
    }

    /**
     * Copy all the elements in <code>original</code>, through <code>to</code>-<code>from</code>, by <code>step</code>.
     *
     * @param original
     * @param fromIndex
     * @param toIndex
     * @param step
     * @return
     * @throws IndexOutOfBoundsException
     * @see copyOfRange(int[], int, int, int)
     */
    public static double[] copyOfRange(final double[] original, int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (fromIndex == toIndex || fromIndex < toIndex != step > 0) {
            return EMPTY_DOUBLE_ARRAY;
        }

        if (step == 1) {
            return copyOfRange(original, fromIndex, toIndex);
        }

        fromIndex = fromIndex > toIndex ? N.min(original.length - 1, fromIndex) : fromIndex;
        final int len = (toIndex - fromIndex) / step + ((toIndex - fromIndex) % step == 0 ? 0 : 1);
        final double[] copy = new double[len];

        for (int i = 0, j = fromIndex; i < len; i++, j += step) {
            copy[i] = original[j];
        }

        return copy;
    }

    /**
     * Copy of range.
     *
     * @param <T>
     * @param original
     * @param fromIndex
     * @param toIndex
     * @return
     * @see Arrays#copyOfRange(Object[], int, int)
     */
    public static <T> T[] copyOfRange(final T[] original, final int fromIndex, final int toIndex) {
        if (fromIndex == 0 && toIndex == original.length) {
            return original.clone();
        }

        return copyOfRange(original, fromIndex, toIndex, (Class<T[]>) original.getClass());
    }

    /**
     * Copy all the elements in <code>original</code>, through <code>to</code>-<code>from</code>, by <code>step</code>.
     *
     * @param <T>
     * @param original
     * @param fromIndex
     * @param toIndex
     * @param step
     * @return
     */
    public static <T> T[] copyOfRange(final T[] original, final int fromIndex, final int toIndex, final int step) {
        return copyOfRange(original, fromIndex, toIndex, step, (Class<T[]>) original.getClass());
    }

    /**
     * {@link Arrays#copyOfRange(Object[], int, int, Class)}.
     *
     * @param <T>
     * @param <U>
     * @param original
     * @param fromIndex
     * @param toIndex
     * @param newType
     * @return
     */
    public static <T, U> T[] copyOfRange(final U[] original, final int fromIndex, final int toIndex, final Class<? extends T[]> newType) {
        final int newLength = toIndex - fromIndex;
        final T[] copy = Object[].class.equals(newType) ? (T[]) new Object[newLength] : (T[]) newArray(newType.getComponentType(), newLength);
        copy(original, fromIndex, copy, 0, Math.min(original.length - fromIndex, newLength));
        return copy;
    }

    /**
     * Copy all the elements in <code>original</code>, through <code>to</code>-<code>from</code>, by <code>step</code>.
     *
     * @param <T>
     * @param original
     * @param fromIndex
     * @param toIndex
     * @param step
     * @param newType
     * @return
     * @throws IndexOutOfBoundsException
     * @see copyOfRange(int[], int, int, int)
     */
    public static <T> T[] copyOfRange(final T[] original, int fromIndex, final int toIndex, final int step, final Class<? extends T[]> newType)
            throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (fromIndex == toIndex || fromIndex < toIndex != step > 0) {
            return Object[].class.equals(newType) ? (T[]) new Object[0] : (T[]) newArray(newType.getComponentType(), 0);
        }

        if (step == 1) {
            return copyOfRange(original, fromIndex, toIndex);
        }

        fromIndex = fromIndex > toIndex ? N.min(original.length - 1, fromIndex) : fromIndex;
        final int len = (toIndex - fromIndex) / step + ((toIndex - fromIndex) % step == 0 ? 0 : 1);
        final T[] copy = Object[].class.equals(newType) ? (T[]) new Object[len] : (T[]) newArray(newType.getComponentType(), len);

        for (int i = 0, j = fromIndex; i < len; i++, j += step) {
            copy[i] = original[j];
        }

        return copy;
    }

    /**
     * Copy of range.
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     * @see Arrays#copyOfRange(Object[], int, int)
     */
    public static <T> List<T> copyOfRange(final List<T> c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex, toIndex, c.size());

        final List<T> result = new ArrayList<>(toIndex - fromIndex);
        result.addAll(c.subList(fromIndex, toIndex));
        return result;
    }

    /**
     * Copy all the elements in <code>original</code>, through <code>to</code>-<code>from</code>, by <code>step</code>.
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param step
     * @return
     * @throws IndexOutOfBoundsException
     */
    @SuppressWarnings("deprecation")
    public static <T> List<T> copyOfRange(final List<T> c, int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex, c.size());

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (fromIndex == toIndex || fromIndex < toIndex != step > 0) {
            return new ArrayList<>();
        }

        if (step == 1) {
            return copyOfRange(c, fromIndex, toIndex);
        }

        fromIndex = fromIndex > toIndex ? N.min(c.size() - 1, fromIndex) : fromIndex;
        final int len = (toIndex - fromIndex) / step + ((toIndex - fromIndex) % step == 0 ? 0 : 1);
        List<T> result = null;

        if (c instanceof RandomAccess) {
            result = new ArrayList<>(len);

            for (int i = 0, j = fromIndex; i < len; i++, j += step) {
                result.add(c.get(j));
            }
        } else {
            final T[] a = (T[]) c.subList(fromIndex, toIndex).toArray();
            result = InternalUtil.createList(copyOfRange(a, 0, a.length, step));
        }

        return result;
    }

    /**
     * Copy of range.
     *
     * @param str
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static String copyOfRange(final String str, final int fromIndex, final int toIndex) {
        return str.substring(fromIndex, toIndex);
    }

    /**
     * Copy of range.
     *
     * @param str
     * @param fromIndex
     * @param toIndex
     * @param step
     * @return
     * @throws IndexOutOfBoundsException
     * @see copyOfRange(int[], int, int, int)
     */
    @SuppressWarnings("deprecation")
    public static String copyOfRange(final String str, int fromIndex, final int toIndex, final int step) throws IndexOutOfBoundsException {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex, str.length());

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (fromIndex == toIndex || fromIndex < toIndex != step > 0) {
            return Strings.EMPTY_STRING;
        }

        if (step == 1) {
            return copyOfRange(str, fromIndex, toIndex);
        }

        return InternalUtil.newString(copyOfRange(InternalUtil.getCharsForReadOnly(str), fromIndex, toIndex, step), true);
    }

    /**
     * Clone the original array. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static boolean[] clone(final boolean[] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        return original.clone();
    }

    /**
     * Clone the original array. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static char[] clone(final char[] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        return original.clone();
    }

    /**
     * Clone the original array. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static byte[] clone(final byte[] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        return original.clone();
    }

    /**
     * Clone the original array. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static short[] clone(final short[] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        return original.clone();
    }

    /**
     * Clone the original array. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static int[] clone(final int[] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        return original.clone();
    }

    /**
     * Clone the original array. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static long[] clone(final long[] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        return original.clone();
    }

    /**
     * Clone the original array. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static float[] clone(final float[] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        return original.clone();
    }

    /**
     * Clone the original array. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static double[] clone(final double[] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        return original.clone();
    }

    /**
     * Clone the original array. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param <T>
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static <T> T[] clone(final T[] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        return original.clone();
    }

    /**
     * Clone the original array and its sub arrays. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static boolean[][] clone(final boolean[][] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        final boolean[][] cp = original.clone();

        for (int i = 0, len = cp.length; i < len; i++) {
            cp[i] = clone(original[i]);
        }

        return cp;
    }

    /**
     * Clone the original array and its sub arrays. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static char[][] clone(final char[][] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        final char[][] cp = original.clone();

        for (int i = 0, len = cp.length; i < len; i++) {
            cp[i] = clone(original[i]);
        }

        return cp;
    }

    /**
     * Clone the original array and its sub arrays. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static byte[][] clone(final byte[][] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        final byte[][] cp = original.clone();

        for (int i = 0, len = cp.length; i < len; i++) {
            cp[i] = clone(original[i]);
        }

        return cp;
    }

    /**
     * Clone the original array and its sub arrays. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static short[][] clone(final short[][] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        final short[][] cp = original.clone();

        for (int i = 0, len = cp.length; i < len; i++) {
            cp[i] = clone(original[i]);
        }

        return cp;
    }

    /**
     * Clone the original array and its sub arrays. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static int[][] clone(final int[][] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        final int[][] cp = original.clone();

        for (int i = 0, len = cp.length; i < len; i++) {
            cp[i] = clone(original[i]);
        }

        return cp;
    }

    /**
     * Clone the original array and its sub arrays. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static long[][] clone(final long[][] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        final long[][] cp = original.clone();

        for (int i = 0, len = cp.length; i < len; i++) {
            cp[i] = clone(original[i]);
        }

        return cp;
    }

    /**
     * Clone the original array and its sub arrays. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static float[][] clone(final float[][] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        final float[][] cp = original.clone();

        for (int i = 0, len = cp.length; i < len; i++) {
            cp[i] = clone(original[i]);
        }

        return cp;
    }

    /**
     * Clone the original array and its sub arrays. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static double[][] clone(final double[][] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        final double[][] cp = original.clone();

        for (int i = 0, len = cp.length; i < len; i++) {
            cp[i] = clone(original[i]);
        }

        return cp;
    }

    /**
     * Clone the original array and its sub arrays. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param <T>
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static <T> T[][] clone(final T[][] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        final T[][] cp = original.clone();

        for (int i = 0, len = cp.length; i < len; i++) {
            cp[i] = clone(original[i]);
        }

        return cp;
    }

    /**
     * Clone the original array and its sub arrays. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static boolean[][][] clone(final boolean[][][] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        final boolean[][][] cp = original.clone();

        for (int i = 0, len = cp.length; i < len; i++) {
            cp[i] = clone(original[i]);
        }

        return cp;
    }

    /**
     * Clone the original array and its sub arrays. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static char[][][] clone(final char[][][] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        final char[][][] cp = original.clone();

        for (int i = 0, len = cp.length; i < len; i++) {
            cp[i] = clone(original[i]);
        }

        return cp;
    }

    /**
     * Clone the original array and its sub arrays. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static byte[][][] clone(final byte[][][] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        final byte[][][] cp = original.clone();

        for (int i = 0, len = cp.length; i < len; i++) {
            cp[i] = clone(original[i]);
        }

        return cp;
    }

    /**
     * Clone the original array and its sub arrays. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static short[][][] clone(final short[][][] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        final short[][][] cp = original.clone();

        for (int i = 0, len = cp.length; i < len; i++) {
            cp[i] = clone(original[i]);
        }

        return cp;
    }

    /**
     * Clone the original array and its sub arrays. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static int[][][] clone(final int[][][] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        final int[][][] cp = original.clone();

        for (int i = 0, len = cp.length; i < len; i++) {
            cp[i] = clone(original[i]);
        }

        return cp;
    }

    /**
     * Clone the original array and its sub arrays. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static long[][][] clone(final long[][][] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        final long[][][] cp = original.clone();

        for (int i = 0, len = cp.length; i < len; i++) {
            cp[i] = clone(original[i]);
        }

        return cp;
    }

    /**
     * Clone the original array and its sub arrays. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static float[][][] clone(final float[][][] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        final float[][][] cp = original.clone();

        for (int i = 0, len = cp.length; i < len; i++) {
            cp[i] = clone(original[i]);
        }

        return cp;
    }

    /**
     * Clone the original array and its sub arrays. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static double[][][] clone(final double[][][] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        final double[][][] cp = original.clone();

        for (int i = 0, len = cp.length; i < len; i++) {
            cp[i] = clone(original[i]);
        }

        return cp;
    }

    /**
     * Clone the original array and its sub arrays. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param <T>
     * @param original
     * @return {@code null} if {@code (original == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    public static <T> T[][][] clone(final T[][][] original) {
        if (original == null) {
            return null; // NOSONAR
        }

        final T[][][] cp = original.clone();

        for (int i = 0, len = cp.length; i < len; i++) {
            cp[i] = clone(original[i]);
        }

        return cp;
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param a
    //     * @param newType
    //     * @return
    //     */
    //    public static <T> T[] copy(Object[] a, Class<T[]> newType) {
    //        if (isEmpty(a)) {
    //            return newArray(newType.getComponentType(), 0);
    //        }
    //
    //        return copyOf(a, a.length, newType);
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param a
    //     * @param newType
    //     * @return
    //     */
    //    public static <T> T[][] copy(Object[][] a, Class<T[][]> newType) {
    //        final Class<T[]> componentType = (Class<T[]>) newType.getComponentType();
    //
    //        if (isEmpty(a)) {
    //            return newArray(componentType, 0);
    //        }
    //
    //        final int len = len(a);
    //        final T[][] result = newArray(componentType, len);
    //
    //        for (int i = 0; i < len; i++) {
    //            result[i] = copy(componentType, a[i]);
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param a
    //     * @param newType
    //     * @return
    //     */
    //    public static <T> T[][][] copy(Object[][][] a, Class<T[][][]> newType) {
    //        final Class<T[][]> componentType = (Class<T[][]>) newType.getComponentType();
    //
    //        if (isEmpty(a)) {
    //            return newArray(componentType, 0);
    //        }
    //
    //        final int len = len(a);
    //        final T[][][] result = newArray(componentType, len);
    //
    //        for (int i = 0; i < len; i++) {
    //            result[i] = copy(componentType, a[i]);
    //        }
    //
    //        return result;
    //    }

    /**
     *
     *
     * @param a
     * @return
     */
    public static boolean isSorted(final boolean[] a) {
        final int len = N.len(a);

        if (len < 2) {
            return true;
        } else if (Boolean.compare(a[len - 1], a[0]) < 0) {
            return false;
        }

        for (int i = 1; i < len; i++) {
            if (Boolean.compare(a[i], a[i - 1]) < 0) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static boolean isSorted(final boolean[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        final int len = N.len(a);

        N.checkFromToIndex(fromIndex, toIndex, len);

        if (toIndex - fromIndex < 2) {
            return true;
        } else if (Boolean.compare(a[toIndex - 1], a[fromIndex]) < 0) {
            return false;
        }

        for (int i = fromIndex + 1; i < toIndex; i++) {
            if (Boolean.compare(a[i], a[i - 1]) < 0) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param a
     * @return
     */
    public static boolean isSorted(final char[] a) {
        final int len = N.len(a);

        if (len < 2) {
            return true;
        } else if (a[len - 1] < a[0]) {
            return false;
        }

        for (int i = 1; i < len; i++) {
            if (a[i] < a[i - 1]) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static boolean isSorted(final char[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (toIndex - fromIndex < 2) {
            return true;
        } else if (a[toIndex - 1] < a[fromIndex]) {
            return false;
        }

        for (int i = fromIndex + 1; i < toIndex; i++) {
            if (a[i] < a[i - 1]) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param a
     * @return
     */
    public static boolean isSorted(final byte[] a) {
        final int len = N.len(a);

        if (len < 2) {
            return true;
        } else if (a[len - 1] < a[0]) {
            return false;
        }

        for (int i = 1; i < len; i++) {
            if (a[i] < a[i - 1]) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static boolean isSorted(final byte[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (toIndex - fromIndex < 2) {
            return true;
        } else if (a[toIndex - 1] < a[fromIndex]) {
            return false;
        }

        for (int i = fromIndex + 1; i < toIndex; i++) {
            if (a[i] < a[i - 1]) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param a
     * @return
     */
    public static boolean isSorted(final short[] a) {
        final int len = N.len(a);

        if (len < 2) {
            return true;
        } else if (a[len - 1] < a[0]) {
            return false;
        }

        for (int i = 1; i < len; i++) {
            if (a[i] < a[i - 1]) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static boolean isSorted(final short[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (toIndex - fromIndex < 2) {
            return true;
        } else if (a[toIndex - 1] < a[fromIndex]) {
            return false;
        }

        for (int i = fromIndex + 1; i < toIndex; i++) {
            if (a[i] < a[i - 1]) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param a
     * @return
     */
    public static boolean isSorted(final int[] a) {
        final int len = N.len(a);

        if (len < 2) {
            return true;
        } else if (a[len - 1] < a[0]) {
            return false;
        }

        for (int i = 1; i < len; i++) {
            if (a[i] < a[i - 1]) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static boolean isSorted(final int[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (toIndex - fromIndex < 2) {
            return true;
        } else if (a[toIndex - 1] < a[fromIndex]) {
            return false;
        }

        for (int i = fromIndex + 1; i < toIndex; i++) {
            if (a[i] < a[i - 1]) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param a
     * @return
     */
    public static boolean isSorted(final long[] a) {
        final int len = N.len(a);

        if (len < 2) {
            return true;
        } else if (a[len - 1] < a[0]) {
            return false;
        }

        for (int i = 1; i < len; i++) {
            if (a[i] < a[i - 1]) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static boolean isSorted(final long[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (toIndex - fromIndex < 2) {
            return true;
        } else if (a[toIndex - 1] < a[fromIndex]) {
            return false;
        }

        for (int i = fromIndex + 1; i < toIndex; i++) {
            if (a[i] < a[i - 1]) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param a
     * @return
     */
    public static boolean isSorted(final float[] a) {
        final int len = N.len(a);

        if (len < 2) {
            return true;
        } else if (Float.compare(a[len - 1], a[0]) < 0) {
            return false;
        }

        for (int i = 1; i < len; i++) {
            if (Float.compare(a[i], a[i - 1]) < 0) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static boolean isSorted(final float[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (toIndex - fromIndex < 2) {
            return true;
        } else if (Float.compare(a[toIndex - 1], a[fromIndex]) < 0) {
            return false;
        }

        for (int i = fromIndex + 1; i < toIndex; i++) {
            if (Float.compare(a[i], a[i - 1]) < 0) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param a
     * @return
     */
    public static boolean isSorted(final double[] a) {
        final int len = N.len(a);

        if (len < 2) {
            return true;
        } else if (Double.compare(a[len - 1], a[0]) < 0) {
            return false;
        }

        for (int i = 1; i < len; i++) {
            if (Double.compare(a[i], a[i - 1]) < 0) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static boolean isSorted(final double[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (toIndex - fromIndex < 2) {
            return true;
        } else if (Double.compare(a[toIndex - 1], a[fromIndex]) < 0) {
            return false;
        }

        for (int i = fromIndex + 1; i < toIndex; i++) {
            if (Double.compare(a[i], a[i - 1]) < 0) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T extends Comparable<? super T>> boolean isSorted(final T[] a) {
        final int len = N.len(a);

        if (len < 2) {
            return true;
        } else if (N.compare(a[len - 1], a[0]) < 0) {
            return false;
        }

        for (int i = 1; i < len; i++) {
            if (N.compare(a[i], a[i - 1]) < 0) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static <T extends Comparable<? super T>> boolean isSorted(final T[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (toIndex - fromIndex < 2) {
            return true;
        } else if (N.compare(a[toIndex - 1], a[fromIndex]) < 0) {
            return false;
        }

        for (int i = fromIndex + 1; i < toIndex; i++) {
            if (N.compare(a[i], a[i - 1]) < 0) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param <T>
     * @param a
     * @param cmp
     * @return
     */
    public static <T> boolean isSorted(final T[] a, Comparator<? super T> cmp) {
        cmp = checkComparator(cmp);

        final int len = N.len(a);

        if (len < 2) {
            return true;
        } else if (cmp.compare(a[len - 1], a[0]) < 0) {
            return false;
        }

        for (int i = 1; i < len; i++) {
            if (cmp.compare(a[i], a[i - 1]) < 0) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param cmp
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static <T> boolean isSorted(final T[] a, final int fromIndex, final int toIndex, Comparator<? super T> cmp) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        cmp = checkComparator(cmp);

        if (toIndex - fromIndex < 2) {
            return true;
        } else if (cmp.compare(a[toIndex - 1], a[fromIndex]) < 0) {
            return false;
        }

        for (int i = fromIndex + 1; i < toIndex; i++) {
            if (cmp.compare(a[i], a[i - 1]) < 0) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Comparable<? super T>> boolean isSorted(final Collection<? extends T> c) {
        if (N.size(c) < 2) {
            return true;
        }

        final Iterator<? extends T> iter = c.iterator();
        T prev = iter.next();
        T cur = null;

        while (iter.hasNext()) {
            cur = iter.next();

            if (N.compare(cur, prev) < 0) {
                return false;
            }

            prev = cur;
        }

        return true;
    }

    /**
     *
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static <T extends Comparable<? super T>> boolean isSorted(final Collection<? extends T> c, final int fromIndex, final int toIndex)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.size(c));

        if (toIndex - fromIndex < 2) {
            return true;
        }

        final Iterator<? extends T> iter = c.iterator();
        int cursor = 0;

        while (cursor < fromIndex) {
            iter.next();
            cursor++;
        }

        cursor++;
        T prev = iter.next();
        T cur = null;

        while (cursor < toIndex) {
            cur = iter.next();

            if (N.compare(cur, prev) < 0) {
                return false;
            }

            prev = cur;
            cursor++;
        }

        return true;
    }

    /**
     *
     *
     * @param <T>
     * @param c
     * @param cmp
     * @return
     */
    public static <T> boolean isSorted(final Collection<? extends T> c, Comparator<? super T> cmp) {
        if (N.size(c) < 2) {
            return true;
        }

        cmp = checkComparator(cmp);

        final Iterator<? extends T> iter = c.iterator();
        T prev = iter.next();
        T cur = null;

        while (iter.hasNext()) {
            cur = iter.next();

            if (cmp.compare(cur, prev) < 0) {
                return false;
            }

            prev = cur;
        }

        return true;
    }

    /**
     *
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param cmp
     * @return
     * @throws IndexOutOfBoundsException
     */
    public static <T> boolean isSorted(final Collection<? extends T> c, final int fromIndex, final int toIndex, Comparator<? super T> cmp)
            throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.size(c));

        if (toIndex - fromIndex < 2) {
            return true;
        }

        cmp = checkComparator(cmp);

        final Iterator<? extends T> iter = c.iterator();
        int cursor = 0;

        while (cursor < fromIndex) {
            iter.next();
            cursor++;
        }

        cursor++;
        T prev = iter.next();
        T cur = null;

        while (cursor < toIndex) {
            cur = iter.next();

            if (cmp.compare(cur, prev) < 0) {
                return false;
            }

            prev = cur;
            cursor++;
        }

        return true;
    }

    /**
     *
     * @param a
     */
    public static void sort(final boolean[] a) {
        if (N.isEmpty(a)) {
            return;
        }

        int numOfFalse = 0;
        for (boolean element : a) {
            if (!element) {
                numOfFalse++;
            }
        }

        N.fill(a, 0, numOfFalse, false);
        N.fill(a, numOfFalse, a.length, true);
    }

    /**
     *
     * @param a
     */
    public static void sort(final char[] a) {
        if (N.isEmpty(a)) {
            return;
        }

        Arrays.sort(a);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void sort(final char[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return;
        }

        Arrays.sort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void sort(final byte[] a) {
        if (N.isEmpty(a)) {
            return;
        }

        Arrays.sort(a);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void sort(final byte[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return;
        }

        Arrays.sort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void sort(final short[] a) {
        if (N.isEmpty(a)) {
            return;
        }

        Arrays.sort(a);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void sort(final short[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return;
        }

        Arrays.sort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void sort(final int[] a) {
        if (N.isEmpty(a)) {
            return;
        }

        Arrays.sort(a);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void sort(final int[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return;
        }

        Arrays.sort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void sort(final long[] a) {
        if (N.isEmpty(a)) {
            return;
        }

        Arrays.sort(a);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void sort(final long[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return;
        }

        Arrays.sort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void sort(final float[] a) {
        if (N.isEmpty(a)) {
            return;
        }

        Arrays.sort(a);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void sort(final float[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return;
        }

        Arrays.sort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void sort(final double[] a) {
        if (N.isEmpty(a)) {
            return;
        }

        Arrays.sort(a);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void sort(final double[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return;
        }

        Arrays.sort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void sort(final Object[] a) {
        if (N.isEmpty(a)) {
            return;
        }

        Arrays.sort(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void sort(final Object[] a, final int fromIndex, final int toIndex) {
        sort(a, fromIndex, toIndex, NATURAL_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param cmp
     */
    public static <T> void sort(final T[] a, final Comparator<? super T> cmp) {
        if (N.isEmpty(a)) {
            return;
        }

        sort(a, 0, a.length, cmp);
    }

    /**
     *
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param cmp
     * @throws IndexOutOfBoundsException
     */
    public static <T> void sort(final T[] a, final int fromIndex, final int toIndex, final Comparator<? super T> cmp) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return;
        }

        Arrays.sort(a, fromIndex, toIndex, cmp);
    }

    /**
     *
     * @param <T>
     * @param list
     */
    public static <T extends Comparable<? super T>> void sort(final List<? extends T> list) {
        if (N.isEmpty(list)) {
            return;
        }

        sort(list, 0, list.size(), NATURAL_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param list
     * @param fromIndex
     * @param toIndex
     */
    public static <T extends Comparable<? super T>> void sort(final List<? extends T> list, final int fromIndex, final int toIndex) {
        if (N.isEmpty(list)) {
            return;
        }

        sort(list, fromIndex, toIndex, NATURAL_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param list
     * @param cmp
     */
    public static <T> void sort(final List<? extends T> list, final Comparator<? super T> cmp) {
        if (N.isEmpty(list)) {
            return;
        }

        sort(list, 0, list.size(), cmp);
    }

    /**
     *
     * @param <T>
     * @param list
     * @param fromIndex
     * @param toIndex
     * @param cmp
     */
    public static <T> void sort(final List<? extends T> list, final int fromIndex, final int toIndex, final Comparator<? super T> cmp) {
        if ((N.isEmpty(list) && fromIndex == 0 && toIndex == 0) || fromIndex == toIndex) {
            return;
        }

        @SuppressWarnings("deprecation")
        final T[] a = (T[]) InternalUtil.getInternalArray(list);

        if (a != null) {
            sort(a, fromIndex, toIndex, cmp);

            return;
        }

        final T[] array = (T[]) list.toArray();
        Arrays.sort(array, fromIndex, toIndex, cmp);
        final ListIterator<T> i = (ListIterator<T>) list.listIterator();

        for (T element : array) {
            i.next();
            i.set(element);
        }
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param a
     * @param keyMapper
     */
    public static <T, U extends Comparable<? super U>> void sortBy(final T[] a, final Function<? super T, ? extends U> keyMapper) {
        sort(a, Comparators.comparingBy(keyMapper));
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param list
     * @param keyMapper
     */
    public static <T, U extends Comparable<? super U>> void sortBy(final List<? extends T> list, final Function<? super T, ? extends U> keyMapper) {
        sort(list, Comparators.comparingBy(keyMapper));
    }

    /**
     * Sort by int.
     *
     * @param <T>
     * @param a
     * @param keyMapper
     */
    public static <T> void sortByInt(final T[] a, final ToIntFunction<? super T> keyMapper) {
        sort(a, Comparators.comparingInt(keyMapper));
    }

    /**
     * Sort by int.
     *
     * @param <T>
     * @param list
     * @param keyMapper
     */
    public static <T> void sortByInt(final List<? extends T> list, final ToIntFunction<? super T> keyMapper) {
        sort(list, Comparators.comparingInt(keyMapper));
    }

    /**
     * Sort by long.
     *
     * @param <T>
     * @param a
     * @param keyMapper
     */
    public static <T> void sortByLong(final T[] a, final ToLongFunction<? super T> keyMapper) {
        sort(a, Comparators.comparingLong(keyMapper));
    }

    /**
     * Sort by long.
     *
     * @param <T>
     * @param list
     * @param keyMapper
     */
    public static <T> void sortByLong(final List<? extends T> list, final ToLongFunction<? super T> keyMapper) {
        sort(list, Comparators.comparingLong(keyMapper));
    }

    /**
     * Sort by float.
     *
     * @param <T>
     * @param a
     * @param keyMapper
     */
    public static <T> void sortByFloat(final T[] a, final ToFloatFunction<? super T> keyMapper) {
        sort(a, Comparators.comparingFloat(keyMapper));
    }

    /**
     * Sort by float.
     *
     * @param <T>
     * @param list
     * @param keyMapper
     */
    public static <T> void sortByFloat(final List<? extends T> list, final ToFloatFunction<? super T> keyMapper) {
        sort(list, Comparators.comparingFloat(keyMapper));
    }

    /**
     * Sort by double.
     *
     * @param <T>
     * @param a
     * @param keyMapper
     */
    public static <T> void sortByDouble(final T[] a, final ToDoubleFunction<? super T> keyMapper) {
        sort(a, Comparators.comparingDouble(keyMapper));
    }

    /**
     * Sort by double.
     *
     * @param <T>
     * @param list
     * @param keyMapper
     */
    public static <T> void sortByDouble(final List<? extends T> list, final ToDoubleFunction<? super T> keyMapper) {
        sort(list, Comparators.comparingDouble(keyMapper));
    }

    /**
     *
     * @param a
     */
    public static void parallelSort(final char[] a) {
        if (N.isEmpty(a)) {
            return;
        }

        parallelSort(a, 0, a.length);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void parallelSort(final char[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return;
        }

        Arrays.parallelSort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void parallelSort(final byte[] a) {
        if (N.isEmpty(a)) {
            return;
        }

        parallelSort(a, 0, a.length);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void parallelSort(final byte[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return;
        }

        Arrays.parallelSort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void parallelSort(final short[] a) {
        if (N.isEmpty(a)) {
            return;
        }

        parallelSort(a, 0, a.length);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void parallelSort(final short[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return;
        }

        Arrays.parallelSort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void parallelSort(final int[] a) {
        if (N.isEmpty(a)) {
            return;
        }

        parallelSort(a, 0, a.length);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void parallelSort(final int[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return;
        }

        Arrays.parallelSort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void parallelSort(final long[] a) {
        if (N.isEmpty(a)) {
            return;
        }

        parallelSort(a, 0, a.length);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void parallelSort(final long[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return;
        }

        Arrays.parallelSort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void parallelSort(final float[] a) {
        if (N.isEmpty(a)) {
            return;
        }

        parallelSort(a, 0, a.length);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void parallelSort(final float[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return;
        }

        Arrays.parallelSort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void parallelSort(final double[] a) {
        if (N.isEmpty(a)) {
            return;
        }

        parallelSort(a, 0, a.length);
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static void parallelSort(final double[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return;
        }

        Arrays.parallelSort(a, fromIndex, toIndex);
    }

    /**
     *
     *
     * @param <T>
     * @param a
     */
    public static <T extends Comparable<? super T>> void parallelSort(T[] a) {
        if (N.isEmpty(a)) {
            return;
        }

        parallelSort(a, 0, a.length);
    }

    /**
     *
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @throws IndexOutOfBoundsException
     */
    public static <T extends Comparable<? super T>> void parallelSort(T[] a, int fromIndex, int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return;
        }

        Arrays.parallelSort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param cmp
     */
    public static <T> void parallelSort(final T[] a, final Comparator<? super T> cmp) {
        if (N.isEmpty(a)) {
            return;
        }

        parallelSort(a, 0, a.length, cmp);
    }

    /**
     *
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param cmp
     * @throws IndexOutOfBoundsException
     */
    public static <T> void parallelSort(final T[] a, final int fromIndex, final int toIndex, final Comparator<? super T> cmp) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return;
        }

        Arrays.parallelSort(a, fromIndex, toIndex, cmp);
    }

    /**
     *
     * @param <T>
     * @param list
     */
    public static <T extends Comparable<? super T>> void parallelSort(final List<? extends T> list) {
        if (N.isEmpty(list)) {
            return;
        }

        parallelSort(list, 0, list.size());
    }

    /**
     *
     * @param <T>
     * @param list
     * @param fromIndex
     * @param toIndex
     */
    public static <T extends Comparable<? super T>> void parallelSort(final List<? extends T> list, final int fromIndex, final int toIndex) {
        parallelSort(list, fromIndex, toIndex, NATURAL_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param list
     * @param cmp
     */
    public static <T> void parallelSort(final List<? extends T> list, final Comparator<? super T> cmp) {
        if (N.isEmpty(list)) {
            return;
        }

        parallelSort(list, 0, list.size(), cmp);
    }

    /**
     *
     * @param <T>
     * @param list
     * @param fromIndex
     * @param toIndex
     * @param cmp
     */
    public static <T> void parallelSort(final List<? extends T> list, final int fromIndex, final int toIndex, final Comparator<? super T> cmp) {
        if ((N.isEmpty(list) && fromIndex == 0 && toIndex == 0) || fromIndex == toIndex) {
            return;
        }

        @SuppressWarnings("deprecation")
        final T[] a = (T[]) InternalUtil.getInternalArray(list);

        if (a != null) {
            parallelSort(a, fromIndex, toIndex, cmp);

            return;
        }

        final T[] array = (T[]) list.toArray();

        parallelSort(array, fromIndex, toIndex, cmp);

        final ListIterator<T> it = (ListIterator<T>) list.listIterator();

        for (T element : array) {
            it.next();

            it.set(element);
        }
    }

    /**
     * Parallel sort by.
     *
     * @param <T>
     * @param <U>
     * @param a
     * @param keyMapper
     */
    public static <T, U extends Comparable<? super U>> void parallelSortBy(final T[] a, final Function<? super T, ? extends U> keyMapper) {
        parallelSort(a, Comparators.comparingBy(keyMapper));
    }

    /**
     * Parallel sort by.
     *
     * @param <T>
     * @param <U>
     * @param list
     * @param keyMapper
     */
    public static <T, U extends Comparable<? super U>> void parallelSortBy(final List<? extends T> list, final Function<? super T, ? extends U> keyMapper) {
        parallelSort(list, Comparators.comparingBy(keyMapper));
    }

    /**
     * Parallel sort by int.
     *
     * @param <T>
     * @param a
     * @param keyMapper
     */
    public static <T> void parallelSortByInt(final T[] a, final ToIntFunction<? super T> keyMapper) {
        parallelSort(a, Comparators.comparingInt(keyMapper));
    }

    /**
     * Parallel sort by int.
     *
     * @param <T>
     * @param list
     * @param keyMapper
     */
    public static <T> void parallelSortByInt(final List<? extends T> list, final ToIntFunction<? super T> keyMapper) {
        parallelSort(list, Comparators.comparingInt(keyMapper));
    }

    /**
     * Parallel sort by long.
     *
     * @param <T>
     * @param a
     * @param keyMapper
     */
    public static <T> void parallelSortByLong(final T[] a, final ToLongFunction<? super T> keyMapper) {
        parallelSort(a, Comparators.comparingLong(keyMapper));
    }

    /**
     * Parallel sort by long.
     *
     * @param <T>
     * @param list
     * @param keyMapper
     */
    public static <T> void parallelSortByLong(final List<? extends T> list, final ToLongFunction<? super T> keyMapper) {
        parallelSort(list, Comparators.comparingLong(keyMapper));
    }

    /**
     * Parallel sort by float.
     *
     * @param <T>
     * @param a
     * @param keyMapper
     */
    public static <T> void parallelSortByFloat(final T[] a, final ToFloatFunction<? super T> keyMapper) {
        parallelSort(a, Comparators.comparingFloat(keyMapper));
    }

    /**
     * Parallel sort by float.
     *
     * @param <T>
     * @param list
     * @param keyMapper
     */
    public static <T> void parallelSortByFloat(final List<? extends T> list, final ToFloatFunction<? super T> keyMapper) {
        parallelSort(list, Comparators.comparingFloat(keyMapper));
    }

    /**
     * Parallel sort by double.
     *
     * @param <T>
     * @param a
     * @param keyMapper
     */
    public static <T> void parallelSortByDouble(final T[] a, final ToDoubleFunction<? super T> keyMapper) {
        parallelSort(a, Comparators.comparingDouble(keyMapper));
    }

    /**
     * Parallel sort by double.
     *
     * @param <T>
     * @param list
     * @param keyMapper
     */
    public static <T> void parallelSortByDouble(final List<? extends T> list, final ToDoubleFunction<? super T> keyMapper) {
        parallelSort(list, Comparators.comparingDouble(keyMapper));
    }

    /**
     *
     * @param a
     */
    public static void reverseSort(final boolean[] a) {
        if (N.isEmpty(a)) {
            return;
        }

        int numOfTrue = 0;
        for (boolean element : a) {
            if (element) {
                numOfTrue++;
            }
        }

        N.fill(a, 0, numOfTrue, true);
        N.fill(a, numOfTrue, a.length, false);
    }

    /**
     *
     * @param a
     */
    public static void reverseSort(final char[] a) {
        sort(a);
        reverse(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void reverseSort(final char[] a, final int fromIndex, final int toIndex) {
        sort(a, fromIndex, toIndex);
        reverse(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void reverseSort(final byte[] a) {
        sort(a);
        reverse(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void reverseSort(final byte[] a, final int fromIndex, final int toIndex) {
        sort(a, fromIndex, toIndex);
        reverse(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void reverseSort(final short[] a) {
        sort(a);
        reverse(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void reverseSort(final short[] a, final int fromIndex, final int toIndex) {
        sort(a, fromIndex, toIndex);
        reverse(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void reverseSort(final int[] a) {
        sort(a);
        reverse(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void reverseSort(final int[] a, final int fromIndex, final int toIndex) {
        sort(a, fromIndex, toIndex);
        reverse(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void reverseSort(final long[] a) {
        sort(a);
        reverse(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void reverseSort(final long[] a, final int fromIndex, final int toIndex) {
        sort(a, fromIndex, toIndex);
        reverse(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void reverseSort(final float[] a) {
        sort(a);
        reverse(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void reverseSort(final float[] a, final int fromIndex, final int toIndex) {
        sort(a, fromIndex, toIndex);
        reverse(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void reverseSort(final double[] a) {
        sort(a);
        reverse(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void reverseSort(final double[] a, final int fromIndex, final int toIndex) {
        sort(a, fromIndex, toIndex);
        reverse(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void reverseSort(final Object[] a) {
        sort(a, REVERSED_COMPARATOR);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void reverseSort(final Object[] a, final int fromIndex, final int toIndex) {
        sort(a, fromIndex, toIndex, REVERSED_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param list
     */
    public static <T extends Comparable<? super T>> void reverseSort(final List<? extends T> list) {
        sort(list, REVERSED_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param list
     * @param fromIndex
     * @param toIndex
     */
    public static <T extends Comparable<? super T>> void reverseSort(final List<? extends T> list, final int fromIndex, final int toIndex) {
        sort(list, fromIndex, toIndex, REVERSED_COMPARATOR);
    }

    /**
     * Reverse sort by.
     *
     * @param <T>
     * @param <U>
     * @param a
     * @param keyMapper
     */
    public static <T, U extends Comparable<? super U>> void reverseSortBy(final T[] a, final Function<? super T, ? extends U> keyMapper) {
        sort(a, Comparators.reversedComparingBy(keyMapper));
    }

    /**
     * Reverse sort by.
     *
     * @param <T>
     * @param <U>
     * @param list
     * @param keyMapper
     */
    public static <T, U extends Comparable<? super U>> void reverseSortBy(final List<? extends T> list, final Function<? super T, ? extends U> keyMapper) {
        sort(list, Comparators.reversedComparingBy(keyMapper));
    }

    /**
     *
     * @param <T>
     * @param a
     * @param keyMapper
     */
    public static <T> void reverseSortByInt(final T[] a, final ToIntFunction<? super T> keyMapper) {
        sort(a, Comparators.reversedComparingInt(keyMapper));
    }

    /**
     *
     * @param <T>
     * @param list
     * @param keyMapper
     */
    public static <T> void reverseSortByInt(final List<? extends T> list, final ToIntFunction<? super T> keyMapper) {
        sort(list, Comparators.reversedComparingInt(keyMapper));
    }

    /**
     *
     * @param <T>
     * @param a
     * @param keyMapper
     */
    public static <T> void reverseSortByLong(final T[] a, final ToLongFunction<? super T> keyMapper) {
        sort(a, Comparators.reversedComparingLong(keyMapper));
    }

    /**
     *
     * @param <T>
     * @param list
     * @param keyMapper
     */
    public static <T> void reverseSortByLong(final List<? extends T> list, final ToLongFunction<? super T> keyMapper) {
        sort(list, Comparators.reversedComparingLong(keyMapper));
    }

    /**
     *
     * @param <T>
     * @param a
     * @param keyMapper
     */
    public static <T> void reverseSortByFloat(final T[] a, final ToFloatFunction<? super T> keyMapper) {
        sort(a, Comparators.reversedComparingFloat(keyMapper));
    }

    /**
     *
     * @param <T>
     * @param list
     * @param keyMapper
     */
    public static <T> void reverseSortByFloat(final List<? extends T> list, final ToFloatFunction<? super T> keyMapper) {
        sort(list, Comparators.reversedComparingFloat(keyMapper));
    }

    /**
     *
     * @param <T>
     * @param a
     * @param keyMapper
     */
    public static <T> void reverseSortByDouble(final T[] a, final ToDoubleFunction<? super T> keyMapper) {
        sort(a, Comparators.reversedComparingDouble(keyMapper));
    }

    /**
     *
     *
     * @param <T>
     * @param list
     * @param keyMapper
     * @throws IndexOutOfBoundsException
     */
    public static <T> void reverseSortByDouble(final List<? extends T> list, final ToDoubleFunction<? super T> keyMapper) throws IndexOutOfBoundsException {
        sort(list, Comparators.reversedComparingDouble(keyMapper));
    }

    //    /**
    //     *
    //     * @param a
    //     */
    //    public static void bucketSort(final int[] a) {
    //        if (N.isEmpty(a)) {
    //            return;
    //        }
    //
    //        bucketSort(a, 0, a.length);
    //    }
    //
    //    /**
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     */
    //    public static void bucketSort(final int[] a, final int fromIndex, final int toIndex) {
    //        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);
    //
    //        if (N.isEmpty(a) || fromIndex == toIndex) {
    //            return;
    //        }
    //
    //        if (toIndex - fromIndex < 32) {
    //            sort(a, fromIndex, toIndex);
    //            return;
    //        }
    //
    //        final Multiset<Integer> multiset = new Multiset<>();
    //
    //        for (int i = fromIndex; i < toIndex; i++) {
    //            multiset.add(a[i]);
    //        }
    //
    //        final Map<Integer, Integer> m = multiset.toMapSortedBy((a1, b) -> N.compare(a1.getKey().intValue(), a1.getKey().intValue()));
    //        int idx = fromIndex;
    //
    //        for (Map.Entry<Integer, Integer> entry : m.entrySet()) {
    //            N.fill(a, idx, idx + entry.getValue(), entry.getKey());
    //            idx += entry.getValue();
    //        }
    //    }
    //
    //    /**
    //     *
    //     * @param a
    //     */
    //    public static void bucketSort(final long[] a) {
    //        if (N.isEmpty(a)) {
    //            return;
    //        }
    //
    //        bucketSort(a, 0, a.length);
    //    }
    //
    //    /**
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     */
    //    public static void bucketSort(final long[] a, final int fromIndex, final int toIndex) {
    //        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);
    //
    //        if (N.isEmpty(a) || fromIndex == toIndex) {
    //            return;
    //        }
    //
    //        if (toIndex - fromIndex < 32) {
    //            sort(a, fromIndex, toIndex);
    //            return;
    //        }
    //
    //        final Multiset<Long> multiset = new Multiset<>();
    //
    //        for (int i = fromIndex; i < toIndex; i++) {
    //            multiset.add(a[i]);
    //        }
    //
    //        final Map<Long, Integer> m = multiset.toMapSortedBy((a1, b) -> N.compare(a1.getKey().longValue(), a1.getKey().longValue()));
    //
    //        int idx = fromIndex;
    //
    //        for (Map.Entry<Long, Integer> entry : m.entrySet()) {
    //            N.fill(a, idx, idx + entry.getValue(), entry.getKey());
    //            idx += entry.getValue();
    //        }
    //    }
    //
    //    /**
    //     *
    //     * @param a
    //     */
    //    public static void bucketSort(final float[] a) {
    //        if (N.isEmpty(a)) {
    //            return;
    //        }
    //
    //        bucketSort(a, 0, a.length);
    //    }
    //
    //    /**
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     */
    //    public static void bucketSort(final float[] a, final int fromIndex, final int toIndex) {
    //        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);
    //
    //        if (N.isEmpty(a) || fromIndex == toIndex) {
    //            return;
    //        }
    //
    //        if (toIndex - fromIndex < 32) {
    //            sort(a, fromIndex, toIndex);
    //            return;
    //        }
    //
    //        final Multiset<Float> multiset = new Multiset<>();
    //
    //        for (int i = fromIndex; i < toIndex; i++) {
    //            multiset.add(a[i]);
    //        }
    //
    //        final Map<Float, Integer> m = multiset.toMapSortedBy((a1, b) -> N.compare(a1.getKey(), a1.getKey()));
    //        int idx = fromIndex;
    //
    //        for (Map.Entry<Float, Integer> entry : m.entrySet()) {
    //            N.fill(a, idx, idx + entry.getValue(), entry.getKey());
    //            idx += entry.getValue();
    //        }
    //    }
    //
    //    /**
    //     *
    //     * @param a
    //     */
    //    public static void bucketSort(final double[] a) {
    //        if (N.isEmpty(a)) {
    //            return;
    //        }
    //
    //        bucketSort(a, 0, a.length);
    //    }
    //
    //    /**
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     */
    //    public static void bucketSort(final double[] a, final int fromIndex, final int toIndex) {
    //        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);
    //
    //        if (N.isEmpty(a) || fromIndex == toIndex) {
    //            return;
    //        }
    //
    //        if (toIndex - fromIndex < 32) {
    //            sort(a, fromIndex, toIndex);
    //            return;
    //        }
    //
    //        final Multiset<Double> multiset = new Multiset<>();
    //
    //        for (int i = fromIndex; i < toIndex; i++) {
    //            multiset.add(a[i]);
    //        }
    //
    //        final Map<Double, Integer> m = multiset.toMapSortedBy((a1, b) -> N.compare(a1.getKey(), a1.getKey()));
    //        int idx = fromIndex;
    //
    //        for (Map.Entry<Double, Integer> entry : m.entrySet()) {
    //            N.fill(a, idx, idx + entry.getValue(), entry.getKey());
    //            idx += entry.getValue();
    //        }
    //    }
    //
    //    /**
    //     * Note: All the objects with same value will be replaced by first element with the same value.
    //     *
    //     * @param a
    //     */
    //    public static void bucketSort(final Object[] a) {
    //        if (N.isEmpty(a)) {
    //            return;
    //        }
    //
    //        bucketSort(a, 0, a.length);
    //    }
    //
    //    /**
    //     * Note: All the objects with same value will be replaced by first element with the same value.
    //     *
    //     * @param a the elements in the array must implements the <code>Comparable</code> interface.
    //     * @param fromIndex
    //     * @param toIndex
    //     */
    //    public static void bucketSort(final Object[] a, final int fromIndex, final int toIndex) {
    //        bucketSort(a, fromIndex, toIndex, NATURAL_COMPARATOR);
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param a
    //     * @param cmp
    //     */
    //    public static <T> void bucketSort(final T[] a, final Comparator<? super T> cmp) {
    //        if (N.isEmpty(a)) {
    //            return;
    //        }
    //
    //        bucketSort(a, 0, a.length, cmp);
    //    }
    //
    //    /**
    //     * Note: All the objects with same value will be replaced by first element with the same value.
    //     *
    //     * @param <T>
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param cmp
    //     */
    //    public static <T> void bucketSort(final T[] a, final int fromIndex, final int toIndex, final Comparator<? super T> cmp) {
    //        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);
    //
    //        if (N.isEmpty(a) || fromIndex == toIndex) {
    //            return;
    //        }
    //
    //        if (toIndex - fromIndex < 32) {
    //            sort(a, fromIndex, toIndex, cmp);
    //            return;
    //        }
    //
    //        final Comparator<? super T> comparator = checkComparator(cmp);
    //        final Multiset<T> multiset = new Multiset<>();
    //
    //        for (int i = fromIndex; i < toIndex; i++) {
    //            multiset.add(a[i]);
    //        }
    //
    //        final Map<T, Integer> m = multiset.toMapSortedBy((a1, b) -> comparator.compare(a1.getKey(), a1.getKey()));
    //        int idx = fromIndex;
    //
    //        for (Map.Entry<T, Integer> entry : m.entrySet()) {
    //            N.fill(a, idx, idx + entry.getValue(), entry.getKey());
    //            idx += entry.getValue();
    //        }
    //    }
    //
    //    /**
    //     * Note: All the objects with same value will be replaced by first element with the same value.
    //     *
    //     * @param <T>
    //     * @param list
    //     */
    //    public static <T extends Comparable<? super T>> void bucketSort(final List<T> list) {
    //        if (N.isEmpty(list)) {
    //            return;
    //        }
    //
    //        bucketSort(list, 0, list.size());
    //    }
    //
    //    /**
    //     * Note: All the objects with same value will be replaced by first element with the same value.
    //     *
    //     * @param <T>
    //     * @param list
    //     * @param fromIndex
    //     * @param toIndex
    //     */
    //    public static <T extends Comparable<? super T>> void bucketSort(final List<T> list, final int fromIndex, final int toIndex) {
    //        bucketSort(list, fromIndex, toIndex, NATURAL_COMPARATOR);
    //    }
    //
    //    /**
    //     * Note: All the objects with same value will be replaced by first element with the same value.
    //     *
    //     * @param <T>
    //     * @param list
    //     * @param cmp
    //     */
    //    public static <T> void bucketSort(final List<? extends T> list, final Comparator<? super T> cmp) {
    //        if (N.isEmpty(list)) {
    //            return;
    //        }
    //
    //        bucketSort(list, 0, list.size(), cmp);
    //    }
    //
    //    /**
    //     * Note: All the objects with same value will be replaced by first element with the same value.
    //     *
    //     * @param <T>
    //     * @param list
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param cmp
    //     */
    //    public static <T> void bucketSort(final List<? extends T> list, final int fromIndex, final int toIndex, final Comparator<? super T> cmp) {
    //        N.checkFromToIndex(fromIndex, toIndex, list == null ? 0 : list.size());
    //
    //        if ((N.isEmpty(list) && fromIndex == 0 && toIndex == 0) || fromIndex == toIndex) {
    //            return;
    //        }
    //
    //        if (toIndex - fromIndex < 32) {
    //            sort(list, fromIndex, toIndex, cmp);
    //            return;
    //        }
    //
    //        final Comparator<? super T> comparator = checkComparator(cmp);
    //        final Multiset<T> multiset = new Multiset<>();
    //        ListIterator<T> itr = (ListIterator<T>) list.listIterator(fromIndex);
    //        int i = fromIndex;
    //
    //        while (itr.hasNext()) {
    //            if (i++ >= toIndex) {
    //                break;
    //            }
    //
    //            multiset.add(itr.next());
    //        }
    //
    //        final Map<T, Integer> m = multiset.toMapSortedBy((a, b) -> comparator.compare(a.getKey(), a.getKey()));
    //
    //        itr = (ListIterator<T>) list.listIterator(fromIndex);
    //
    //        for (Map.Entry<T, Integer> entry : m.entrySet()) {
    //            final T key = entry.getKey();
    //            for (int j = 0; j < entry.getValue(); j++) {
    //                itr.next();
    //                itr.set(key);
    //            }
    //        }
    //    }
    //
    //    /**
    //     * Bucket sort by.
    //     *
    //     * @param <T>
    //     * @param <U>
    //     * @param a
    //     * @param keyMapper
    //     */
    //    public static <T, U extends Comparable<? super U>> void bucketSortBy(final T[] a, final Function<? super T, ? extends U> keyMapper) {
    //        if (N.isEmpty(a)) {
    //            return;
    //        }
    //
    //        bucketSort(a, Comparators.comparingBy(keyMapper));
    //    }
    //
    //    /**
    //     * Bucket sort by.
    //     *
    //     * @param <T>
    //     * @param <U>
    //     * @param list
    //     * @param keyMapper
    //     */
    //    public static <T, U extends Comparable<? super U>> void bucketSortBy(final List<? extends T> list, final Function<? super T, ? extends U> keyMapper) {
    //        if (N.isEmpty(list)) {
    //            return;
    //        }
    //
    //        bucketSort(list, Comparators.comparingBy(keyMapper));
    //    }

    /**
     * {@link Arrays#binarySearch(boolean[], boolean)}.
     *
     * @param a
     * @param valueToFind
     * @return
     */
    static int binarySearch(final boolean[] a, final boolean valueToFind) {
        if (N.isEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        if (a[0] == valueToFind) {
            return 0;
        } else if (a[a.length - 1] != valueToFind) {
            return N.INDEX_NOT_FOUND;
        }

        int left = 0, right = a.length - 1;
        while (left < right) {
            int mid = left + (right - left) / 2;

            if (a[mid] == valueToFind) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }
        return left;
    }

    /**
     * {@link Arrays#binarySearch(char[], char)}.
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static int binarySearch(final char[] a, final char valueToFind) {
        if (N.isEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        return Arrays.binarySearch(a, valueToFind);
    }

    /**
     * {@link Arrays#binarySearch(char[], int, int, char)}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param valueToFind
     * @return
     */
    public static int binarySearch(final char[] a, final int fromIndex, final int toIndex, final char valueToFind) {
        if (N.isEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        return Arrays.binarySearch(a, fromIndex, toIndex, valueToFind);
    }

    /**
     * {@link Arrays#binarySearch(byte[], byte)}.
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static int binarySearch(final byte[] a, final byte valueToFind) {
        if (N.isEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        return Arrays.binarySearch(a, valueToFind);
    }

    /**
     * {@link Arrays#binarySearch(byte[], int, int, byte)}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param valueToFind
     * @return
     */
    public static int binarySearch(final byte[] a, final int fromIndex, final int toIndex, final byte valueToFind) {
        if (N.isEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        return Arrays.binarySearch(a, fromIndex, toIndex, valueToFind);
    }

    /**
     * {@link Arrays#binarySearch(short[], short)}.
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static int binarySearch(final short[] a, final short valueToFind) {
        if (N.isEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        return Arrays.binarySearch(a, valueToFind);
    }

    /**
     * {@link Arrays#binarySearch(short[], int, int, short)}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param valueToFind
     * @return
     */
    public static int binarySearch(final short[] a, final int fromIndex, final int toIndex, final short valueToFind) {
        if (N.isEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        return Arrays.binarySearch(a, fromIndex, toIndex, valueToFind);
    }

    /**
     * {@link Arrays#binarySearch(int[], int)}.
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static int binarySearch(final int[] a, final int valueToFind) {
        if (N.isEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        return Arrays.binarySearch(a, valueToFind);
    }

    /**
     * {@link Arrays#binarySearch(int[], int, int, int)}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param valueToFind
     * @return
     */
    public static int binarySearch(final int[] a, final int fromIndex, final int toIndex, final int valueToFind) {
        if (N.isEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        return Arrays.binarySearch(a, fromIndex, toIndex, valueToFind);
    }

    /**
     * {@link Arrays#binarySearch(long[], long)}.
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static int binarySearch(final long[] a, final long valueToFind) {
        if (N.isEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        return Arrays.binarySearch(a, valueToFind);
    }

    /**
     * {@link Arrays#binarySearch(long[], int, int, long)}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param valueToFind
     * @return
     */
    public static int binarySearch(final long[] a, final int fromIndex, final int toIndex, final long valueToFind) {
        if (N.isEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        return Arrays.binarySearch(a, fromIndex, toIndex, valueToFind);
    }

    /**
     * {@link Arrays#binarySearch(float[], float)}.
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static int binarySearch(final float[] a, final float valueToFind) {
        if (N.isEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        return Arrays.binarySearch(a, valueToFind);
    }

    /**
     * {@link Arrays#binarySearch(float[], int, int, float)}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param valueToFind
     * @return
     */
    public static int binarySearch(final float[] a, final int fromIndex, final int toIndex, final float valueToFind) {
        if (N.isEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        return Arrays.binarySearch(a, fromIndex, toIndex, valueToFind);
    }

    /**
     * {@link Arrays#binarySearch(double[], double)}.
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static int binarySearch(final double[] a, final double valueToFind) {
        if (N.isEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        return Arrays.binarySearch(a, valueToFind);
    }

    /**
     * {@link Arrays#binarySearch(double[], int, int, double)}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param valueToFind
     * @return
     */
    public static int binarySearch(final double[] a, final int fromIndex, final int toIndex, final double valueToFind) {
        if (N.isEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        return Arrays.binarySearch(a, fromIndex, toIndex, valueToFind);
    }

    /**
     * {@link Arrays#binarySearch(Object[], Object)}.
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static int binarySearch(final Object[] a, final Object valueToFind) {
        if (N.isEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        return Arrays.binarySearch(a, valueToFind);
    }

    /**
     * {@link Arrays#binarySearch(Object[], int, int, Object)}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param valueToFind
     * @return
     */
    public static int binarySearch(final Object[] a, final int fromIndex, final int toIndex, final Object valueToFind) {
        if (N.isEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        return Arrays.binarySearch(a, fromIndex, toIndex, valueToFind);
    }

    /**
     * {@link Arrays#binarySearch(Object[], Object, Comparator)}.
     *
     * @param <T>
     * @param a
     * @param valueToFind
     * @param cmp
     * @return
     */
    public static <T> int binarySearch(final T[] a, final T valueToFind, final Comparator<? super T> cmp) {
        if (N.isEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        return Arrays.binarySearch(a, valueToFind, checkComparator(cmp));
    }

    /**
     * {@link Arrays#binarySearch(Object[], int, int, Object, Comparator)}.
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param valueToFind
     * @param cmp
     * @return
     */
    public static <T> int binarySearch(final T[] a, final int fromIndex, final int toIndex, final T valueToFind, final Comparator<? super T> cmp) {
        if (N.isEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        return Arrays.binarySearch(a, fromIndex, toIndex, valueToFind, checkComparator(cmp));
    }

    /**
     * {@link Collections#binarySearch(List, Object)}.
     *
     * @param <T>
     * @param list
     * @param valueToFind
     * @return
     */
    public static <T extends Comparable<? super T>> int binarySearch(final List<? extends T> list, final T valueToFind) {
        if (N.isEmpty(list)) {
            return N.INDEX_NOT_FOUND;
        }

        return binarySearch(list, 0, list.size(), valueToFind);
    }

    /**
     *
     * @param <T>
     * @param list
     * @param fromIndex
     * @param toIndex
     * @param valueToFind
     * @return
     */
    public static <T extends Comparable<? super T>> int binarySearch(final List<? extends T> list, final int fromIndex, final int toIndex,
            final T valueToFind) {
        if (N.isEmpty(list)) {
            return N.INDEX_NOT_FOUND;
        }

        return binarySearch(list, fromIndex, toIndex, valueToFind, NATURAL_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param list
     * @param valueToFind
     * @param cmp
     * @return
     */
    public static <T> int binarySearch(final List<? extends T> list, final T valueToFind, final Comparator<? super T> cmp) {
        if (N.isEmpty(list)) {
            return N.INDEX_NOT_FOUND;
        }

        return binarySearch(list, 0, list.size(), valueToFind, cmp);
    }

    /**
     *
     * @param <T>
     * @param list
     * @param fromIndex
     * @param toIndex
     * @param valueToFind
     * @param cmp
     * @return
     * @see Collections#binarySearch(List, Object, Comparator)
     */
    public static <T> int binarySearch(final List<? extends T> list, final int fromIndex, final int toIndex, final T valueToFind, Comparator<? super T> cmp) {
        if (N.isEmpty(list)) {
            return N.INDEX_NOT_FOUND;
        }

        cmp = checkComparator(cmp);

        @SuppressWarnings("deprecation")
        final T[] a = (T[]) InternalUtil.getInternalArray(list);

        if (a != null) {
            return binarySearch(a, fromIndex, toIndex, valueToFind, cmp);
        }

        if (list instanceof RandomAccess || list.size() < BINARYSEARCH_THRESHOLD) {
            return indexedBinarySearch(list, fromIndex, toIndex, valueToFind, cmp);
        } else {
            return iteratorBinarySearch(list, fromIndex, toIndex, valueToFind, cmp);
        }
    }

    //    /**
    //     * Binary search by.
    //     *
    //     * @param <T>
    //     * @param <U>
    //     * @param a
    //     * @param valueToFind
    //     * @param valueToFindMapper
    //     * @return
    //     */
    //    public static <T, U extends Comparable<? super U>> int binarySearchBy(final T[] a, final T valueToFind,
    //            final Function<? super T, ? extends U> valueToFindMapper) {
    //        if (N.isEmpty(a)) {
    //            return N.INDEX_NOT_FOUND;
    //        }
    //
    //        return Arrays.binarySearch(a, valueToFind, Comparators.comparingBy(valueToFindMapper));
    //    }
    //
    //    /**
    //     * Binary search by.
    //     *
    //     * @param <T>
    //     * @param <U>
    //     * @param list
    //     * @param valueToFind
    //     * @param valueToFindMapper
    //     * @return
    //     */
    //    public static <T, U extends Comparable<? super U>> int binarySearchBy(final List<? extends T> list, final T valueToFind,
    //            final Function<? super T, ? extends U> valueToFindMapper) {
    //        if (N.isEmpty(list)) {
    //            return N.INDEX_NOT_FOUND;
    //        }
    //
    //        return binarySearch(list, valueToFind, Comparators.comparingBy(valueToFindMapper));
    //    }

    private static <T> int indexedBinarySearch(final List<? extends T> l, final int fromIndex, final int toIndex, final T valueToFind,
            final Comparator<? super T> cmp) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            T midVal = l.get(mid);

            int res = cmp.compare(midVal, valueToFind);

            if (res < 0) {
                low = mid + 1;
            } else if (res > 0) {
                high = mid - 1;
            } else {
                return mid; // valueToFind found
            }
        }

        return N.INDEX_NOT_FOUND; // valueToFind not found
    }

    private static <T> int iteratorBinarySearch(final List<? extends T> l, final int fromIndex, final int toIndex, final T valueToFind,
            final Comparator<? super T> cmp) {
        int low = fromIndex;
        int high = toIndex - 1;

        ListIterator<? extends T> iterator = l.listIterator();

        while (low <= high) {
            int mid = (low + high) >>> 1;
            T midVal = get(iterator, mid);

            int res = cmp.compare(midVal, valueToFind);

            if (res < 0) {
                low = mid + 1;
            } else if (res > 0) {
                high = mid - 1;
            } else {
                return mid; // valueToFind found
            }
        }

        return N.INDEX_NOT_FOUND; // valueToFind not found
    }

    private static <T> T get(final ListIterator<? extends T> iterator, final int index) {
        T obj = null;
        int pos = iterator.nextIndex();

        if (pos <= index) {
            do {
                obj = iterator.next();
            } while (pos++ < index);
        } else {
            do {
                obj = iterator.previous();
            } while (--pos > index);
        }

        return obj;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOf(final boolean[] a, final boolean valueToFind) {
        return indexOf(a, 0, valueToFind);
    }

    /**
     *
     * @param a
     * @param startIndex the index from which to start the search.
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOf(final boolean[] a, final int startIndex, final boolean valueToFind) {
        final int len = len(a);

        if (len == 0 || startIndex >= len) {
            return INDEX_NOT_FOUND;
        }

        for (int i = N.max(startIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOf(final char[] a, final char valueToFind) {
        return indexOf(a, 0, valueToFind);
    }

    /**
     *
     * @param a
     * @param startIndex the index from which to start the search.
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOf(final char[] a, final int startIndex, final char valueToFind) {
        final int len = len(a);

        if (len == 0 || startIndex >= len) {
            return INDEX_NOT_FOUND;
        }

        for (int i = N.max(startIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOf(final byte[] a, final byte valueToFind) {
        return indexOf(a, 0, valueToFind);

    }

    /**
     *
     * @param a
     * @param startIndex the index from which to start the search.
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOf(final byte[] a, final int startIndex, final byte valueToFind) {
        final int len = len(a);

        if (len == 0 || startIndex >= len) {
            return INDEX_NOT_FOUND;
        }

        for (int i = N.max(startIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOf(final short[] a, final short valueToFind) {
        return indexOf(a, 0, valueToFind);
    }

    /**
     *
     * @param a
     * @param startIndex the index from which to start the search.
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOf(final short[] a, final int startIndex, final short valueToFind) {
        final int len = len(a);

        if (len == 0 || startIndex >= len) {
            return INDEX_NOT_FOUND;
        }

        for (int i = N.max(startIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOf(final int[] a, final int valueToFind) {
        return indexOf(a, 0, valueToFind);
    }

    /**
     *
     * @param a
     * @param startIndex the index from which to start the search.
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOf(final int[] a, final int startIndex, final int valueToFind) {
        final int len = len(a);

        if (len == 0 || startIndex >= len) {
            return INDEX_NOT_FOUND;
        }

        for (int i = N.max(startIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOf(final long[] a, final long valueToFind) {
        return indexOf(a, 0, valueToFind);
    }

    /**
     *
     * @param a
     * @param startIndex the index from which to start the search.
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOf(final long[] a, final int startIndex, final long valueToFind) {
        final int len = len(a);

        if (len == 0 || startIndex >= len) {
            return INDEX_NOT_FOUND;
        }

        for (int i = N.max(startIndex, 0); i < len; i++) {
            if (a[i] == valueToFind) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOf(final float[] a, final float valueToFind) {
        return indexOf(a, 0, valueToFind);
    }

    /**
     *
     * @param a
     * @param startIndex the index from which to start the search.
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOf(final float[] a, final int startIndex, final float valueToFind) {
        final int len = len(a);

        if (len == 0 || startIndex >= len) {
            return INDEX_NOT_FOUND;
        }

        for (int i = N.max(startIndex, 0); i < len; i++) {
            if (Float.compare(a[i], valueToFind) == 0) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOf(final double[] a, final double valueToFind) {
        return indexOf(a, 0, valueToFind);
    }

    /**
     *
     * @param a
     * @param startIndex the index from which to start the search.
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOf(final double[] a, final int startIndex, final double valueToFind) {
        final int len = len(a);

        if (len == 0 || startIndex >= len) {
            return INDEX_NOT_FOUND;
        }

        for (int i = N.max(startIndex, 0); i < len; i++) {
            if (Double.compare(a[i], valueToFind) == 0) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Finds the index of the given value within a given tolerance in the array.
     * This method will return the index of the first value which falls between the region
     * defined by valueToFind - tolerance and valueToFind + tolerance.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * @param a the array to search through for the object, may be {@code null}
     * @param valueToFind
     * @param tolerance tolerance of the search
     * @return the index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(final double[] a, final double valueToFind, final double tolerance) {
        return indexOf(a, 0, valueToFind, tolerance);
    }

    /**
     * <p>Finds the index of the given value in the array starting at the given index.
     * This method will return the index of the first value which falls between the region
     * defined by valueToFind - tolerance and valueToFind + tolerance.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * <p>A negative startIndex is treated as zero. A startIndex larger than the array
     * length will return {@link #INDEX_NOT_FOUND} ({@code -1}).
     *
     * @param a the array to search through for the object, may be {@code null}
     * @param startIndex the index to start searching at
     * @param valueToFind
     * @param tolerance tolerance of the search
     * @return the index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int indexOf(final double[] a, int startIndex, final double valueToFind, final double tolerance) {
        final int len = len(a);

        if (len == 0 || startIndex >= len) {
            return INDEX_NOT_FOUND;
        }

        final double min = valueToFind - tolerance;
        final double max = valueToFind + tolerance;

        for (int i = startIndex; i < len; i++) {
            if (a[i] >= min && a[i] <= max) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOf(final Object[] a, final Object valueToFind) {
        return indexOf(a, 0, valueToFind);
    }

    /**
     *
     * @param a
     * @param startIndex the index from which to start the search.
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOf(final Object[] a, final int startIndex, final Object valueToFind) {
        final int len = len(a);

        if (len == 0 || startIndex >= len) {
            return INDEX_NOT_FOUND;
        }

        for (int i = N.max(startIndex, 0); i < len; i++) {
            if (equals(a[i], valueToFind)) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     *
     * @param c
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOf(final Collection<?> c, final Object valueToFind) {
        return indexOf(c, 0, valueToFind);
    }

    /**
     *
     *
     * @param c
     * @param startIndex the index from which to start the search.
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOf(final Collection<?> c, final int startIndex, final Object valueToFind) {
        final int len = size(c);

        if (len == 0 || startIndex >= len) {
            return INDEX_NOT_FOUND;
        }

        if (c instanceof List && c instanceof RandomAccess) {
            final List<?> list = (List<?>) c;

            for (int i = N.max(startIndex, 0); i < len; i++) {
                if (equals(list.get(i), valueToFind)) {
                    return i;
                }
            }
        } else {
            final Iterator<?> iter = c.iterator();
            int index = 0;

            if (startIndex > 0) {
                while (index < startIndex && iter.hasNext()) {
                    iter.next();
                    index++;
                }

                while (iter.hasNext()) {
                    if (N.equals(iter.hasNext(), valueToFind)) {
                        return index;
                    }

                    index++;
                }
            } else {
                while (iter.hasNext()) {
                    if (N.equals(iter.next(), valueToFind)) {
                        return index;
                    }

                    index++;
                }
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     *
     *
     * @param iter
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOf(final Iterator<?> iter, final Object valueToFind) {
        return indexOf(iter, 0, valueToFind);
    }

    /**
     *
     *
     * @param iter
     * @param startIndex
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOf(final Iterator<?> iter, final int startIndex, final Object valueToFind) {
        if (iter == null) {
            return INDEX_NOT_FOUND;
        }

        long index = 0;

        if (startIndex > 0) {
            while (index < startIndex && iter.hasNext()) {
                iter.next();
                index++;
            }

            while (iter.hasNext()) {
                if (N.equals(iter.hasNext(), valueToFind)) {
                    return Numbers.toIntExact(index);
                }

                index++;
            }
        } else {
            while (iter.hasNext()) {
                if (N.equals(iter.next(), valueToFind)) {
                    return Numbers.toIntExact(index);
                }

                index++;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     * Index of sub list.
     *
     * @param sourceList
     * @param subListToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     * @see java.util.Collections#indexOfSubList(List, List)
     */
    public static int indexOfSubList(final List<?> sourceList, final List<?> subListToFind) {
        if (isEmpty(sourceList) || isEmpty(subListToFind)) {
            return INDEX_NOT_FOUND;
        }

        return Collections.indexOfSubList(sourceList, subListToFind);
    }

    /**
     * Index of sub list.
     *
     * @param sourceList
     * @param startIndex
     * @param subListToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     * @see Index#lastOfSubList(List, int, List)
     */
    public static int indexOfSubList(final List<?> sourceList, final int startIndex, final List<?> subListToFind) {
        if (isEmpty(sourceList) || isEmpty(subListToFind)) {
            return INDEX_NOT_FOUND;
        }

        return Index.ofSubList(sourceList, startIndex, subListToFind).orElse(INDEX_NOT_FOUND);
    }

    /**
     *
     *
     * @param a
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOfIgnoreCase(final String[] a, final String valueToFind) {
        return indexOfIgnoreCase(a, 0, valueToFind);
    }

    /**
     *
     *
     * @param a
     * @param startIndex
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int indexOfIgnoreCase(final String[] a, final int startIndex, final String valueToFind) {
        final int len = len(a);

        if (len == 0 || startIndex >= len) {
            return INDEX_NOT_FOUND;
        }

        for (int i = startIndex; i < len; i++) {
            if (N.equalsIgnoreCase(a[i], valueToFind)) {
                return i;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Last index of.
     *
     * @param a
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int lastIndexOf(final boolean[] a, final boolean valueToFind) {
        return lastIndexOf(a, a.length - 1, valueToFind);
    }

    /**
     * Last index of.
     *
     * @param a
     * @param startIndexFromBack the start index to traverse backwards from
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int lastIndexOf(final boolean[] a, final int startIndexFromBack, final boolean valueToFind) {
        final int len = len(a);

        if (len == 0 || startIndexFromBack < 0) {
            return INDEX_NOT_FOUND;
        }

        for (int i = N.min(startIndexFromBack, len - 1); i >= 0; i--) {
            if (a[i] == valueToFind) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     * Last index of.
     *
     * @param a
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int lastIndexOf(final char[] a, final char valueToFind) {
        if (isEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, valueToFind);
    }

    /**
     * Last index of.
     *
     * @param a
     * @param startIndexFromBack the start index to traverse backwards from
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int lastIndexOf(final char[] a, final int startIndexFromBack, final char valueToFind) {
        final int len = len(a);

        if (len == 0 || startIndexFromBack < 0) {
            return INDEX_NOT_FOUND;
        }

        for (int i = N.min(startIndexFromBack, len - 1); i >= 0; i--) {
            if (a[i] == valueToFind) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     * Last index of.
     *
     * @param a
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int lastIndexOf(final byte[] a, final byte valueToFind) {
        if (isEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, valueToFind);

    }

    /**
     * Last index of.
     *
     * @param a
     * @param startIndexFromBack the start index to traverse backwards from
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int lastIndexOf(final byte[] a, final int startIndexFromBack, final byte valueToFind) {
        final int len = len(a);

        if (len == 0 || startIndexFromBack < 0) {
            return INDEX_NOT_FOUND;
        }

        for (int i = N.min(startIndexFromBack, len - 1); i >= 0; i--) {
            if (a[i] == valueToFind) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     * Last index of.
     *
     * @param a
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int lastIndexOf(final short[] a, final short valueToFind) {
        if (isEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, valueToFind);
    }

    /**
     * Last index of.
     *
     * @param a
     * @param startIndexFromBack the start index to traverse backwards from
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int lastIndexOf(final short[] a, final int startIndexFromBack, final short valueToFind) {
        final int len = len(a);

        if (len == 0 || startIndexFromBack < 0) {
            return INDEX_NOT_FOUND;
        }

        for (int i = N.min(startIndexFromBack, len - 1); i >= 0; i--) {
            if (a[i] == valueToFind) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     * Last index of.
     *
     * @param a
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int lastIndexOf(final int[] a, final int valueToFind) {
        if (isEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, valueToFind);
    }

    /**
     * Last index of.
     *
     * @param a
     * @param startIndexFromBack the start index to traverse backwards from
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int lastIndexOf(final int[] a, final int startIndexFromBack, final int valueToFind) {
        final int len = len(a);

        if (len == 0 || startIndexFromBack < 0) {
            return INDEX_NOT_FOUND;
        }

        for (int i = N.min(startIndexFromBack, len - 1); i >= 0; i--) {
            if (a[i] == valueToFind) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     * Last index of.
     *
     * @param a
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int lastIndexOf(final long[] a, final long valueToFind) {
        if (isEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, valueToFind);
    }

    /**
     * Last index of.
     *
     * @param a
     * @param startIndexFromBack the start index to traverse backwards from
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int lastIndexOf(final long[] a, final int startIndexFromBack, final long valueToFind) {
        final int len = len(a);

        if (len == 0 || startIndexFromBack < 0) {
            return INDEX_NOT_FOUND;
        }

        for (int i = N.min(startIndexFromBack, len - 1); i >= 0; i--) {
            if (a[i] == valueToFind) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     * Last index of.
     *
     * @param a
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int lastIndexOf(final float[] a, final float valueToFind) {
        if (isEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, valueToFind);
    }

    /**
     * Last index of.
     *
     * @param a
     * @param startIndexFromBack the start index to traverse backwards from
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int lastIndexOf(final float[] a, final int startIndexFromBack, final float valueToFind) {
        final int len = len(a);

        if (len == 0 || startIndexFromBack < 0) {
            return INDEX_NOT_FOUND;
        }

        for (int i = N.min(startIndexFromBack, len - 1); i >= 0; i--) {
            if (Float.compare(a[i], valueToFind) == 0) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     * Last index of.
     *
     * @param a
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int lastIndexOf(final double[] a, final double valueToFind) {
        if (isEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, valueToFind);
    }

    /**
     * Last index of.
     *
     * @param a
     * @param startIndexFromBack the start index to traverse backwards from
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int lastIndexOf(final double[] a, final int startIndexFromBack, final double valueToFind) {
        final int len = len(a);

        if (len == 0 || startIndexFromBack < 0) {
            return INDEX_NOT_FOUND;
        }

        for (int i = N.min(startIndexFromBack, len - 1); i >= 0; i--) {
            if (Double.compare(a[i], valueToFind) == 0) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     * <p>Finds the last index of the given value within a given tolerance in the array.
     * This method will return the index of the last value which falls between the region
     * defined by valueToFind - tolerance and valueToFind + tolerance.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * @param a the array to search through for the object, may be {@code null}
     * @param valueToFind
     * @param tolerance tolerance of the search
     * @return the index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(final double[] a, final double valueToFind, final double tolerance) {
        return lastIndexOf(a, 0, valueToFind, tolerance);
    }

    /**
     * <p>Finds the last index of the given value in the array starting at the given index.
     * This method will return the index of the last value which falls between the region
     * defined by valueToFind - tolerance and valueToFind + tolerance.
     *
     * <p>This method returns {@link #INDEX_NOT_FOUND} ({@code -1}) for a {@code null} input array.
     *
     * <p>A negative startIndex will return {@link #INDEX_NOT_FOUND} ({@code -1}). A startIndex larger than the
     * array length will search from the end of the array.
     *
     * @param a the array to traverse for looking for the object, may be {@code null}
     * @param startIndexFromBack the start index to traverse backwards from
     * @param valueToFind
     * @param tolerance search for value within plus/minus this amount
     * @return the last index of the value within the array,
     *  {@link #INDEX_NOT_FOUND} ({@code -1}) if not found or {@code null} array input
     */
    public static int lastIndexOf(final double[] a, int startIndexFromBack, final double valueToFind, final double tolerance) {
        final int len = len(a);

        if (len == 0 || startIndexFromBack < 0) {
            return INDEX_NOT_FOUND;
        }

        final double min = valueToFind - tolerance;
        final double max = valueToFind + tolerance;

        for (int i = N.min(startIndexFromBack, len - 1); i >= 0; i--) {
            if (a[i] >= min && a[i] <= max) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     * Last index of.
     *
     * @param a
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int lastIndexOf(final Object[] a, final Object valueToFind) {
        if (isEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, valueToFind);
    }

    /**
     * Last index of.
     *
     * @param a
     * @param startIndexFromBack the start index to traverse backwards from
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int lastIndexOf(final Object[] a, final int startIndexFromBack, final Object valueToFind) {
        final int len = len(a);

        if (len == 0 || startIndexFromBack < 0) {
            return INDEX_NOT_FOUND;
        }

        for (int i = N.min(startIndexFromBack, len - 1); i >= 0; i--) {
            if (equals(a[i], valueToFind)) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     * Last index of.
     *
     * @param c
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int lastIndexOf(final Collection<?> c, final Object valueToFind) {
        if (isEmpty(c)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(c, c.size() - 1, valueToFind);
    }

    /**
     * Last index of.
     *
     * @param c
     * @param startIndexFromBack the start index to traverse backwards from
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int lastIndexOf(final Collection<?> c, final int startIndexFromBack, final Object valueToFind) {
        final int size = N.size(c);

        if (size == 0 || startIndexFromBack < 0) {
            return INDEX_NOT_FOUND;
        }

        if (c instanceof List && c instanceof RandomAccess) {
            final List<Object> list = (List<Object>) c;

            for (int i = N.min(startIndexFromBack, size - 1); i >= 0; i--) {
                if (N.equals(list.get(i), valueToFind)) {
                    return i;
                }
            }

            return INDEX_NOT_FOUND;
        }

        final Iterator<Object> descendingIterator = getDescendingIteratorIfPossible(c);

        if (descendingIterator != null) {
            for (int i = size - 1; descendingIterator.hasNext(); i--) {
                if (i > startIndexFromBack) {
                    descendingIterator.next();
                } else if (N.equals(descendingIterator.next(), valueToFind)) {
                    return i;
                }
            }

            return INDEX_NOT_FOUND;
        }

        final Object[] a = c.toArray();

        return lastIndexOf(a, startIndexFromBack, valueToFind);
    }

    /**
     * Last index of sub list.
     *
     * @param sourceList
     * @param subListToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     * @see java.util.Collections#lastIndexOfSubList(List, List)
     */
    public static int lastIndexOfSubList(final List<?> sourceList, final List<?> subListToFind) {
        if (isEmpty(sourceList) || isEmpty(subListToFind)) {
            return INDEX_NOT_FOUND;
        }

        return Collections.lastIndexOfSubList(sourceList, subListToFind);
    }

    /**
     * Last index of sub list.
     *
     * @param sourceList
     * @param startIndexFromBack
     * @param subListToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     * @see java.util.Collections#lastIndexOfSubList(List, List)
     */
    public static int lastIndexOfSubList(final List<?> sourceList, final int startIndexFromBack, final List<?> subListToFind) {
        if (isEmpty(sourceList) || isEmpty(subListToFind)) {
            return INDEX_NOT_FOUND;
        }

        return Index.lastOfSubList(sourceList, startIndexFromBack, subListToFind).orElse(INDEX_NOT_FOUND);
    }

    /**
     *
     *
     * @param a
     * @param valueToFind
     * @return {@code -1} if no target value/element is found in the specified {@code Collection/Array}.
     */
    public static int lastIndexOfIgnoreCase(final String[] a, final String valueToFind) {
        if (isEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOfIgnoreCase(a, a.length - 1, valueToFind);
    }

    /**
     *
     *
     * @param a
     * @param startIndexFromBack
     * @param valueToFind
     * @return
     */
    public static int lastIndexOfIgnoreCase(final String[] a, final int startIndexFromBack, final String valueToFind) {
        final int len = len(a);

        if (len == 0 || startIndexFromBack < 0) {
            return INDEX_NOT_FOUND;
        }

        for (int i = N.min(startIndexFromBack, len - 1); i >= 0; i--) {
            if (N.equalsIgnoreCase(a[i], valueToFind)) {
                return i;
            }
        }

        return N.INDEX_NOT_FOUND;
    }

    /**
     * Find first index.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param predicate
     * @return an {@code OptionalInt} with the index of found target value/element, or an empty {@code OptionalInt} no target value/element found.
     * @throws E the e
     */
    public static <T, E extends Exception> OptionalInt findFirstIndex(final T[] a, final Throwables.Predicate<? super T, E> predicate) throws E {
        if (N.isEmpty(a)) {
            return OptionalInt.empty();
        }

        for (int len = a.length, i = 0; i < len; i++) {
            if (predicate.test(a[i])) {
                return OptionalInt.of(i);
            }
        }

        return OptionalInt.empty();
    }

    /**
     * Find first index.
     *
     * @param <T>
     * @param <U>
     * @param <E>
     * @param a
     * @param valueToFind
     * @param predicate
     * @return an {@code OptionalInt} with the index of found target value/element, or an empty {@code OptionalInt} no target value/element found.
     * @throws E the e
     */
    public static <T, U, E extends Exception> OptionalInt findFirstIndex(final T[] a, final U valueToFind,
            final Throwables.BiPredicate<? super T, ? super U, E> predicate) throws E {
        if (N.isEmpty(a)) {
            return OptionalInt.empty();
        }

        for (int len = a.length, i = 0; i < len; i++) {
            if (predicate.test(a[i], valueToFind)) {
                return OptionalInt.of(i);
            }
        }

        return OptionalInt.empty();
    }

    /**
     * Find first index.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param predicate
     * @return an {@code OptionalInt} with the index of found target value/element, or an empty {@code OptionalInt} no target value/element found.
     * @throws E the e
     */
    public static <T, E extends Exception> OptionalInt findFirstIndex(final Collection<? extends T> c, final Throwables.Predicate<? super T, E> predicate)
            throws E {
        if (N.isEmpty(c)) {
            return OptionalInt.empty();
        }

        int idx = 0;

        for (T e : c) {
            if (predicate.test(e)) {
                return OptionalInt.of(idx);
            }

            idx++;
        }

        return OptionalInt.empty();
    }

    /**
     * Find first index.
     *
     * @param <T>
     * @param <U>
     * @param <E>
     * @param c
     * @param valueToFind
     * @param predicate
     * @return an {@code OptionalInt} with the index of found target value/element, or an empty {@code OptionalInt} no target value/element found.
     * @throws E the e
     */
    public static <T, U, E extends Exception> OptionalInt findFirstIndex(final Collection<? extends T> c, final U valueToFind,
            final Throwables.BiPredicate<? super T, ? super U, E> predicate) throws E {
        if (N.isEmpty(c)) {
            return OptionalInt.empty();
        }

        int idx = 0;

        for (T e : c) {
            if (predicate.test(e, valueToFind)) {
                return OptionalInt.of(idx);
            }

            idx++;
        }

        return OptionalInt.empty();
    }

    /**
     * Find last index.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param predicate
     * @return an {@code OptionalInt} with the index of found target value/element, or an empty {@code OptionalInt} no target value/element found.
     * @throws E the e
     */
    public static <T, E extends Exception> OptionalInt findLastIndex(final T[] a, final Throwables.Predicate<? super T, E> predicate) throws E {
        if (N.isEmpty(a)) {
            return OptionalInt.empty();
        }

        for (int i = a.length - 1; i >= 0; i--) {
            if (predicate.test(a[i])) {
                return OptionalInt.of(i);
            }
        }

        return OptionalInt.empty();
    }

    /**
     * Find last index.
     *
     * @param <T>
     * @param <U>
     * @param <E>
     * @param a
     * @param valueToFind
     * @param predicate
     * @return an {@code OptionalInt} with the index of found target value/element, or an empty {@code OptionalInt} no target value/element found.
     * @throws E the e
     */
    public static <T, U, E extends Exception> OptionalInt findLastIndex(final T[] a, final U valueToFind,
            final Throwables.BiPredicate<? super T, ? super U, E> predicate) throws E {
        if (N.isEmpty(a)) {
            return OptionalInt.empty();
        }

        for (int i = a.length - 1; i >= 0; i--) {
            if (predicate.test(a[i], valueToFind)) {
                return OptionalInt.of(i);
            }
        }

        return OptionalInt.empty();
    }

    /**
     * Find last index.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param predicate
     * @return an {@code OptionalInt} with the index of found target value/element, or an empty {@code OptionalInt} no target value/element found.
     * @throws E the e
     */
    public static <T, E extends Exception> OptionalInt findLastIndex(final Collection<? extends T> c, final Throwables.Predicate<? super T, E> predicate)
            throws E {
        if (N.isEmpty(c)) {
            return OptionalInt.empty();
        }

        final int size = c.size();

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = size - 1; i >= 0; i--) {
                if (predicate.test(list.get(i))) {
                    return OptionalInt.of(i);
                }
            }

            return OptionalInt.empty();
        }

        final Iterator<T> descendingIterator = getDescendingIteratorIfPossible(c);

        if (descendingIterator != null) {
            for (int i = size - 1; descendingIterator.hasNext(); i--) {
                if (predicate.test(descendingIterator.next())) {
                    return OptionalInt.of(i);
                }
            }

            return OptionalInt.empty();
        }

        final T[] a = (T[]) c.toArray();

        for (int i = a.length - 1; i >= 0; i--) {
            if (predicate.test(a[i])) {
                return OptionalInt.of(i);
            }
        }

        return OptionalInt.empty();
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param <E>
     * @param c
     * @param valueToFind
     * @param predicate
     * @return an {@code OptionalInt} with the index of found target value/element, or an empty {@code OptionalInt} no target value/element found.
     * @throws E
     */
    public static <T, U, E extends Exception> OptionalInt findLastIndex(final Collection<? extends T> c, final U valueToFind,
            final Throwables.BiPredicate<? super T, ? super U, E> predicate) throws E {
        if (N.isEmpty(c)) {
            return OptionalInt.empty();
        }

        final Throwables.Predicate<? super T, E> predicate2 = t -> predicate.test(t, valueToFind);

        return findLastIndex(c, predicate2);
    }

    /**
     *
     *
     * @param <T>
     * @param a
     * @return the indices of all minimum value/element in the specified {@code Collection/Array}.
     * @throws IllegalArgumentException
     */
    public static <T extends Comparable<? super T>> int[] indicesOfAllMin(final T[] a) throws IllegalArgumentException {
        return indicesOfAllMin(a, NATURAL_COMPARATOR);
    }

    /**
     *
     *
     * @param <T>
     * @param a
     * @param cmp
     * @return the indices of all minimum value/element in the specified {@code Collection/Array}.
     * @throws IllegalArgumentException
     */
    public static <T> int[] indicesOfAllMin(final T[] a, Comparator<? super T> cmp) throws IllegalArgumentException {
        if (isEmpty(a)) {
            return EMPTY_INT_ARRAY;
        }

        cmp = cmp == null ? (Comparator<T>) NULL_MAX_COMPARATOR : cmp;
        final IntList result = new IntList();
        final int len = N.len(a);
        T candidate = a[0];
        int cp = 0;

        result.add(0);

        for (int idx = 1; idx < len; idx++) {
            cp = cmp.compare(a[idx], candidate);

            if (cp == 0) {
                result.add(idx);
            } else if (cp < 0) {
                result.clear();
                result.add(idx);
                candidate = a[idx];
            }
        }

        return result.toArray();
    }

    /**
     *
     *
     * @param <T>
     * @param c
     * @return the indices of all minimum value/element in the specified {@code Collection/Array}.
     * @throws IllegalArgumentException
     */
    public static <T extends Comparable<? super T>> int[] indicesOfAllMin(final Collection<? extends T> c) throws IllegalArgumentException {
        return indicesOfAllMin(c, NATURAL_COMPARATOR);
    }

    /**
     *
     *
     * @param <T>
     * @param c
     * @param cmp
     * @return the indices of all minimum value/element in the specified {@code Collection/Array}.
     * @throws IllegalArgumentException
     */
    public static <T> int[] indicesOfAllMin(final Collection<? extends T> c, Comparator<? super T> cmp) throws IllegalArgumentException {
        if (isEmpty(c)) {
            return EMPTY_INT_ARRAY;
        }

        cmp = cmp == null ? (Comparator<T>) NULL_MAX_COMPARATOR : cmp;
        final IntList result = new IntList();
        final Iterator<? extends T> iter = c.iterator();

        T candidate = iter.next();
        T next = null;
        int cp = 0;
        int idx = 0;

        result.add(idx++);

        while (iter.hasNext()) {
            next = iter.next();
            cp = cmp.compare(next, candidate);

            if (cp == 0) {
                result.add(idx);
            } else if (cp < 0) {
                result.clear();
                result.add(idx);
                candidate = next;
            }

            idx++;
        }

        return result.toArray();
    }

    /**
     *
     *
     * @param <T>
     * @param a
     * @return the indices of all maximum value/element in the specified {@code Collection/Array}.
     * @throws IllegalArgumentException
     */
    public static <T extends Comparable<? super T>> int[] indicesOfAllMax(final T[] a) throws IllegalArgumentException {
        return indicesOfAllMax(a, NATURAL_COMPARATOR);
    }

    /**
     *
     *
     * @param <T>
     * @param a
     * @param cmp
     * @return the indices of all maximum value/element in the specified {@code Collection/Array}.
     * @throws IllegalArgumentException
     */
    public static <T> int[] indicesOfAllMax(final T[] a, Comparator<? super T> cmp) throws IllegalArgumentException {
        if (isEmpty(a)) {
            return EMPTY_INT_ARRAY;
        }

        cmp = cmp == null ? (Comparator<T>) NULL_MIN_COMPARATOR : cmp;
        final IntList result = new IntList();
        final int len = N.len(a);
        T candidate = a[0];
        int cp = 0;

        result.add(0);

        for (int idx = 1; idx < len; idx++) {
            cp = cmp.compare(a[idx], candidate);

            if (cp == 0) {
                result.add(idx);
            } else if (cp > 0) {
                result.clear();
                result.add(idx);
                candidate = a[idx];
            }
        }

        return result.toArray();
    }

    /**
     *
     *
     * @param <T>
     * @param c
     * @return the indices of all maximum value/element in the specified {@code Collection/Array}.
     * @throws IllegalArgumentException
     */
    public static <T extends Comparable<? super T>> int[] indicesOfAllMax(final Collection<? extends T> c) throws IllegalArgumentException {
        return indicesOfAllMax(c, NATURAL_COMPARATOR);
    }

    /**
     *
     *
     * @param <T>
     * @param c
     * @param cmp
     * @return the indices of all maximum value/element in the specified {@code Collection/Array}.
     * @throws IllegalArgumentException
     */
    public static <T> int[] indicesOfAllMax(final Collection<? extends T> c, Comparator<? super T> cmp) throws IllegalArgumentException {
        if (isEmpty(c)) {
            return EMPTY_INT_ARRAY;
        }

        cmp = cmp == null ? (Comparator<T>) NULL_MIN_COMPARATOR : cmp;
        final IntList result = new IntList();
        final Iterator<? extends T> iter = c.iterator();

        T candidate = iter.next();
        T next = null;
        int cp = 0;
        int idx = 0;

        result.add(idx++);

        while (iter.hasNext()) {
            next = iter.next();
            cp = cmp.compare(next, candidate);

            if (cp == 0) {
                result.add(idx);
            } else if (cp > 0) {
                result.clear();
                result.add(idx);
                candidate = next;
            }

            idx++;
        }

        return result.toArray();
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return the indices of all found target value/element in the specified {@code Collection/Array}.
     */
    public static int[] indicesOfAll(final Object[] a, final Object valueToFind) {
        return indicesOfAll(a, 0, valueToFind);
    }

    /**
     *
     * @param a
     * @param startIndex
     * @param valueToFind
     * @return the indices of all found target value/element in the specified {@code Collection/Array}.
     */
    public static int[] indicesOfAll(final Object[] a, int startIndex, final Object valueToFind) {
        final int len = N.len(a);

        if (len == 0 || startIndex >= len) {
            return EMPTY_INT_ARRAY;
        }

        final IntList result = new IntList();

        for (int idx = N.max(startIndex, 0); idx < len; idx++) {
            if (N.equals(a[idx], valueToFind)) {
                result.add(idx);
            }
        }

        return result.toArray();
    }

    /**
     *
     * @param c
     * @param valueToFind
     * @return the indices of all found target value/element in the specified {@code Collection/Array}.
     */
    public static int[] indicesOfAll(final Collection<?> c, final Object valueToFind) {
        return indicesOfAll(c, 0, valueToFind);
    }

    /**
     *
     * @param c
     * @param startIndex
     * @param valueToFind
     * @return the indices of all found target value/element in the specified {@code Collection/Array}.
     */
    public static int[] indicesOfAll(final Collection<?> c, int startIndex, final Object valueToFind) {
        final int size = N.size(c);

        if (size == 0 || startIndex >= size) {
            return EMPTY_INT_ARRAY;
        }

        final IntList result = new IntList();

        if (c instanceof List && c instanceof RandomAccess) {
            final List<?> list = (List<?>) c;

            for (int idx = N.max(startIndex, 0); idx < size; idx++) {
                if (N.equals(list.get(idx), valueToFind)) {
                    result.add(idx);
                }
            }
        } else {
            final Iterator<?> iter = c.iterator();

            int idx = 0;

            while (idx < startIndex) {
                iter.next();
                idx++;
            }

            while (iter.hasNext()) {
                if (N.equals(iter.next(), valueToFind)) {
                    result.add(idx);
                }

                idx++;
            }
        }

        return result.toArray();
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param predicate
     * @return the indices of all found target value/element in the specified {@code Collection/Array}.
     * @throws E
     */
    public static <T, E extends Exception> int[] indicesOfAll(final T[] a, final Throwables.Predicate<? super T, E> predicate) throws E {
        return indicesOfAll(a, 0, predicate);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param startIndex
     * @param predicate
     * @return the indices of all found target value/element in the specified {@code Collection/Array}.
     * @throws E
     */
    public static <T, E extends Exception> int[] indicesOfAll(final T[] a, int startIndex, final Throwables.Predicate<? super T, E> predicate) throws E {
        final int len = N.len(a);

        if (len == 0 || startIndex >= len) {
            return EMPTY_INT_ARRAY;
        }

        final IntList result = new IntList();

        for (int idx = N.max(startIndex, 0); idx < len; idx++) {
            if (predicate.test(a[idx])) {
                result.add(idx);
            }
        }

        return result.toArray();
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param predicate
     * @return the indices of all found target value/element in the specified {@code Collection/Array}.
     * @throws E
     */
    public static <T, E extends Exception> int[] indicesOfAll(final Collection<? extends T> c, final Throwables.Predicate<? super T, E> predicate) throws E {
        return indicesOfAll(c, 0, predicate);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param startIndex
     * @param predicate
     * @return the indices of all found target value/element in the specified {@code Collection/Array}.
     * @throws E
     */
    public static <T, E extends Exception> int[] indicesOfAll(final Collection<? extends T> c, int startIndex,
            final Throwables.Predicate<? super T, E> predicate) throws E {
        final int size = N.size(c);

        if (size == 0 || startIndex >= size) {
            return EMPTY_INT_ARRAY;
        }

        final IntList result = new IntList();

        if (c instanceof List && c instanceof RandomAccess) {
            final List<? extends T> list = (List<? extends T>) c;

            for (int i = N.max(startIndex, 0); i < size; i++) {
                if (predicate.test(list.get(i))) {
                    result.add(i);
                }
            }
        } else {
            final Iterator<? extends T> iter = c.iterator();

            int idx = 0;

            while (idx < startIndex) {
                iter.next();
                idx++;
            }

            while (iter.hasNext()) {
                if (predicate.test(iter.next())) {
                    result.add(idx);
                }

                idx++;
            }
        }

        return result.toArray();
    }

    static <T> Iterator<T> getDescendingIteratorIfPossible(final Iterable<? extends T> c) {
        if (c instanceof Deque) {
            return ((Deque<T>) c).descendingIterator();
        } else {
            try {
                Method m = null;

                if ((m = ClassUtil.getDeclaredMethod(c.getClass(), "descendingIterator")) != null && Modifier.isPublic(m.getModifiers())
                        && Iterator.class.isAssignableFrom(m.getReturnType())) {

                    return (Iterator<T>) ClassUtil.invokeMethod(c, m);
                }
            } catch (Exception e) {
                // continue
            }
        }

        return null; // NOSONAR
    }

    /**
     * Creates the mask.
     *
     * @param <T>
     * @param interfaceClass
     * @return
     */
    static <T> T createMask(final Class<T> interfaceClass) {
        InvocationHandler h = (proxy, method, args) -> {
            throw new UnsupportedOperationException("It's a mask");
        };

        return newProxyInstance(interfaceClass, h);
    }
}
