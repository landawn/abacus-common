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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.landawn.abacus.DataSet;
import com.landawn.abacus.DirtyMarker;
import com.landawn.abacus.EntityId;
import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.NullSafe;
import com.landawn.abacus.core.EntityUtil;
import com.landawn.abacus.core.MapEntity;
import com.landawn.abacus.core.RowDataSet;
import com.landawn.abacus.exception.UncheckedException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.DeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.parser.JSONSerializationConfig;
import com.landawn.abacus.parser.XMLDeserializationConfig;
import com.landawn.abacus.parser.XMLDeserializationConfig.XDC;
import com.landawn.abacus.parser.XMLSerializationConfig;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.Fn.Factory;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;

/**
 * <p>
 * Note: This class includes codes copied from Apache Commons Lang, Google Guava and other open source projects under the Apache License 2.0.
 * The methods copied from other libraries/frameworks/projects may be modified in this class.
 * </p>
 * Class <code>N</code> is a general java utility class. It provides the most daily used operations for Object/primitive types/String/Array/Collection/Map/Entity...:
 *
 * When to throw exception? It's designed to avoid throwing any unnecessary
 * exception if the contract defined by method is not broken. for example, if
 * user tries to reverse a null or empty String. the input String will be
 * returned. But exception will be thrown if trying to repeat/swap a null or
 * empty string or operate Array/Collection by adding/removing... <br>
 *
 * @author Haiyang Li
 *
 * @version $Revision: 0.8 $ 07/03/10
 *
 * @see com.landawn.abacus.util.IOUtil
 * @see com.landawn.abacus.util.StringUtil
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Maps
 * @see com.landawn.abacus.util.Primitives
 * @see com.landawn.abacus.util.Array
 * @see com.landawn.abacus.util.Seq
 */
public final class N {
    private static final AsyncExecutor asyncExecutor = new AsyncExecutor(32, 256, 300L, TimeUnit.SECONDS);

    // ... it has to be big enough to make it's safety to add element to
    // ArrayBlockingQueue.
    private static final int POOL_SIZE = Internals.POOL_SIZE;

    // ...
    static final String ELEMENT_SEPARATOR = Type.ELEMENT_SEPARATOR;
    static final char[] ELEMENT_SEPARATOR_CHAR_ARRAY = Type.ELEMENT_SEPARATOR_CHAR_ARRAY;

    /**
     * An empty immutable {@code Boolean} array.
     */
    static final Boolean[] EMPTY_BOOLEAN_OBJECT_ARRAY = new Boolean[0];
    /**
     * An empty immutable {@code Character} array.
     */
    static final Character[] EMPTY_CHARACTER_OBJECT_ARRAY = new Character[0];
    /**
     * An empty immutable {@code Byte} array.
     */
    static final Byte[] EMPTY_BYTE_OBJECT_ARRAY = new Byte[0];
    /**
     * An empty immutable {@code Short} array.
     */
    static final Short[] EMPTY_SHORT_OBJECT_ARRAY = new Short[0];
    /**
     * An empty immutable {@code Integer} array.
     */
    static final Integer[] EMPTY_INTEGER_OBJECT_ARRAY = new Integer[0];
    /**
     * An empty immutable {@code Long} array.
     */
    static final Long[] EMPTY_LONG_OBJECT_ARRAY = new Long[0];
    /**
     * An empty immutable {@code Float} array.
     */
    static final Float[] EMPTY_FLOAT_OBJECT_ARRAY = new Float[0];
    /**
     * An empty immutable {@code Double} array.
     */
    static final Double[] EMPTY_DOUBLE_OBJECT_ARRAY = new Double[0];
    /**
     * An empty immutable {@code Class} array.
     */
    static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];
    static final String NULL_STRING = "null".intern();
    static final char[] NULL_CHAR_ARRAY = NULL_STRING.toCharArray();
    static final String TRUE = Boolean.TRUE.toString().intern();
    static final char[] TRUE_CHAR_ARRAY = TRUE.toCharArray();
    static final String FALSE = Boolean.FALSE.toString().intern();
    static final char[] FALSE_CHAR_ARRAY = FALSE.toCharArray();

    // ...
    static final char CHAR_0 = WD.CHAR_0;

    /**
     *
     * @see <a
     *      href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6">JLF:
     *      Escape Sequences for Character and String Literals</a>
     * @since 2.2
     */
    static final char CHAR_LF = WD.CHAR_LF;

    /**
     *
     * @see <a
     *      href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6">JLF:
     *      Escape Sequences for Character and String Literals</a>
     * @since 2.2
     */
    static final char CHAR_CR = WD.CHAR_CR;

    // ...
    /**
     * The index value when an element is not found in a list or array:
     * {@code -1}. This value is returned by methods in this class and can also
     * be used in comparisons with values returned by various method from
     * {@link java.util.List} .
     */
    public static final int INDEX_NOT_FOUND = -1;

    // ...
    public static final String EMPTY_STRING = "".intern();

    /**
     * An empty immutable {@code boolean} array.
     */
    public static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];
    /**
     * An empty immutable {@code char} array.
     */
    public static final char[] EMPTY_CHAR_ARRAY = new char[0];
    /**
     * An empty immutable {@code byte} array.
     */
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    /**
     * An empty immutable {@code short} array.
     */
    public static final short[] EMPTY_SHORT_ARRAY = new short[0];
    /**
     * An empty immutable {@code int} array.
     */
    public static final int[] EMPTY_INT_ARRAY = new int[0];
    /**
     * An empty immutable {@code long} array.
     */
    public static final long[] EMPTY_LONG_ARRAY = new long[0];
    /**
     * An empty immutable {@code float} array.
     */
    public static final float[] EMPTY_FLOAT_ARRAY = new float[0];
    /**
     * An empty immutable {@code double} array.
     */
    public static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
    /**
     * An empty immutable {@code Boolean} array.
     */
    public static final Boolean[] EMPTY_BOOLEAN_OBJ_ARRAY = new Boolean[0];
    /**
     * An empty immutable {@code Character} array.
     */
    public static final Character[] EMPTY_CHAR_OBJ_ARRAY = new Character[0];
    /**
     * An empty immutable {@code Byte} array.
     */
    public static final Byte[] EMPTY_BYTE_OBJ_ARRAY = new Byte[0];
    /**
     * An empty immutable {@code Short} array.
     */
    public static final Short[] EMPTY_SHORT_OBJ_ARRAY = new Short[0];

    /**
     * An empty immutable {@code Integer} array.
     */
    public static final Integer[] EMPTY_INT_OBJ_ARRAY = new Integer[0];
    /**
     * An empty immutable {@code Long} array.
     */
    public static final Long[] EMPTY_LONG_OBJ_ARRAY = new Long[0];
    /**
     * An empty immutable {@code Float} array.
     */
    public static final Float[] EMPTY_FLOAT_OBJ_ARRAY = new Float[0];
    /**
     * An empty immutable {@code Double} array.
     */
    public static final Double[] EMPTY_DOUBLE_OBJ_ARRAY = new Double[0];
    /**
     * An empty immutable {@code String} array.
     */
    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    /**
     * An empty immutable {@code Object} array.
     */
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    @SuppressWarnings("rawtypes")
    static final List EMPTY_LIST = Collections.emptyList();
    @SuppressWarnings("rawtypes")
    static final Set EMPTY_SET = Collections.emptySet();
    @SuppressWarnings("rawtypes")
    static final SortedSet EMPTY_SORTED_SET = Collections.emptySortedSet();
    @SuppressWarnings("rawtypes")
    static final NavigableSet EMPTY_NAVIGABLE_SET = Collections.emptyNavigableSet();
    @SuppressWarnings("rawtypes")
    static final Map EMPTY_MAP = Collections.emptyMap();
    @SuppressWarnings("rawtypes")
    static final SortedMap EMPTY_SORTED_MAP = Collections.emptySortedMap();
    @SuppressWarnings("rawtypes")
    static final NavigableMap EMPTY_NAVIGABLE_MAP = Collections.emptyNavigableMap();
    @SuppressWarnings("rawtypes")
    static final Iterator EMPTY_ITERATOR = Collections.emptyIterator();
    @SuppressWarnings("rawtypes")
    static final ListIterator EMPTY_LIST_ITERATOR = Collections.emptyListIterator();

    // ...
    static final Object NULL_MASK = new NullMask();

    static final String BACKSLASH_ASTERISK = "*";

    // ...
    private static final int REVERSE_THRESHOLD = 18;
    private static final int FILL_THRESHOLD = 25;
    private static final int REPLACEALL_THRESHOLD = 11;

    // ...
    static final Random RAND = new SecureRandom();

    @SuppressWarnings("rawtypes")
    private static final Comparator NULL_MIN_COMPARATOR = Comparators.nullsFirst();
    @SuppressWarnings("rawtypes")
    private static final Comparator NULL_MAX_COMPARATOR = Comparators.nullsLast();
    @SuppressWarnings("rawtypes")
    private static final Comparator NATURAL_ORDER = Comparators.naturalOrder();

    // ...
    static final Map<Class<?>, Object> CLASS_EMPTY_ARRAY = new ConcurrentHashMap<>();

    static {
        CLASS_EMPTY_ARRAY.put(boolean.class, N.EMPTY_BOOLEAN_ARRAY);
        CLASS_EMPTY_ARRAY.put(Boolean.class, N.EMPTY_BOOLEAN_OBJECT_ARRAY);

        CLASS_EMPTY_ARRAY.put(char.class, N.EMPTY_CHAR_ARRAY);
        CLASS_EMPTY_ARRAY.put(Character.class, N.EMPTY_CHARACTER_OBJECT_ARRAY);

        CLASS_EMPTY_ARRAY.put(byte.class, N.EMPTY_BYTE_ARRAY);
        CLASS_EMPTY_ARRAY.put(Byte.class, N.EMPTY_BYTE_OBJECT_ARRAY);

        CLASS_EMPTY_ARRAY.put(short.class, N.EMPTY_SHORT_ARRAY);
        CLASS_EMPTY_ARRAY.put(Short.class, N.EMPTY_SHORT_OBJECT_ARRAY);

        CLASS_EMPTY_ARRAY.put(int.class, N.EMPTY_INT_ARRAY);
        CLASS_EMPTY_ARRAY.put(Integer.class, N.EMPTY_INTEGER_OBJECT_ARRAY);

        CLASS_EMPTY_ARRAY.put(long.class, N.EMPTY_LONG_ARRAY);
        CLASS_EMPTY_ARRAY.put(Long.class, N.EMPTY_LONG_OBJECT_ARRAY);

        CLASS_EMPTY_ARRAY.put(float.class, N.EMPTY_FLOAT_ARRAY);
        CLASS_EMPTY_ARRAY.put(Float.class, N.EMPTY_FLOAT_OBJECT_ARRAY);

        CLASS_EMPTY_ARRAY.put(double.class, N.EMPTY_DOUBLE_ARRAY);
        CLASS_EMPTY_ARRAY.put(Double.class, N.EMPTY_DOUBLE_OBJECT_ARRAY);

        CLASS_EMPTY_ARRAY.put(String.class, N.EMPTY_STRING_ARRAY);
        CLASS_EMPTY_ARRAY.put(Object.class, N.EMPTY_OBJECT_ARRAY);
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
    private static final Map<Class<? extends Enum<?>>, List<? extends Enum<?>>> enumListPool = new ObjectPool<>(POOL_SIZE);
    private static final Map<Class<? extends Enum<?>>, Set<? extends Enum<?>>> enumSetPool = new ObjectPool<>(POOL_SIZE);
    private static final Map<Class<? extends Enum<?>>, BiMap<? extends Enum<?>, String>> enumMapPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<String, Type<?>> nameTypePool = new ObjectPool<>(POOL_SIZE);
    private static final Map<Class<?>, Type<?>> clsTypePool = new ObjectPool<>(POOL_SIZE);

    // ...
    static final Field listElementDataField;
    static final Field listSizeField;
    static volatile boolean isListElementDataFieldGettable = true;
    static volatile boolean isListElementDataFieldSettable = true;

    static {
        Field tmp = null;

        try {
            tmp = String.class.getDeclaredField("offset");
        } catch (Throwable e) {
            // ignore.
        }

        if (tmp == null) {
            try {
                tmp = String.class.getDeclaredField("count");
            } catch (Throwable e) {
                // ignore.
            }
        }

        if (tmp == null) {
            try {
                tmp = String.class.getDeclaredField("value");
            } catch (Throwable e) {
                // ignore.
            }
        }

        tmp = null;

        try {
            tmp = ArrayList.class.getDeclaredField("elementData");
        } catch (Throwable e) {
            // ignore.
        }

        listElementDataField = tmp != null && tmp.getType().equals(Object[].class) ? tmp : null;

        if (listElementDataField != null) {
            listElementDataField.setAccessible(true);
        }

        tmp = null;

        try {
            tmp = ArrayList.class.getDeclaredField("size");
        } catch (Throwable e) {
            // ignore.
        }

        listSizeField = tmp != null && tmp.getType().equals(int.class) ? tmp : null;

        if (listSizeField != null) {
            listSizeField.setAccessible(true);
        }
    }

    static final String[] charStringCache = new String[128];
    static final int intStringCacheLow = -1001;
    static final int intStringCacheHigh = 10001;
    static final String[] intStringCache = new String[intStringCacheHigh - intStringCacheLow];
    static final Map<String, Integer> stringIntCache = new HashMap<>((int) (intStringCache.length * 1.5));

    static {
        for (int i = 0, j = intStringCacheLow, len = intStringCache.length; i < len; i++, j++) {
            intStringCache[i] = Integer.valueOf(j).toString();
            stringIntCache.put(intStringCache[i], j);
        }

        for (int i = 0; i < charStringCache.length; i++) {
            charStringCache[i] = String.valueOf((char) i);
        }
    }

    private static final int MIN_SIZE_FOR_COPY_ALL = 9;

    /**
     * Constructor for
     */
    private N() {
        // no instance();
    }

    @SuppressWarnings("unchecked")
    public static <T> Type<T> typeOf(final String typeName) {
        if (typeName == null) {
            return null;
        }

        Type<?> type = nameTypePool.get(typeName);

        if (type == null) {
            type = TypeFactory.getType(typeName);

            nameTypePool.put(typeName, type);
        }

        return (Type<T>) type;
    }

    @SuppressWarnings("unchecked")
    public static <T> Type<T> typeOf(final Class<?> cls) {
        if (cls == null) {
            return null;
        }

        Type<?> type = clsTypePool.get(cls);

        if (type == null) {
            type = TypeFactory.getType(cls);
            clsTypePool.put(cls, type);
        }

        return (Type<T>) type;
    }

    /**
     *
     * @param targetClass
     * @param str
     * @return the default value of the specified <code>targetClass</code> if the specified string is null.
     */
    @SuppressWarnings("unchecked")
    public static <T> T valueOf(final Class<? extends T> targetClass, final String str) {
        return (str == null) ? defaultValueOf(targetClass) : (T) N.typeOf(targetClass).valueOf(str);
    }

    @SuppressWarnings("unchecked")
    public static <T> T defaultValueOf(final Class<T> cls) {
        return (T) N.typeOf(cls).defaultValue();
    }

    public static boolean defaultIfNull(Boolean b) {
        if (b == null) {
            return false;
        }

        return b.booleanValue();
    }

    public static boolean defaultIfNull(Boolean b, boolean defaultForNull) {
        if (b == null) {
            return defaultForNull;
        }

        return b.booleanValue();
    }

    public static char defaultIfNull(Character c) {
        if (c == null) {
            return N.CHAR_0;
        }

        return c.charValue();
    }

    public static char defaultIfNull(Character c, char defaultForNull) {
        if (c == null) {
            return defaultForNull;
        }

        return c.charValue();
    }

    public static byte defaultIfNull(Byte b) {
        if (b == null) {
            return (byte) 0;
        }

        return b.byteValue();
    }

    public static byte defaultIfNull(Byte b, byte defaultForNull) {
        if (b == null) {
            return defaultForNull;
        }

        return b.byteValue();
    }

    public static short defaultIfNull(Short b) {
        if (b == null) {
            return (short) 0;
        }

        return b.shortValue();
    }

    public static short defaultIfNull(Short b, short defaultForNull) {
        if (b == null) {
            return defaultForNull;
        }

        return b.shortValue();
    }

    public static int defaultIfNull(Integer b) {
        if (b == null) {
            return 0;
        }

        return b.intValue();
    }

    public static int defaultIfNull(Integer b, int defaultForNull) {
        if (b == null) {
            return defaultForNull;
        }

        return b.intValue();
    }

    public static long defaultIfNull(Long b) {
        if (b == null) {
            return 0;
        }

        return b.longValue();
    }

    public static long defaultIfNull(Long b, long defaultForNull) {
        if (b == null) {
            return defaultForNull;
        }

        return b.longValue();
    }

    public static float defaultIfNull(Float b) {
        if (b == null) {
            return 0;
        }

        return b.floatValue();
    }

    public static float defaultIfNull(Float b, float defaultForNull) {
        if (b == null) {
            return defaultForNull;
        }

        return b.floatValue();
    }

    public static double defaultIfNull(Double b) {
        if (b == null) {
            return 0;
        }

        return b.doubleValue();
    }

    public static double defaultIfNull(Double b, double defaultForNull) {
        if (b == null) {
            return defaultForNull;
        }

        return b.doubleValue();
    }

    public static <T> T defaultIfNull(final T obj, final T defaultForNull) {
        return obj == null ? defaultForNull : obj;
    }

    @SuppressWarnings("unchecked")
    //    @SafeVarargs
    //    public static <T> List<Type<T>> typeOf(final Class<?>... classes) {
    //        if (N.isNullOrEmpty(classes)) {
    //            return new ArrayList<>();
    //        }
    //
    //        final List<Type<T>> result = new ArrayList<>(classes.length);
    //
    //        for (int i = 0, len = classes.length; i < len; i++) {
    //            result.add((Type<T>) typeOf(classes[i]));
    //        }
    //
    //        return result;
    //    }
    //
    //    @SuppressWarnings("unchecked")
    //    public static <T> List<Type<T>> typeOf(final Collection<? extends Class<?>> classes) {
    //        final List<Type<T>> result = new ArrayList<>(classes.size());
    //
    //        for (Class<?> cls : classes) {
    //            result.add((Type<T>) typeOf(cls));
    //        }
    //
    //        return result;
    //    }

    public static String stringOf(final boolean val) {
        return String.valueOf(val);
    }

    public static String stringOf(final char val) {
        if (val < 128) {
            return charStringCache[val];
        }

        return String.valueOf(val);
    }

    public static String stringOf(final byte val) {
        if (val > intStringCacheLow && val < intStringCacheHigh) {
            return intStringCache[val - intStringCacheLow];
        }

        return String.valueOf(val);
    }

    public static String stringOf(final short val) {
        if (val > intStringCacheLow && val < intStringCacheHigh) {
            return intStringCache[val - intStringCacheLow];
        }

        return String.valueOf(val);
    }

    public static String stringOf(final int val) {
        if (val > intStringCacheLow && val < intStringCacheHigh) {
            return intStringCache[val - intStringCacheLow];
        }

        return String.valueOf(val);
    }

    public static String stringOf(final long val) {
        if (val > intStringCacheLow && val < intStringCacheHigh) {
            return intStringCache[(int) (val - intStringCacheLow)];
        }

        return String.valueOf(val);
    }

    public static String stringOf(final float val) {
        return String.valueOf(val);
    }

    public static String stringOf(final double val) {
        return String.valueOf(val);
    }

    /**
     *
     * @param obj
     * @return <code>null</code> if the specified object is null.
     */
    public static String stringOf(final Object obj) {
        return (obj == null) ? null : N.typeOf(obj.getClass()).stringOf(obj);
    }

    public static <E extends Enum<E>> List<E> enumListOf(final Class<E> enumClass) {
        List<E> enumList = (List<E>) enumListPool.get(enumClass);

        if (enumList == null) {
            enumList = ImmutableList.of(N.asList(enumClass.getEnumConstants()));

            enumListPool.put(enumClass, enumList);
        }

        return enumList;
    }

    public static <E extends Enum<E>> Set<E> enumSetOf(final Class<E> enumClass) {
        Set<E> enumSet = (Set<E>) enumSetPool.get(enumClass);

        if (enumSet == null) {
            enumSet = ImmutableSet.of(EnumSet.allOf(enumClass));

            enumSetPool.put(enumClass, enumSet);
        }

        return enumSet;
    }

    public static <E extends Enum<E>> BiMap<E, String> enumMapOf(final Class<E> enumClass) {
        BiMap<E, String> enumMap = (BiMap<E, String>) enumMapPool.get(enumClass);

        if (enumMap == null) {
            final EnumMap<E, String> keyMap = new EnumMap<>(enumClass);
            final Map<String, E> valueMap = new HashMap<>();

            for (final E e : enumClass.getEnumConstants()) {
                keyMap.put(e, e.name());
                valueMap.put(e.name(), e);
            }

            enumMap = new BiMap<>(ImmutableMap.of(keyMap), ImmutableMap.of(valueMap));

            enumMapPool.put(enumClass, enumMap);
        }

        return enumMap;
    }

    /**
     * Method newInstance.
     *
     * @param cls
     * @return T
     */
    public static <T> T newInstance(final Class<T> cls) {
        if (Modifier.isAbstract(cls.getModifiers())) {
            if (cls.equals(Map.class)) {
                return (T) new HashMap<>();
            } else if (cls.equals(List.class)) {
                return (T) new ArrayList<>();
            } else if (cls.equals(Set.class)) {
                return (T) new HashSet<>();
            } else if (cls.equals(Queue.class)) {
                return (T) new LinkedList<>();
            } else if (cls.equals(Deque.class)) {
                return (T) new LinkedList<>();
            } else if (cls.equals(SortedSet.class) || cls.equals(NavigableSet.class)) {
                return (T) new TreeSet<>();
            } else if (cls.equals(SortedMap.class) || cls.equals(NavigableMap.class)) {
                return (T) new TreeMap<>();
            }
        }

        try {
            if (Modifier.isStatic(cls.getModifiers()) == false && (cls.isAnonymousClass() || cls.isMemberClass())) {
                // http://stackoverflow.com/questions/2097982/is-it-possible-to-create-an-instance-of-nested-class-using-java-reflection

                final List<Class<?>> toInstantiate = new ArrayList<>();
                Class<?> parent = cls.getEnclosingClass();

                do {
                    toInstantiate.add(parent);
                    parent = parent.getEnclosingClass();
                } while (parent != null && Modifier.isStatic(parent.getModifiers()) == false && (parent.isAnonymousClass() || parent.isMemberClass()));

                if (parent != null) {
                    toInstantiate.add(parent);
                }

                N.reverse(toInstantiate);

                Object instance = null;
                for (Class<?> current : toInstantiate) {
                    instance = instance == null ? invoke(ClassUtil.getDeclaredConstructor(current))
                            : invoke(ClassUtil.getDeclaredConstructor(current, instance.getClass()), instance);
                }

                return invoke(ClassUtil.getDeclaredConstructor(cls, instance.getClass()), instance);
            } else {
                final Constructor<T> constructor = ClassUtil.getDeclaredConstructor(cls);

                if (constructor == null) {
                    throw new IllegalArgumentException("No default constructor found in class: " + ClassUtil.getCanonicalClassName(cls));
                }

                return invoke(constructor);
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw N.toRuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T invoke(final Constructor<T> c, final Object... args)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (c.isAccessible() == false) {
            c.setAccessible(true);
        }

        return c.newInstance(args);
    }

    public static <T> T newProxyInstance(final Class<T> interfaceClass, final InvocationHandler h) {
        return newProxyInstance(N.asArray(interfaceClass), h);
    }

    /**
     * Refer to {@code java.lang.reflect}
     *
     * @param interfaceClasses
     * @param h
     * @return
     */
    public static <T> T newProxyInstance(final Class<?>[] interfaceClasses, final InvocationHandler h) {
        return (T) Proxy.newProxyInstance(N.class.getClassLoader(), interfaceClasses, h);
    }

    /**
     * Method newArray.
     *
     * @param componentType
     * @param length
     * @return T[]
     */
    @SuppressWarnings("unchecked")
    public static <T> T newArray(final Class<?> componentType, final int length) {
        //        if (length == 0) {
        //            final Object result = CLASS_EMPTY_ARRAY.get(componentType);
        //
        //            if (result != null) {
        //                return (T) result;
        //            }
        //        }

        return (T) Array.newInstance(componentType, length);
    }

    @SuppressWarnings("unchecked")
    public static <T> T newEntity(final Class<T> cls) {
        return newEntity(cls, null);
    }

    public static <T> T newEntity(final Class<T> cls, final String entityName) {
        if (MapEntity.class.isAssignableFrom(cls)) {
            return (T) asMapEntity(entityName);
        }

        final Class<?> enclosingClass = ClassUtil.getEnclosingClass(cls);

        if (enclosingClass == null || Modifier.isStatic(cls.getModifiers())) {
            return newInstance(cls);
        } else {
            return ClassUtil.invokeConstructor(ClassUtil.getDeclaredConstructor(cls, enclosingClass), newInstance(enclosingClass));
        }
    }

    /**
     * Returns a set backed by the specified map.
     *
     * @param map the backing map
     * @return the set backed by the map
     * @see Collections#newSetFromMap(Map)
     */
    public static <E> Set<E> newSetFromMap(final Map<E, Boolean> map) {
        return Collections.newSetFromMap(map);
    }

    public static int initHashCapacity(final int size) {
        N.checkArgNotNegative(size, "size");

        if (size == 0) {
            return 0;
        }

        int res = size < MAX_HASH_LENGTH ? (int) (size * 1.25) + 1 : MAX_ARRAY_SIZE;

        switch (res / 64) {
            case 0:
            case 1:
                return res;

            case 2:
            case 3:
            case 4:
                return 128;

            default:
                return 256;
        }
    }

    public static <T> ArrayList<T> newArrayList() {
        return new ArrayList<>();
    }

    public static <T> ArrayList<T> newArrayList(int initialCapacity) {
        return new ArrayList<>(initialCapacity);
    }

    public static <T> ArrayList<T> newArrayList(Collection<? extends T> c) {
        return N.isNullOrEmpty(c) ? new ArrayList<T>() : new ArrayList<T>(c);
    }

    public static <T> LinkedList<T> newLinkedList() {
        return new LinkedList<>();
    }

    public static <T> LinkedList<T> newLinkedList(Collection<? extends T> c) {
        return N.isNullOrEmpty(c) ? new LinkedList<T>() : new LinkedList<>(c);
    }

    public static <T> HashSet<T> newHashSet() {
        return new HashSet<>();
    }

    /**
     *
     * @param initialCapacity
     * @return
     */
    public static <T> HashSet<T> newHashSet(int initialCapacity) {
        return new HashSet<>(initialCapacity);
    }

    public static <T> HashSet<T> newHashSet(Collection<? extends T> c) {
        return N.isNullOrEmpty(c) ? new HashSet<T>() : new HashSet<>(c);
    }

    public static <T> LinkedHashSet<T> newLinkedHashSet() {
        return new LinkedHashSet<>();
    }

    /**
     *
     * @param initialCapacity
     * @return
     */
    public static <T> LinkedHashSet<T> newLinkedHashSet(int initialCapacity) {
        return new LinkedHashSet<>(initialCapacity);
    }

    public static <T> LinkedHashSet<T> newLinkedHashSet(Collection<? extends T> c) {
        return N.isNullOrEmpty(c) ? new LinkedHashSet<T>() : new LinkedHashSet<>(c);
    }

    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> TreeSet<T> newTreeSet() {
        return new TreeSet<>();
    }

    public static <T> TreeSet<T> newTreeSet(Comparator<? super T> comparator) {
        return new TreeSet<>(comparator);
    }

    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> TreeSet<T> newTreeSet(Collection<? extends T> c) {
        return N.isNullOrEmpty(c) ? new TreeSet<T>() : new TreeSet<>(c);
    }

    public static <T> TreeSet<T> newTreeSet(SortedSet<T> c) {
        return N.isNullOrEmpty(c) ? new TreeSet<T>() : new TreeSet<>(c);
    }

    public static <T> Multiset<T> newMultiset() {
        return new Multiset<>();
    }

    public static <T> Multiset<T> newMultiset(final int initialCapacity) {
        return new Multiset<>(initialCapacity);
    }

    @SuppressWarnings("rawtypes")
    public static <T> Multiset<T> newMultiset(final Class<? extends Map> valueMapType) {
        return new Multiset<>(valueMapType);
    }

    public static <T> Multiset<T> newMultiset(final Supplier<? extends Map<T, ?>> mapSupplier) {
        return new Multiset<>(mapSupplier);
    }

    public static <T> Multiset<T> newMultiset(final Collection<? extends T> c) {
        return new Multiset<>(c);
    }

    public static <T> ArrayDeque<T> newArrayDeque() {
        return new ArrayDeque<>();
    }

    /**
     * Constructs an empty array deque with an initial capacity sufficient to hold the specified number of elements.
     *
     * @param numElements lower bound on initial capacity of the deque.
     * @return
     */
    public static <T> ArrayDeque<T> newArrayDeque(int numElements) {
        return new ArrayDeque<>(numElements);
    }

    /**
     * Constructs a deque containing the elements of the specified collection, in the order they are returned by the collection's iterator.
     *
     * @param c
     * @return
     */
    public static <E> ArrayDeque<E> newArrayDeque(Collection<? extends E> c) {
        return new ArrayDeque<>(c);
    }

    public static <K, V> Map.Entry<K, V> newEntry(K key, V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    public static <K, V> ImmutableEntry<K, V> newImmutableEntry(K key, V value) {
        return new ImmutableEntry<>(key, value);
    }

    public static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<>();
    }

    /**
     *
     * @param initialCapacity
     * @return
     */
    public static <K, V> HashMap<K, V> newHashMap(int initialCapacity) {
        return new HashMap<>(initialCapacity);
    }

    public static <K, V> HashMap<K, V> newHashMap(Map<? extends K, ? extends V> m) {
        return N.isNullOrEmpty(m) ? new HashMap<K, V>() : new HashMap<K, V>(m);
    }

    public static <K, V, E extends Exception> HashMap<K, V> newHashMap(final Collection<? extends V> c, final Try.Function<? super V, ? extends K, E> keyMapper)
            throws E {
        N.checkArgNotNull(keyMapper);

        if (isNullOrEmpty(c)) {
            return new HashMap<>();
        }

        final HashMap<K, V> result = new HashMap<>(N.initHashCapacity(c.size()));

        for (V v : c) {
            result.put(keyMapper.apply(v), v);
        }

        return result;
    }

    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap() {
        return new LinkedHashMap<>();
    }

    /**
     *
     * @param initialCapacity
     * @return
     */
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(int initialCapacity) {
        return new LinkedHashMap<>(initialCapacity);
    }

    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(Map<? extends K, ? extends V> m) {
        return N.isNullOrEmpty(m) ? new LinkedHashMap<K, V>() : new LinkedHashMap<K, V>(m);
    }

    public static <K, V, E extends Exception> LinkedHashMap<K, V> newLinkedHashMap(final Collection<? extends V> c,
            final Try.Function<? super V, ? extends K, E> keyMapper) throws E {
        N.checkArgNotNull(keyMapper);

        if (isNullOrEmpty(c)) {
            return new LinkedHashMap<>();
        }

        final LinkedHashMap<K, V> result = new LinkedHashMap<>(N.initHashCapacity(c.size()));

        for (V v : c) {
            result.put(keyMapper.apply(v), v);
        }

        return result;
    }

    @SuppressWarnings("rawtypes")
    public static <K extends Comparable, V> TreeMap<K, V> newTreeMap() {
        return new TreeMap<>();
    }

    public static <C, K extends C, V> TreeMap<K, V> newTreeMap(Comparator<C> comparator) {
        return new TreeMap<>(comparator);
    }

    @SuppressWarnings("rawtypes")
    public static <K extends Comparable, V> TreeMap<K, V> newTreeMap(Map<? extends K, ? extends V> m) {
        return N.isNullOrEmpty(m) ? new TreeMap<K, V>() : new TreeMap<K, V>(m);
    }

    public static <K, V> TreeMap<K, V> newTreeMap(SortedMap<K, ? extends V> m) {
        return N.isNullOrEmpty(m) ? new TreeMap<K, V>() : new TreeMap<K, V>(m);
    }

    public static <K, V> IdentityHashMap<K, V> newIdentityHashMap() {
        return new IdentityHashMap<>();
    }

    /**
     *
     * @param initialCapacity
     * @return
     */
    public static <K, V> IdentityHashMap<K, V> newIdentityHashMap(int initialCapacity) {
        return new IdentityHashMap<>(initialCapacity);
    }

    public static <K, V> IdentityHashMap<K, V> newIdentityHashMap(Map<? extends K, ? extends V> m) {
        return N.isNullOrEmpty(m) ? new IdentityHashMap<K, V>() : new IdentityHashMap<K, V>(m);
    }

    public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap() {
        return new ConcurrentHashMap<>();
    }

    /**
     *
     * @param initCapacity the initial capacity of new HashSet
     * @return
     */
    public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap(int initialCapacity) {
        return new ConcurrentHashMap<>(initialCapacity);
    }

    public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap(Map<? extends K, ? extends V> m) {
        return N.isNullOrEmpty(m) ? new ConcurrentHashMap<K, V>() : new ConcurrentHashMap<K, V>(m);
    }

    public static <K, V> BiMap<K, V> newBiMap() {
        return new BiMap<>();
    }

    public static <K, V> BiMap<K, V> newBiMap(int initialCapacity) {
        return new BiMap<>(initialCapacity);
    }

    public static <K, V> BiMap<K, V> newBiMap(int initialCapacity, float loadFactor) {
        return new BiMap<>(initialCapacity, loadFactor);
    }

    @SuppressWarnings("rawtypes")
    public static <K, V> BiMap<K, V> newBiMap(final Class<? extends Map> keyMapType, final Class<? extends Map> valueMapType) {
        return new BiMap<>(keyMapType, valueMapType);
    }

    public static <K, V> BiMap<K, V> newBiMap(final Supplier<? extends Map<K, V>> keyMapSupplier, final Supplier<? extends Map<V, K>> valueMapSupplier) {
        return new BiMap<>(keyMapSupplier, valueMapSupplier);
    }

    public static <K, E, V extends Collection<E>> Multimap<K, E, V> newMultimap(final Supplier<? extends Map<K, V>> mapSupplier,
            final Supplier<? extends V> valueSupplier) {
        return new Multimap<>(mapSupplier, valueSupplier);
    }

    public static <K, E> ListMultimap<K, E> newListMultimap() {
        return new ListMultimap<>();
    }

    public static <K, E> ListMultimap<K, E> newListMultimap(final int initialCapacity) {
        return new ListMultimap<>(initialCapacity);
    }

    public static <K, E> ListMultimap<K, E> newListMultimap(final Map<? extends K, ? extends E> m) {
        final ListMultimap<K, E> multiMap = newListMultimap();

        multiMap.putAll(m);

        return multiMap;
    }

    public static <K, E> ListMultimap<K, E> newListLinkedMultimap() {
        return new ListMultimap<>(LinkedHashMap.class, ArrayList.class);
    }

    public static <K, E> ListMultimap<K, E> newListLinkedMultimap(final int initialCapacity) {
        return new ListMultimap<>(new LinkedHashMap<K, List<E>>(initialCapacity), ArrayList.class);
    }

    public static <K, E> ListMultimap<K, E> newListLinkedMultimap(final Map<? extends K, ? extends E> m) {
        final ListMultimap<K, E> multiMap = new ListMultimap<>(new LinkedHashMap<K, List<E>>(), ArrayList.class);

        multiMap.putAll(m);

        return multiMap;
    }

    public static <K extends Comparable<K>, E> ListMultimap<K, E> newListSortedMultimap() {
        return new ListMultimap<>(new TreeMap<K, List<E>>(), ArrayList.class);
    }

    public static <K extends Comparable<K>, E> ListMultimap<K, E> newListSortedMultimap(final Map<? extends K, ? extends E> m) {
        final ListMultimap<K, E> multiMap = new ListMultimap<>(new TreeMap<K, List<E>>(), ArrayList.class);

        multiMap.putAll(m);

        return multiMap;
    }

    @SuppressWarnings("rawtypes")
    public static <K, E> ListMultimap<K, E> newListMultimap(final Class<? extends Map> mapType) {
        return new ListMultimap<>(mapType, ArrayList.class);
    }

    @SuppressWarnings("rawtypes")
    public static <K, E> ListMultimap<K, E> newListMultimap(final Class<? extends Map> mapType, final Class<? extends List> valueType) {
        return new ListMultimap<>(mapType, valueType);
    }

    public static <K, E> ListMultimap<K, E> newListMultimap(final Supplier<? extends Map<K, List<E>>> mapSupplier,
            final Supplier<? extends List<E>> valueSupplier) {
        return new ListMultimap<>(mapSupplier, valueSupplier);
    }

    public static <K, E> SetMultimap<K, E> newSetMultimap() {
        return new SetMultimap<>();
    }

    public static <K, E> SetMultimap<K, E> newSetMultimap(final int initialCapacity) {
        return new SetMultimap<>(initialCapacity);
    }

    public static <K, E> SetMultimap<K, E> newSetMultimap(final Map<? extends K, ? extends E> m) {
        final SetMultimap<K, E> multiMap = newSetMultimap();

        multiMap.putAll(m);

        return multiMap;
    }

    public static <K, E> SetMultimap<K, E> newSetLinkedMultimap() {
        return new SetMultimap<>(LinkedHashMap.class, HashSet.class);
    }

    public static <K, E> SetMultimap<K, E> newSetLinkedMultimap(final int initialCapacity) {
        return new SetMultimap<>(new LinkedHashMap<K, Set<E>>(initialCapacity), HashSet.class);
    }

    public static <K, E> SetMultimap<K, E> newSetLinkedMultimap(final Map<? extends K, ? extends E> m) {
        final SetMultimap<K, E> multiMap = new SetMultimap<>(new LinkedHashMap<K, Set<E>>(), HashSet.class);

        multiMap.putAll(m);

        return multiMap;
    }

    public static <K extends Comparable<K>, E> SetMultimap<K, E> newSetSortedMultimap() {
        return new SetMultimap<>(new TreeMap<K, Set<E>>(), HashSet.class);
    }

    public static <K extends Comparable<K>, E> SetMultimap<K, E> newSetSortedMultimap(final Map<? extends K, ? extends E> m) {
        final SetMultimap<K, E> multiMap = new SetMultimap<>(new TreeMap<K, Set<E>>(), HashSet.class);

        multiMap.putAll(m);

        return multiMap;
    }

    @SuppressWarnings("rawtypes")
    public static <K, E> SetMultimap<K, E> newSetMultimap(final Class<? extends Map> mapType) {
        return new SetMultimap<>(mapType, HashSet.class);
    }

    @SuppressWarnings("rawtypes")
    public static <K, E> SetMultimap<K, E> newSetMultimap(final Class<? extends Map> mapType, final Class<? extends Set> valueType) {
        return new SetMultimap<>(mapType, valueType);
    }

    public static <K, E> SetMultimap<K, E> newSetMultimap(final Supplier<? extends Map<K, Set<E>>> mapSupplier,
            final Supplier<? extends Set<E>> valueSupplier) {
        return new SetMultimap<>(mapSupplier, valueSupplier);
    }

    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    static final int MAX_HASH_LENGTH = (int) (MAX_ARRAY_SIZE / 1.25) - 1;

    public static DataSet newEmptyDataSet() {
        return new RowDataSet(new ArrayList<String>(), new ArrayList<List<Object>>());
    }

    public static DataSet newEmptyDataSet(final Collection<String> columnNames) {
        if (N.isNullOrEmpty(columnNames)) {
            return newEmptyDataSet();
        }

        final List<List<Object>> columnList = new ArrayList<>(columnNames.size());

        for (int i = 0, size = columnNames.size(); i < size; i++) {
            columnList.add(new ArrayList<>(0));
        }

        return new RowDataSet(new ArrayList<String>(columnNames), columnList);
    }

    /**
     * Convert the specified Map to a two columns <code>DataSet</code>: one column is for keys and one column is for values
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

        final List<String> columnNameList = N.asList(keyColumnName, valueColumnName);
        final List<List<Object>> columnList = N.asList(keyColumn, valueColumn);

        return newDataSet(columnNameList, columnList);
    }

    /**
     * Converts a list of row(which can be: Map/Entity) into a <code>DataSet</code>.
     *
     * @param rows list of row which can be: Map/Entity.
     */
    public static <T> DataSet newDataSet(final Collection<T> rows) {
        return newDataSet(null, rows);
    }

    /**
     * Converts a list of row(which can be: Map/Entity/Array/Collection) into a <code>DataSet</code>.
     *
     * @param columnNames
     * @param rows list of row which can be: Map/Entity or Array/Collection if {@code columnNames} is null or empty.
     * @return
     */
    @SuppressWarnings("deprecation")
    public static <T> DataSet newDataSet(Collection<String> columnNames, Collection<T> rowList) {
        if (N.isNullOrEmpty(columnNames) && N.isNullOrEmpty(rowList)) {
            // throw new IllegalArgumentException("Column name list and row list can not be both null or empty");
            return N.newEmptyDataSet();
        } else if (N.isNullOrEmpty(rowList)) {
            return N.newEmptyDataSet(columnNames);
        }

        final int rowSize = rowList.size();

        if (N.isNullOrEmpty(columnNames)) {
            final Set<String> columnNameSet = new LinkedHashSet<>();
            Set<Class<?>> clsSet = null;
            Map<Class<?>, Set<String>> clsSignedPropNameSetMap = new HashMap<>();

            Class<?> cls = null;
            Type<?> type = null;

            for (Object e : rowList) {
                if (e == null) {
                    continue;
                }

                cls = e.getClass();
                type = N.typeOf(cls);

                if (type.isMap()) {
                    columnNameSet.addAll(((Map<String, Object>) e).keySet());
                } else if (type.isEntity()) {
                    if (e instanceof DirtyMarker) {
                        Set<String> clsSignedPropNameSet = clsSignedPropNameSetMap.get(cls);

                        if (clsSignedPropNameSet == null) {
                            clsSignedPropNameSet = new HashSet<>();
                            clsSignedPropNameSetMap.put(cls, clsSignedPropNameSet);
                        }

                        Method method = null;

                        for (String signedPropName : ((DirtyMarker) e).signedPropNames()) {
                            if (clsSignedPropNameSet.add(signedPropName) == false) {
                                continue;
                            }

                            method = ClassUtil.getPropGetMethod(cls, signedPropName);

                            if (method != null) {
                                columnNameSet.add(ClassUtil.getPropNameByMethod(method));
                            }
                        }
                    } else {
                        if (clsSet == null) {
                            clsSet = new HashSet<>();
                        }

                        if (clsSet.contains(cls)) {
                            continue;
                        }

                        columnNameSet.addAll(ClassUtil.checkPropGetMethodList(cls).keySet());
                        clsSet.add(cls);
                    }
                } else {
                    throw new IllegalArgumentException("'columnNameList' is required if the sepcified row type is not Entity or Map");
                }
            }

            // re-order column.
            for (Map.Entry<Class<?>, Set<String>> entry : clsSignedPropNameSetMap.entrySet()) {
                final List<String> intersecion = N.intersection(ClassUtil.getPropGetMethodList(entry.getKey()).keySet(), columnNameSet);
                columnNameSet.removeAll(intersecion);
                columnNameSet.addAll(intersecion);
            }

            columnNames = new ArrayList<>(columnNameSet);

            if (N.isNullOrEmpty(columnNames)) {
                throw new IllegalArgumentException("Column name list can not be obtained from row list because it's empty or null");
            }
        }

        final int columnCount = columnNames.size();
        final List<String> columnNameList = new ArrayList<>(columnNames);
        final List<List<Object>> columnList = new ArrayList<>(columnCount);
        for (int i = 0; i < columnCount; i++) {
            columnList.add(new ArrayList<>(rowSize));
        }

        Type<?> type = null;
        for (Object e : rowList) {
            if (e == null) {
                for (int i = 0; i < columnCount; i++) {
                    columnList.get(i).add(null);
                }

                continue;
            }

            type = N.typeOf(e.getClass());

            if (type.isMap()) {
                Map<String, Object> props = (Map<String, Object>) e;

                for (int i = 0; i < columnCount; i++) {
                    columnList.get(i).add(props.get(columnNameList.get(i)));
                }
            } else if (type.isEntity()) {
                Class<?> cls = e.getClass();

                Method method = null;
                for (int i = 0; i < columnCount; i++) {
                    method = ClassUtil.getPropGetMethod(cls, columnNameList.get(i));

                    if (method == null) {
                        columnList.get(i).add(null);
                    } else {
                        columnList.get(i).add(ClassUtil.getPropValue(e, method));
                    }
                }
            } else if (type.isArray()) {
                if (type.isPrimitiveArray()) {
                    for (int i = 0; i < columnCount; i++) {
                        columnList.get(i).add(Array.get(e, i));
                    }
                } else {
                    Object[] array = (Object[]) e;

                    for (int i = 0; i < columnCount; i++) {
                        columnList.get(i).add(array[i]);
                    }
                }
            } else if (type.isCollection()) {
                final Iterator<Object> it = ((Collection<Object>) e).iterator();

                for (int i = 0; i < columnCount; i++) {
                    columnList.get(i).add(it.next());
                }
            } else {
                throw new IllegalArgumentException(
                        "Unsupported row type: " + ClassUtil.getCanonicalClassName(e.getClass()) + ". Only array, collection, map and entity are supported");
            }
        }

        return new RowDataSet(columnNameList, columnList);
    }

    @SuppressWarnings("deprecation")
    static MapEntity asMapEntity(final String entityName, final Object... props) {
        final MapEntity mapEntity = new MapEntity(entityName);

        if (N.isNullOrEmpty(props)) {
            return mapEntity;
        }

        if (props.length == 1) {
            if (props[0] instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) props[0];
                for (String key : map.keySet()) {
                    mapEntity.set(key, map.get(key));
                }
            } else if (ClassUtil.isEntity(props[0].getClass())) {
                Object anEntity = props[0];
                if (anEntity instanceof DirtyMarker) {
                    Class<?> entityClass = anEntity.getClass();
                    Method propGetMethod = null;
                    for (String propName : ((DirtyMarker) anEntity).signedPropNames()) {
                        propGetMethod = ClassUtil.getPropGetMethod(entityClass, propName);
                        propName = ClassUtil.getPropNameByMethod(propGetMethod);
                        mapEntity.set(propName, ClassUtil.getPropValue(anEntity, propGetMethod));
                    }
                } else {
                    final Map<String, Method> getterMethodList = ClassUtil.checkPropGetMethodList(anEntity.getClass());

                    for (Map.Entry<String, Method> entry : getterMethodList.entrySet()) {
                        mapEntity.set(entry.getKey(), ClassUtil.getPropValue(anEntity, entry.getValue()));
                    }
                }
            } else {
                throw new IllegalArgumentException(
                        "The parameters must be the pairs of property name and value, or Map, or an entity class with getter/setter methods.");
            }
        } else {
            if ((props.length % 2) != 0) {
                throw new IllegalArgumentException(
                        "The parameters must be the pairs of property name and value, or Map, or an entity class with getter/setter methods.");
            }

            for (int i = 0; i < props.length; i++) {
                mapEntity.set((String) props[i], props[++i]);
            }
        }

        return mapEntity;
    }

    //    /**
    //     * Wrap the specified map with a unmodifiable BiMap.
    //     *
    //     * Note: To avoid the side-effects in the initialization of auto-generated interface *PNL/*CNL class,
    //     * This method just return null if the specified keyMap has duplicated values, instead of throwing exception.
    //     *
    //     * @param keyMap
    //     * @return null if the specified <code>keyMap</code> has duplicated values
    //     */
    //    @Internal
    //    public static <K, V> BiMap<K, V> newImmutableBiMapForInterface(final Map<? extends K, ? extends V> keyMap) {
    //        final Map<V, K> valueMap = new LinkedHashMap<>();
    //
    //        for (Map.Entry<? extends K, ? extends V> entry : keyMap.entrySet()) {
    //            valueMap.put(entry.getValue(), entry.getKey());
    //        }
    //
    //        if (valueMap.size() != keyMap.size()) {
    //            return null;
    //        }
    //
    //        return new BiMap<>(ImmutableMap.of(keyMap), ImmutableMap.of(valueMap));
    //    }

    //    /**
    //     *
    //     * @param arrayClass
    //     * @param c
    //     * @return
    //     * @deprecated replaced by {@code N#toArray(Class, Collection)}
    //     */
    //    @Deprecated
    //    public static <T> T collection2Array(final Class<T> arrayClass, final Collection<?> c) {
    //        if (c == null) {
    //            return N.newArray(arrayClass.getComponentType(), 0);
    //        }
    //
    //        return (T) N.typeOf(arrayClass).collection2Array(c);
    //    }
    //
    //    /**
    //     *
    //     * @param a
    //     * @return
    //     * @deprecated replaced by {@code N#toList(Object[])}
    //     */
    //    @Deprecated
    //    public static <T> List<T> array2List(final Object a) {
    //        if (a == null) {
    //            return asList();
    //        }
    //
    //        final List<T> c = asList();
    //
    //        N.typeOf(a.getClass()).array2Collection(c, a);
    //
    //        return c;
    //    }
    //
    //    /**
    //     *
    //     * @param a
    //     * @return
    //     * @deprecated replaced by {@code N#toSet(Object[])}
    //     */
    //    @Deprecated
    //    public static <T> Set<T> array2Set(final Object a) {
    //        if (a == null) {
    //            return asSet();
    //        }
    //
    //        final Set<T> c = asSet();
    //
    //        N.typeOf(a.getClass()).array2Collection(c, a);
    //
    //        return c;
    //    }
    //
    //    /**
    //     * The input collection is returned
    //     * @param c
    //     * @param a
    //     * @return the input collection.
    //     * @deprecated replaced by {@code N#toCollection(Object[], IntFunction)}
    //     */
    //    @Deprecated
    //    @SuppressWarnings({ "unchecked" })
    //    public static <C extends Collection<?>> C array2Collection(final Object a, final IntFunction<? extends C> supplier) {
    //        if (a == null) {
    //            return supplier.apply(0);
    //        }
    //
    //        final C c = supplier.apply(Array.getLength(a));
    //
    //        N.typeOf(a.getClass()).array2Collection((Collection<?>) c, a);
    //
    //        return c;
    //    }

    /**
     *
     * @param map keys are column names, values are columns
     * @return
     */
    public static <C extends Collection<?>> DataSet newDataSet(final Map<String, C> map) {
        if (N.isNullOrEmpty(map)) {
            return N.newEmptyDataSet();
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

            if (N.notNullOrEmpty(v)) {
                column.addAll(v);
            }

            if (column.size() < maxColumnLen) {
                N.fill(column, column.size(), maxColumnLen, null);
            }

            columnList.add(column);
        }

        return new RowDataSet(columnNameList, columnList);
    }

    /**
     * Returns an empty array if the specified collection is null or empty.
     *
     * @param c
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Object[] toArray(final Collection<?> c) {
        if (N.isNullOrEmpty(c)) {
            return N.EMPTY_OBJECT_ARRAY;
        }

        return c.toArray(new Object[c.size()]);
    }

    @SuppressWarnings("rawtypes")
    public static Object[] toArray(final Collection<?> c, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, size(c));

        if (N.isNullOrEmpty(c)) {
            return N.EMPTY_OBJECT_ARRAY;
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

    public static <A, T extends A> A[] toArray(final Collection<T> c, final A[] a) {
        N.checkArgNotNull(a);

        if (N.isNullOrEmpty(c)) {
            return a;
        }

        return c.toArray(a);
    }

    public static <A, T extends A> A[] toArray(final Collection<T> c, final int fromIndex, final int toIndex, final A[] a) {
        N.checkFromToIndex(fromIndex, toIndex, size(c));
        N.checkArgNotNull(a);

        if (N.isNullOrEmpty(c)) {
            return a;
        } else if (fromIndex == 0 || toIndex == c.size()) {
            return c.toArray(a);
        } else if (c instanceof List) {
            return ((List<T>) c).subList(fromIndex, toIndex).toArray(a);
        } else {
            final A[] res = a.length >= toIndex - fromIndex ? a : (A[]) N.newArray(a.getClass().getComponentType(), toIndex - fromIndex);
            final Iterator<T> iter = c.iterator();
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

    public static <A, T extends A> A[] toArray(final Collection<T> c, final IntFunction<A[]> arraySupplier) {
        N.checkArgNotNull(arraySupplier);

        if (N.isNullOrEmpty(c)) {
            return arraySupplier.apply(0);
        }

        return toArray(c, arraySupplier);
    }

    public static <A, T extends A> A[] toArray(final Collection<T> c, final int fromIndex, final int toIndex, final IntFunction<A[]> arraySupplier) {
        N.checkArgNotNull(arraySupplier);
        N.checkFromToIndex(fromIndex, toIndex, size(c));

        if (N.isNullOrEmpty(c)) {
            return arraySupplier.apply(0);
        } else if (fromIndex == 0 || toIndex == c.size()) {
            return c.toArray(arraySupplier.apply(c.size()));
        } else if (c instanceof List) {
            return ((List<T>) c).subList(fromIndex, toIndex).toArray(arraySupplier.apply(toIndex - fromIndex));
        } else {
            final A[] res = arraySupplier.apply(toIndex - fromIndex);
            final Iterator<T> iter = c.iterator();
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

    public static <A, T extends A> A[] toArray(final Class<A[]> targetClass, final Collection<T> c) {
        N.checkArgNotNull(targetClass);

        if (N.isNullOrEmpty(c)) {
            return N.newArray(targetClass.getComponentType(), 0);
        }

        return c.toArray((A[]) N.newArray(targetClass.getComponentType(), c.size()));
    }

    public static <A, T extends A> A[] toArray(final Class<A[]> targetClass, final Collection<T> c, final int fromIndex, final int toIndex) {
        N.checkArgNotNull(targetClass);
        N.checkFromToIndex(fromIndex, toIndex, size(c));

        final A[] res = N.newArray(targetClass.getComponentType(), toIndex - fromIndex);

        if (N.isNullOrEmpty(c)) {
            return res;
        } else if (fromIndex == 0 || toIndex == c.size()) {
            return c.toArray(res);
        } else if (c instanceof List) {
            return ((List<T>) c).subList(fromIndex, toIndex).toArray(res);
        } else {
            final Iterator<T> iter = c.iterator();
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

    public static boolean[] toBooleanArray(final Collection<Boolean> c) {
        return toBooleanArray(c, false);
    }

    public static boolean[] toBooleanArray(final Collection<Boolean> c, final int fromIndex, final int toIndex) {
        return toBooleanArray(c, fromIndex, toIndex, false);
    }

    public static boolean[] toBooleanArray(final Collection<Boolean> c, final boolean defaultForNull) {
        return toBooleanArray(c, 0, size(c), defaultForNull);
    }

    public static boolean[] toBooleanArray(final Collection<Boolean> c, final int fromIndex, final int toIndex, final boolean defaultForNull) {
        N.checkFromToIndex(fromIndex, toIndex, size(c));

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

    public static char[] toCharArray(final Collection<Character> c) {
        return toCharArray(c, (char) 0);
    }

    public static char[] toCharArray(final Collection<Character> c, final int fromIndex, final int toIndex) {
        return toCharArray(c, fromIndex, toIndex, (char) 0);
    }

    public static char[] toCharArray(final Collection<Character> c, final char defaultForNull) {
        return toCharArray(c, 0, size(c), defaultForNull);
    }

    public static char[] toCharArray(final Collection<Character> c, final int fromIndex, final int toIndex, final char defaultForNull) {
        N.checkFromToIndex(fromIndex, toIndex, size(c));

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

    public static byte[] toByteArray(final Collection<? extends Number> c) {
        return toByteArray(c, (byte) 0);
    }

    public static byte[] toByteArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex) {
        return toByteArray(c, fromIndex, toIndex, (byte) 0);
    }

    public static byte[] toByteArray(final Collection<? extends Number> c, final byte defaultForNull) {
        return toByteArray(c, 0, size(c), defaultForNull);
    }

    public static byte[] toByteArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex, final byte defaultForNull) {
        N.checkFromToIndex(fromIndex, toIndex, size(c));

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

    public static short[] toShortArray(final Collection<? extends Number> c) {
        return toShortArray(c, (short) 0);
    }

    public static short[] toShortArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex) {
        return toShortArray(c, fromIndex, toIndex, (short) 0);
    }

    public static short[] toShortArray(final Collection<? extends Number> c, final short defaultForNull) {
        return toShortArray(c, 0, size(c), defaultForNull);
    }

    public static short[] toShortArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex, final short defaultForNull) {
        N.checkFromToIndex(fromIndex, toIndex, size(c));

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

    public static int[] toIntArray(final Collection<? extends Number> c) {
        return toIntArray(c, 0);
    }

    public static int[] toIntArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex) {
        return toIntArray(c, fromIndex, toIndex, 0);
    }

    public static int[] toIntArray(final Collection<? extends Number> c, final int defaultForNull) {
        return toIntArray(c, 0, size(c), defaultForNull);
    }

    public static int[] toIntArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex, final int defaultForNull) {
        N.checkFromToIndex(fromIndex, toIndex, size(c));

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

    public static long[] toLongArray(final Collection<? extends Number> c) {
        return toLongArray(c, 0);
    }

    public static long[] toLongArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex) {
        return toLongArray(c, fromIndex, toIndex, 0);
    }

    public static long[] toLongArray(final Collection<? extends Number> c, final long defaultForNull) {
        return toLongArray(c, 0, size(c), defaultForNull);
    }

    public static long[] toLongArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex, final long defaultForNull) {
        N.checkFromToIndex(fromIndex, toIndex, size(c));

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

    public static float[] toFloatArray(final Collection<? extends Number> c) {
        return toFloatArray(c, 0);
    }

    public static float[] toFloatArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex) {
        return toFloatArray(c, fromIndex, toIndex, 0);
    }

    public static float[] toFloatArray(final Collection<? extends Number> c, final float defaultForNull) {
        return toFloatArray(c, 0, size(c), defaultForNull);
    }

    public static float[] toFloatArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex, final float defaultForNull) {
        N.checkFromToIndex(fromIndex, toIndex, size(c));

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
                    result[i] = val.floatValue();
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
                    result[i] = val.floatValue();
                }
            }
        }

        return result;
    }

    public static double[] toDoubleArray(final Collection<? extends Number> c) {
        return toDoubleArray(c, 0);
    }

    public static double[] toDoubleArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex) {
        return toDoubleArray(c, fromIndex, toIndex, 0);
    }

    public static double[] toDoubleArray(final Collection<? extends Number> c, final double defaultForNull) {
        return toDoubleArray(c, 0, size(c), defaultForNull);
    }

    public static double[] toDoubleArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex, final double defaultForNull) {
        N.checkFromToIndex(fromIndex, toIndex, size(c));

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
                    result[i] = val.doubleValue();
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
                    result[i] = val.doubleValue();
                }
            }
        }

        return result;
    }

    public static List<Boolean> toList(final boolean[] a) {
        return toList(a, 0, len(a));
    }

    public static List<Boolean> toList(final boolean[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final List<Boolean> result = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static List<Character> toList(final char[] a) {
        return toList(a, 0, len(a));
    }

    public static List<Character> toList(final char[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final List<Character> result = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static List<Byte> toList(final byte[] a) {
        return toList(a, 0, len(a));
    }

    public static List<Byte> toList(final byte[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final List<Byte> result = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static List<Short> toList(final short[] a) {
        return toList(a, 0, len(a));
    }

    public static List<Short> toList(final short[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final List<Short> result = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static List<Integer> toList(final int[] a) {
        return toList(a, 0, len(a));
    }

    public static List<Integer> toList(final int[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final List<Integer> result = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static List<Long> toList(final long[] a) {
        return toList(a, 0, len(a));
    }

    public static List<Long> toList(final long[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final List<Long> result = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static List<Float> toList(final float[] a) {
        return toList(a, 0, len(a));
    }

    public static List<Float> toList(final float[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final List<Float> result = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static List<Double> toList(final double[] a) {
        return toList(a, 0, len(a));
    }

    public static List<Double> toList(final double[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final List<Double> result = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static <T> List<T> toList(final T[] a) {
        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        return N.asList(a);
    }

    public static <T> List<T> toList(final T[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new ArrayList<>();
        } else if (fromIndex == 0 && toIndex == a.length) {
            return N.asList(a);
        }

        final List<T> result = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static Set<Boolean> toSet(final boolean[] a) {
        return toSet(a, 0, len(a));
    }

    public static Set<Boolean> toSet(final boolean[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new HashSet<>();
        }

        final Set<Boolean> result = new HashSet<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static Set<Character> toSet(final char[] a) {
        return toSet(a, 0, len(a));
    }

    public static Set<Character> toSet(final char[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new HashSet<>();
        }

        final Set<Character> result = new HashSet<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static Set<Byte> toSet(final byte[] a) {
        return toSet(a, 0, len(a));
    }

    public static Set<Byte> toSet(final byte[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new HashSet<>();
        }

        final Set<Byte> result = new HashSet<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static Set<Short> toSet(final short[] a) {
        return toSet(a, 0, len(a));
    }

    public static Set<Short> toSet(final short[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new HashSet<>();
        }

        final Set<Short> result = new HashSet<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static Set<Integer> toSet(final int[] a) {
        return toSet(a, 0, len(a));
    }

    public static Set<Integer> toSet(final int[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new HashSet<>();
        }

        final Set<Integer> result = new HashSet<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static Set<Long> toSet(final long[] a) {
        return toSet(a, 0, len(a));
    }

    public static Set<Long> toSet(final long[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new HashSet<>();
        }

        final Set<Long> result = new HashSet<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static Set<Float> toSet(final float[] a) {
        return toSet(a, 0, len(a));
    }

    public static Set<Float> toSet(final float[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new HashSet<>();
        }

        final Set<Float> result = new HashSet<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static Set<Double> toSet(final double[] a) {
        return toSet(a, 0, len(a));
    }

    public static Set<Double> toSet(final double[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new HashSet<>();
        }

        final Set<Double> result = new HashSet<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static <T> Set<T> toSet(final T[] a) {
        if (N.isNullOrEmpty(a)) {
            return new HashSet<>();
        }

        return N.asSet(a);
    }

    public static <T> Set<T> toSet(final T[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new HashSet<>();
        }

        final Set<T> result = new HashSet<>(N.initHashCapacity(toIndex - fromIndex));

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static <C extends Collection<Boolean>> C toCollection(final boolean[] a, final IntFunction<? extends C> supplier) {
        return toCollection(a, 0, len(a), supplier);
    }

    public static <C extends Collection<Boolean>> C toCollection(final boolean[] a, final int fromIndex, final int toIndex,
            final IntFunction<? extends C> supplier) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static <C extends Collection<Character>> C toCollection(final char[] a, final IntFunction<? extends C> supplier) {
        return toCollection(a, 0, len(a), supplier);
    }

    public static <C extends Collection<Character>> C toCollection(final char[] a, final int fromIndex, final int toIndex,
            final IntFunction<? extends C> supplier) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static <C extends Collection<Byte>> C toCollection(final byte[] a, final IntFunction<? extends C> supplier) {
        return toCollection(a, 0, len(a), supplier);
    }

    public static <C extends Collection<Byte>> C toCollection(final byte[] a, final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static <C extends Collection<Short>> C toCollection(final short[] a, final IntFunction<? extends C> supplier) {
        return toCollection(a, 0, len(a), supplier);
    }

    public static <C extends Collection<Short>> C toCollection(final short[] a, final int fromIndex, final int toIndex,
            final IntFunction<? extends C> supplier) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static <C extends Collection<Integer>> C toCollection(final int[] a, final IntFunction<? extends C> supplier) {
        return toCollection(a, 0, len(a), supplier);
    }

    public static <C extends Collection<Integer>> C toCollection(final int[] a, final int fromIndex, final int toIndex,
            final IntFunction<? extends C> supplier) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static <C extends Collection<Long>> C toCollection(final long[] a, final IntFunction<? extends C> supplier) {
        return toCollection(a, 0, len(a), supplier);
    }

    public static <C extends Collection<Long>> C toCollection(final long[] a, final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static <C extends Collection<Float>> C toCollection(final float[] a, final IntFunction<? extends C> supplier) {
        return toCollection(a, 0, len(a), supplier);
    }

    public static <C extends Collection<Float>> C toCollection(final float[] a, final int fromIndex, final int toIndex,
            final IntFunction<? extends C> supplier) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static <C extends Collection<Double>> C toCollection(final double[] a, final IntFunction<? extends C> supplier) {
        return toCollection(a, 0, len(a), supplier);
    }

    public static <C extends Collection<Double>> C toCollection(final double[] a, final int fromIndex, final int toIndex,
            final IntFunction<? extends C> supplier) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(a[i]);
        }

        return result;
    }

    public static <T, C extends Collection<T>> C toCollection(final T[] a, final IntFunction<? extends C> supplier) {
        if (N.isNullOrEmpty(a)) {
            return supplier.apply(0);
        }

        return toCollection(a, 0, a.length, supplier);
    }

    public static <T, C extends Collection<T>> C toCollection(final T[] a, final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier) {
        N.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return supplier.apply(0);
        } else if (fromIndex == 0 && toIndex == a.length && a.length >= MIN_SIZE_FOR_COPY_ALL) {
            final C result = supplier.apply(a.length);
            result.addAll(Arrays.asList(a));
            return result;
        } else {
            final C result = supplier.apply(toIndex - fromIndex);

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(a[i]);
            }

            return result;
        }
    }

    /**
     * The input array is returned.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> T[] asArray(final T... a) {
        return a;
    }

    @SuppressWarnings("unchecked")
    static <K, V, T extends Map<K, V>> T newMap(final T m, final Object... a) {
        if (isNullOrEmpty(a)) {
            return m;
        }

        if (a.length == 1) {
            if (a[0] instanceof Map) {
                m.putAll((Map<K, V>) a[0]);
            } else if (ClassUtil.isEntity(a[0].getClass())) {
                Maps.entity2Map((Map<String, Object>) m, a[0]);
            } else {
                throw new IllegalArgumentException(
                        "The parameters must be the pairs of property name and value, or Map, or an entity class with getter/setter methods.");
            }
        } else {
            if (0 != (a.length % 2)) {
                throw new IllegalArgumentException(
                        "The parameters must be the pairs of property name and value, or Map, or an entity class with getter/setter methods.");
            }

            for (int i = 0; i < a.length; i++) {
                m.put((K) a[i], (V) a[++i]);
            }
        }

        return m;
    }

    public static <K, V, k extends K, v extends V> Map<K, V> asMap(final k k1, final v v1) {
        final Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        return map;
    }

    public static <K, V, k extends K, v extends V> Map<K, V> asMap(final k k1, final v v1, final k k2, final v v2) {
        final Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    public static <K, V, k extends K, v extends V> Map<K, V> asMap(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3) {
        final Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }

    public static <K, V, k extends K, v extends V> Map<K, V> asMap(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3, final k k4,
            final v v4) {
        final Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        return map;
    }

    public static <K, V, k extends K, v extends V> Map<K, V> asMap(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3, final k k4,
            final v v4, final k k5, final v v5) {
        final Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        return map;
    }

    public static <K, V, k extends K, v extends V> Map<K, V> asMap(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3, final k k4,
            final v v4, final k k5, final v v5, final k k6, final v v6) {
        final Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        return map;
    }

    public static <K, V, k extends K, v extends V> Map<K, V> asMap(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3, final k k4,
            final v v4, final k k5, final v v5, final k k6, final v v6, final k k7, final v v7) {
        final Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        return map;
    }

    @SafeVarargs
    @NullSafe
    public static <K, V> Map<K, V> asMap(final Object... a) {
        if (N.isNullOrEmpty(a)) {
            return new HashMap<>();
        }

        return newMap(new HashMap<K, V>(N.initHashCapacity(a.length / 2)), a);
    }

    /**
     *
     * @param a
     *            pairs of property name and value or a Java Entity Object what
     *            allows access to properties using getter and setter methods.
     * @return
     */
    @SafeVarargs
    public static Map<String, Object> asProps(final Object... a) {
        if (N.isNullOrEmpty(a)) {
            return new LinkedHashMap<>();
        }

        return newMap(new LinkedHashMap<String, Object>(N.initHashCapacity(a.length / 2)), a);
    }

    public static <K, V, k extends K, v extends V> LinkedHashMap<K, V> asLinkedHashMap(final k k1, final v v1) {
        final LinkedHashMap<K, V> map = new LinkedHashMap<>();
        map.put(k1, v1);
        return map;
    }

    public static <K, V, k extends K, v extends V> LinkedHashMap<K, V> asLinkedHashMap(final k k1, final v v1, final k k2, final v v2) {
        final LinkedHashMap<K, V> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    public static <K, V, k extends K, v extends V> LinkedHashMap<K, V> asLinkedHashMap(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3) {
        final LinkedHashMap<K, V> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }

    public static <K, V, k extends K, v extends V> LinkedHashMap<K, V> asLinkedHashMap(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3,
            final k k4, final v v4) {
        final LinkedHashMap<K, V> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        return map;
    }

    public static <K, V, k extends K, v extends V> LinkedHashMap<K, V> asLinkedHashMap(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3,
            final k k4, final v v4, final k k5, final v v5) {
        final LinkedHashMap<K, V> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        return map;
    }

    public static <K, V, k extends K, v extends V> LinkedHashMap<K, V> asLinkedHashMap(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3,
            final k k4, final v v4, final k k5, final v v5, final k k6, final v v6) {
        final LinkedHashMap<K, V> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        return map;
    }

    public static <K, V, k extends K, v extends V> LinkedHashMap<K, V> asLinkedHashMap(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3,
            final k k4, final v v4, final k k5, final v v5, final k k6, final v v6, final k k7, final v v7) {
        final LinkedHashMap<K, V> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        map.put(k7, v7);
        return map;
    }

    @SafeVarargs
    public static <K, V> LinkedHashMap<K, V> asLinkedHashMap(final Object... a) {
        if (N.isNullOrEmpty(a)) {
            return new LinkedHashMap<>();
        }

        return newMap(new LinkedHashMap<K, V>(N.initHashCapacity(a.length / 2)), a);
    }

    public static <T> List<T> asList(final T e) {
        final List<T> list = new ArrayList<>(1);
        list.add(e);
        return list;
    }

    public static <T> List<T> asList(final T e1, final T e2) {
        final List<T> list = new ArrayList<>(2);
        list.add(e1);
        list.add(e2);
        return list;
    }

    public static <T> List<T> asList(final T e1, final T e2, final T e3) {
        final List<T> list = new ArrayList<>(3);
        list.add(e1);
        list.add(e2);
        list.add(e3);
        return list;
    }

    public static <T> List<T> asList(final T e1, final T e2, final T e3, final T e4) {
        final List<T> list = new ArrayList<>(4);
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        return list;
    }

    public static <T> List<T> asList(final T e1, final T e2, final T e3, final T e4, final T e5) {
        final List<T> list = new ArrayList<>(5);
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        list.add(e5);
        return list;
    }

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
     * @param a
     * @return
     */
    @SafeVarargs
    @NullSafe
    public static <T> List<T> asList(final T... a) {
        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final List<T> list = new ArrayList<>(a.length);

        if (a.length < MIN_SIZE_FOR_COPY_ALL) {
            for (T e : a) {
                list.add(e);
            }
        } else {
            list.addAll(Arrays.asList(a));
        }

        return list;
    }

    public static <T> LinkedList<T> asLinkedList(final T e) {
        final LinkedList<T> list = new LinkedList<>();
        list.add(e);
        return list;
    }

    public static <T> LinkedList<T> asLinkedList(final T e1, final T e2) {
        final LinkedList<T> list = new LinkedList<>();
        list.add(e1);
        list.add(e2);
        return list;
    }

    public static <T> LinkedList<T> asLinkedList(final T e1, final T e2, final T e3) {
        final LinkedList<T> list = new LinkedList<>();
        list.add(e1);
        list.add(e2);
        list.add(e3);
        return list;
    }

    public static <T> LinkedList<T> asLinkedList(final T e1, final T e2, final T e3, final T e4) {
        final LinkedList<T> list = new LinkedList<>();
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        return list;
    }

    public static <T> LinkedList<T> asLinkedList(final T e1, final T e2, final T e3, final T e4, final T e5) {
        final LinkedList<T> list = new LinkedList<>();
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        list.add(e5);
        return list;
    }

    public static <T> LinkedList<T> asLinkedList(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6) {
        final LinkedList<T> list = new LinkedList<>();
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        list.add(e5);
        list.add(e6);
        return list;
    }

    public static <T> LinkedList<T> asLinkedList(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6, final T e7) {
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

    @SafeVarargs
    @NullSafe
    public static <T> LinkedList<T> asLinkedList(final T... a) {
        if (N.isNullOrEmpty(a)) {
            return new LinkedList<>();
        }

        final LinkedList<T> list = new LinkedList<>();

        for (T e : a) {
            list.add(e);
        }

        return list;
    }

    public static <T> Set<T> asSet(final T e) {
        final Set<T> set = new HashSet<>(1);
        set.add(e);
        return set;
    }

    public static <T> Set<T> asSet(final T e1, final T e2) {
        final Set<T> set = new HashSet<>(2);
        set.add(e1);
        set.add(e2);
        return set;
    }

    public static <T> Set<T> asSet(final T e1, final T e2, final T e3) {
        final Set<T> set = new HashSet<>(3);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        return set;
    }

    public static <T> Set<T> asSet(final T e1, final T e2, final T e3, final T e4) {
        final Set<T> set = new HashSet<>(4);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        return set;
    }

    public static <T> Set<T> asSet(final T e1, final T e2, final T e3, final T e4, final T e5) {
        final Set<T> set = new HashSet<>(5);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        set.add(e5);
        return set;
    }

    public static <T> Set<T> asSet(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6) {
        final Set<T> set = new HashSet<>(6);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        set.add(e5);
        set.add(e6);
        return set;
    }

    public static <T> Set<T> asSet(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6, final T e7) {
        final Set<T> set = new HashSet<>(7);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        set.add(e5);
        set.add(e6);
        set.add(e7);
        return set;
    }

    public static <T> Set<T> asSet(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6, final T e7, final T e8) {
        final Set<T> set = new HashSet<>(8);
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

    public static <T> Set<T> asSet(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6, final T e7, final T e8, final T e9) {
        final Set<T> set = new HashSet<>(9);
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

    @SafeVarargs
    @NullSafe
    public static <T> Set<T> asSet(final T... a) {
        if (N.isNullOrEmpty(a)) {
            return new HashSet<>();
        }

        final Set<T> set = new HashSet<>(N.initHashCapacity(a.length));

        for (T e : a) {
            set.add(e);
        }

        return set;
    }

    public static <T> LinkedHashSet<T> asLinkedLinkedHashSet(final T e) {
        final LinkedHashSet<T> set = new LinkedHashSet<>(1);
        set.add(e);
        return set;
    }

    public static <T> LinkedHashSet<T> asLinkedLinkedHashSet(final T e1, final T e2) {
        final LinkedHashSet<T> set = new LinkedHashSet<>(2);
        set.add(e1);
        set.add(e2);
        return set;
    }

    public static <T> LinkedHashSet<T> asLinkedLinkedHashSet(final T e1, final T e2, final T e3) {
        final LinkedHashSet<T> set = new LinkedHashSet<>(3);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        return set;
    }

    public static <T> LinkedHashSet<T> asLinkedLinkedHashSet(final T e1, final T e2, final T e3, final T e4) {
        final LinkedHashSet<T> set = new LinkedHashSet<>(4);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        return set;
    }

    public static <T> LinkedHashSet<T> asLinkedLinkedHashSet(final T e1, final T e2, final T e3, final T e4, final T e5) {
        final LinkedHashSet<T> set = new LinkedHashSet<>(5);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        set.add(e5);
        return set;
    }

    public static <T> LinkedHashSet<T> asLinkedLinkedHashSet(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6) {
        final LinkedHashSet<T> set = new LinkedHashSet<>(6);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        set.add(e5);
        set.add(e6);
        return set;
    }

    public static <T> LinkedHashSet<T> asLinkedLinkedHashSet(final T e1, final T e2, final T e3, final T e4, final T e5, final T e6, final T e7) {
        final LinkedHashSet<T> set = new LinkedHashSet<>(7);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        set.add(e5);
        set.add(e6);
        set.add(e7);
        return set;
    }

    @SafeVarargs
    @NullSafe
    public static <T> LinkedHashSet<T> asLinkedHashSet(final T... a) {
        if (N.isNullOrEmpty(a)) {
            return new LinkedHashSet<>();
        }

        final LinkedHashSet<T> set = new LinkedHashSet<>(N.initHashCapacity(a.length));

        for (T e : a) {
            set.add(e);
        }

        return set;
    }

    @SafeVarargs
    @NullSafe
    public static <T> SortedSet<T> asSortedSet(final T... a) {
        if (N.isNullOrEmpty(a)) {
            return new TreeSet<>();
        }

        final SortedSet<T> set = new TreeSet<>();

        for (T e : a) {
            set.add(e);
        }

        return set;
    }

    @SafeVarargs
    public static <T> NavigableSet<T> asNavigableSet(final T... a) {
        if (N.isNullOrEmpty(a)) {
            return new TreeSet<>();
        }

        final NavigableSet<T> set = new TreeSet<>();

        for (T e : a) {
            set.add(e);
        }

        return set;
    }

    @SafeVarargs
    public static <T> Queue<T> asQueue(final T... a) {
        return asArrayDeque(a);
    }

    @SafeVarargs
    public static <T> ArrayBlockingQueue<T> asArrayBlockingQueue(final T... a) {
        if (N.isNullOrEmpty(a)) {
            return new ArrayBlockingQueue<>(0);
        }

        final ArrayBlockingQueue<T> queue = new ArrayBlockingQueue<>(a.length);

        for (T e : a) {
            queue.add(e);
        }

        return queue;
    }

    @SafeVarargs
    public static <T> LinkedBlockingQueue<T> asLinkedBlockingQueue(final T... a) {
        if (N.isNullOrEmpty(a)) {
            return new LinkedBlockingQueue<>();
        }

        final LinkedBlockingQueue<T> queue = new LinkedBlockingQueue<>(a.length);

        for (T e : a) {
            queue.add(e);
        }

        return queue;
    }

    @SafeVarargs
    public static <T> ConcurrentLinkedQueue<T> asConcurrentLinkedQueue(final T... a) {
        if (N.isNullOrEmpty(a)) {
            return new ConcurrentLinkedQueue<>();
        }

        final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();

        for (T e : a) {
            queue.add(e);
        }

        return queue;
    }

    @SafeVarargs
    public static <T extends Delayed> DelayQueue<T> asDelayQueue(final T... a) {
        if (N.isNullOrEmpty(a)) {
            return new DelayQueue<>();
        }

        final DelayQueue<T> queue = new DelayQueue<>();

        for (T e : a) {
            queue.add(e);
        }

        return queue;
    }

    @SafeVarargs
    public static <T> PriorityQueue<T> asPriorityQueue(final T... a) {
        if (N.isNullOrEmpty(a)) {
            return new PriorityQueue<>();
        }

        final PriorityQueue<T> queue = new PriorityQueue<>(a.length);

        for (T e : a) {
            queue.add(e);
        }

        return queue;
    }

    @SafeVarargs
    public static <T> Deque<T> asDeque(final T... a) {
        return asArrayDeque(a);
    }

    @SafeVarargs
    public static <T> ArrayDeque<T> asArrayDeque(final T... a) {
        if (N.isNullOrEmpty(a)) {
            return new ArrayDeque<>();
        }

        final ArrayDeque<T> arrayDeque = new ArrayDeque<>(a.length);

        for (T e : a) {
            arrayDeque.add(e);
        }

        return arrayDeque;
    }

    @SafeVarargs
    public static <T> LinkedBlockingDeque<T> asLinkedBlockingDeque(final T... a) {
        if (N.isNullOrEmpty(a)) {
            return new LinkedBlockingDeque<>();
        }

        final LinkedBlockingDeque<T> deque = new LinkedBlockingDeque<>(a.length);

        for (T e : a) {
            deque.add(e);
        }

        return deque;
    }

    @SafeVarargs
    public static <T> ConcurrentLinkedDeque<T> asConcurrentLinkedDeque(final T... a) {
        if (N.isNullOrEmpty(a)) {
            return new ConcurrentLinkedDeque<>();
        }

        final ConcurrentLinkedDeque<T> deque = new ConcurrentLinkedDeque<>();

        for (T e : a) {
            deque.add(e);
        }

        return deque;
    }

    @SafeVarargs
    public static <T> Multiset<T> asMultiset(final T... a) {
        return Multiset.of(a);
    }

    /**
     * Wrap the specified value with a singleton list.
     *
     * @param o
     * @return
     * @see java.util.Collections#singletonList(Object)
     */
    public static <T> List<T> asSingletonList(final T o) {
        return Collections.singletonList(o);
    }

    /**
     * Wrap the specified value with a singleton set.
     *
     * @param o
     * @return
     * @see java.util.Collections#singleton(Object)
     */
    public static <T> Set<T> asSingletonSet(final T o) {
        return Collections.singleton(o);
    }

    /**
     * Wrap the specified key/value with a singleton map.
     *
     * @param key
     * @param value
     * @return
     * @see java.util.Collections#singletonMap(Object, Object)
     */
    public static <K, V> Map<K, V> asSingletonMap(final K key, final V value) {
        return Collections.singletonMap(key, value);
    }

    //    /**
    //     * Try to convert the specified {@code obj} to the specified
    //     * {@code targetClass}. Default value of {@code targetClass} is returned if
    //     * {@code src} is null. An instance of {@code targetClass} is returned if
    //     * convert successfully
    //     *
    //     * @param targetClass
    //     * @param obj
    //     * @return
    //     * @throws ClassCastException
    //     * @Deprecated replaced by {@link N#convert(Object, Class)}.
    //     */
    //    @Deprecated
    //    @SuppressWarnings("unchecked")
    //    public static <T> T as(final Class<? extends T> targetClass, final Object obj) {
    //        return convert(obj, targetClass);
    //    }
    //
    //    /**
    //     *
    //     * @param targetType
    //     * @param obj
    //     * @return
    //     * @Deprecated replaced by {@link N#convert(Object, Type)}.
    //     */
    //    @Deprecated
    //    public static <T> T as(final Type<? extends T> targetType, final Object obj) {
    //        return convert(obj, targetType);
    //    }

    /**
     * Try to convert the specified {@code obj} to the specified
     * {@code targetClass}. Default value of {@code targetClass} is returned if
     * {@code sourceObject} is null. An instance of {@code targetClass} is returned if
     * convert successfully
     *
     * @param obj
     * @param targetClass
     * @return
     */
    public static <T> T convert(final Object obj, final Class<? extends T> targetClass) {
        //        if (obj == null) {
        //            return defaultValueOf(targetClass);
        //        }
        //
        //        final Class<?> srcPropClass = obj.getClass();
        //
        //        if (targetClass.isAssignableFrom(srcPropClass)) {
        //            return (T) obj;
        //        }
        //
        //        final Type<Object> targetPropType = targetClass.isEnum()
        //                ? getType(getCanonicalClassName(targetClass) + "(" + Integer.class.isAssignableFrom(srcPropClass) + ")") : N.getType(targetClass);
        //        final Type<Object> srcPropType = getType(srcPropClass);
        //
        //        if (targetPropType.isBoolean() && srcPropType.isNumber()) {
        //            return (T) ((Boolean) (((Number) obj).longValue() > 0));
        //        }
        //
        //        if (targetPropType.isEntity() && srcPropType.isMap()) {
        //            return map2Entity(targetClass, (Map<String, Object>) obj);
        //        } else if (targetPropType.isMap() && srcPropType.isEntity()) {
        //            try {
        //                return (T) entity2Map((Map<String, Object>) N.newInstance(targetClass), obj);
        //            } catch (Exception e) {
        //                // ignore.
        //            }
        //        } else if (targetPropType.isEntity() && srcPropType.isEntity()) {
        //            return copy(targetClass, obj);
        //        }
        //
        //        return (T) targetPropType.valueOf(srcPropType.stringOf(obj));

        final Type<T> type = typeOf(targetClass);
        return convert(obj, type);
    }

    @SuppressWarnings("unchecked")
    public static <T> T convert(final Object obj, final Type<? extends T> targetType) {
        if (obj == null) {
            return targetType.defaultValue();
        }

        final Class<?> srcPropClass = obj.getClass();

        if (targetType.clazz().isAssignableFrom(srcPropClass)) {
            return (T) obj;
        }

        final Type<Object> srcPropType = typeOf(srcPropClass);

        if (targetType.isBoolean() && srcPropType.isNumber()) {
            return (T) ((Boolean) (((Number) obj).longValue() > 0));
        }

        if (targetType.isEntity() && srcPropType.isMap()) {
            return Maps.map2Entity(targetType.clazz(), (Map<String, Object>) obj);
        } else if (targetType.isMap() && srcPropType.isEntity()) {
            try {
                return (T) Maps.entity2Map((Map<String, Object>) N.newInstance(targetType.clazz()), obj);
            } catch (Exception e) {
                // ignore.
            }
        } else if (targetType.isEntity() && srcPropType.isEntity()) {
            return copy(targetType.clazz(), obj);
        } else if (targetType.isNumber() && srcPropType.isNumber() && CLASS_TYPE_ENUM.containsKey(targetType.clazz())) {
            switch (CLASS_TYPE_ENUM.get(targetType.clazz())) {
                case 3:
                case 13:
                    return (T) (Byte) ((Number) obj).byteValue();

                case 4:
                case 14:
                    return (T) (Short) ((Number) obj).shortValue();

                case 5:
                case 15:
                    return (T) (Integer) ((Number) obj).intValue();

                case 6:
                case 16:
                    return (T) (Long) ((Number) obj).longValue();

                case 7:
                case 17:
                    return (T) (Float) ((Number) obj).floatValue();

                case 8:
                case 18:
                    return (T) (Double) ((Number) obj).doubleValue();

            }
        }

        return targetType.valueOf(obj);
    }

    /**
     * Returns a <code>Boolean</code> with a value represented by the specified
     * string. The <code>Boolean</code> returned represents a true value if the
     * string argument is not <code>null</code> and is equal, ignoring case, to
     * the string {@code "true"}.
     *
     * @param str
     *            a string.
     * @return the <code>Boolean</code> value represented by the string.
     */
    public static boolean parseBoolean(final String str) {
        return isNullOrEmpty(str) ? false : Boolean.valueOf(str);
    }

    public static char parseChar(final String str) {
        return isNullOrEmpty(str) ? CHAR_0 : ((str.length() == 1) ? str.charAt(0) : (char) Integer.parseInt(str));
    }

    /**
     * Returns the value by calling {@code Byte.valueOf(String)} if {@code str}
     * is not {@code null}, otherwise, the default value 0 for {@code byte} is
     * returned.
     *
     * @param str
     * @return
     * @throws  NumberFormatException If the string does not
     *          contain a parsable {@code byte}.
     */
    public static byte parseByte(final String str) {
        if (N.isNullOrEmpty(str)) {
            return 0;
        }

        if (str.length() < 5) {
            Integer result = stringIntCache.get(str);
            if (result != null) {
                if (result.intValue() < Byte.MIN_VALUE || result.intValue() > Byte.MAX_VALUE) {
                    throw new NumberFormatException("Value out of range. Value:\"" + str + "\" Radix: 10");
                }

                return result.byteValue();
            }
        }

        return Byte.parseByte(str);
    }

    /**
     * Returns the value by calling {@code Short.valueOf(String)} if {@code str}
     * is not {@code null}, otherwise, the default value 0 for {@code short} is
     * returned.
     *
     * @param str
     * @return
     * @throws  NumberFormatException If the string does not
     *          contain a parsable {@code short}.
     */
    public static short parseShort(final String str) {
        if (N.isNullOrEmpty(str)) {
            return 0;
        }

        if (str.length() < 5) {
            Integer result = stringIntCache.get(str);
            if (result != null) {
                return result.shortValue();
            }
        }

        return Short.parseShort(str);
    }

    /**
     * Returns the value by calling {@code Integer.valueOf(String)} if
     * {@code str} is not {@code null}, otherwise, the default value 0 for
     * {@code int} is returned.
     *
     * @param str
     * @return
     * @throws  NumberFormatException If the string does not
     *          contain a parsable {@code int}.
     */
    public static int parseInt(final String str) {
        if (N.isNullOrEmpty(str)) {
            return 0;
        }

        if (str.length() < 5) {
            Integer result = stringIntCache.get(str);
            if (result != null) {
                return result.intValue();
            }
        }

        return Integer.decode(str);
    }

    /**
     * Returns the value by calling {@code Long.valueOf(String)} if {@code str}
     * is not {@code null}, otherwise, the default value 0 for {@code long} is
     * returned.
     *
     * @param str
     * @return
     * @throws  NumberFormatException If the string does not
     *          contain a parsable {@code long}.
     */
    public static long parseLong(final String str) {
        if (N.isNullOrEmpty(str)) {
            return 0;
        }

        if (str.length() < 5) {
            Integer result = stringIntCache.get(str);
            if (result != null) {
                return result.intValue();
            }
        }

        return Long.decode(str);
    }

    /**
     * Returns the value by calling {@code Float.valueOf(String)} if {@code str}
     * is not {@code null}, otherwise, the default value 0f for {@code float} is
     * returned.
     *
     * @param str
     * @return
     * @throws  NumberFormatException If the string does not
     *          contain a parsable {@code float}.
     */
    public static float parseFloat(final String str) {
        if (isNullOrEmpty(str)) {
            return 0f;
        }

        return Float.parseFloat(str);
    }

    /**
     * Returns the value by calling {@code Double.valueOf(String)} if {@code str}
     * is not {@code null}, otherwise, the default value 0d for {@code double} is
     * returned.
     *
     * @param str
     * @return
     * @throws  NumberFormatException If the string does not
     *          contain a parsable {@code double}.
     */
    public static double parseDouble(final String str) {
        return isNullOrEmpty(str) ? 0d : Double.parseDouble(str);
    }

    /**
     * @param binaryData
     * @return
     */
    public static String base64Encode(final byte[] binaryData) {
        if (N.isNullOrEmpty(binaryData)) {
            return N.EMPTY_STRING;
        }

        return Base64.encodeBase64String(binaryData);
    }

    /**
     * @param binaryData
     * @return
     */
    public static String base64EncodeChunked(final byte[] binaryData) {
        if (N.isNullOrEmpty(binaryData)) {
            return N.EMPTY_STRING;
        }

        return new String(Base64.encodeBase64Chunked(binaryData), Charsets.US_ASCII);
    }

    /**
     * @param base64String
     * @return
     */
    public static byte[] base64Decode(final String base64String) {
        if (N.isNullOrEmpty(base64String)) {
            return N.EMPTY_BYTE_ARRAY;
        }

        return Base64.decodeBase64(base64String);
    }

    /**
     * @param base64String
     * @return
     */
    public static String base64DecodeToString(final String base64String) {
        if (N.isNullOrEmpty(base64String)) {
            return N.EMPTY_STRING;
        }

        return new String(base64Decode(base64String));
    }

    /**
     * @param binaryData
     * @return
     */
    public static String base64UrlEncode(final byte[] binaryData) {
        if (N.isNullOrEmpty(binaryData)) {
            return N.EMPTY_STRING;
        }

        return Base64.encodeBase64URLSafeString(binaryData);
    }

    /**
     * @param base64String
     * @return
     */
    public static byte[] base64UrlDecode(final String base64String) {
        if (N.isNullOrEmpty(base64String)) {
            return N.EMPTY_BYTE_ARRAY;
        }

        return Base64.decodeBase64URL(base64String);
    }

    /**
     * @param base64String
     * @return
     */
    public static String base64UrlDecodeToString(final String base64String) {
        if (N.isNullOrEmpty(base64String)) {
            return N.EMPTY_STRING;
        }

        return new String(Base64.decodeBase64URL(base64String));
    }

    public static String urlEncode(final Object parameters) {
        if (parameters == null) {
            return N.EMPTY_STRING;
        }

        return URLEncodedUtil.encode(parameters);
    }

    public static String urlEncode(final Object parameters, final Charset charset) {
        if (parameters == null) {
            return N.EMPTY_STRING;
        }

        return URLEncodedUtil.encode(parameters, charset);
    }

    public static Map<String, String> urlDecode(final String urlQuery) {
        if (N.isNullOrEmpty(urlQuery)) {
            return new LinkedHashMap<>();
        }

        return URLEncodedUtil.decode(urlQuery);
    }

    public static Map<String, String> urlDecode(final String urlQuery, final Charset charset) {
        if (N.isNullErrorMsg(urlQuery)) {
            return new LinkedHashMap<>();
        }

        return URLEncodedUtil.decode(urlQuery, charset);
    }

    public static <T> T urlDecode(final Class<? extends T> targetClass, final String urlQuery) {
        if (N.isNullErrorMsg(urlQuery)) {
            return N.newInstance(targetClass);
        }

        return URLEncodedUtil.decode(targetClass, urlQuery);
    }

    public static <T> T urlDecode(final Class<? extends T> targetClass, final String urlQuery, final Charset charset) {
        if (N.isNullOrEmpty(urlQuery)) {
            return N.newInstance(targetClass);
        }

        return URLEncodedUtil.decode(targetClass, urlQuery, charset);
    }

    /**
     * Returns the UUID without '-'.
     *
     * @see UUID#randomUUID().
     */
    public static String guid() {
        return uuid().replace("-", "");
    }

    /**
     * Returns an UUID
     *
     * @see UUID#randomUUID().
     * @return
     */
    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    /**
     *
     * @param cls
     * @return
     * @deprecated replaced by {@code ClassUtil.isEntity(Class)}
     * @see ClassUtil#isEntity(Class)
     */
    @Deprecated
    public static boolean isEntity(final Class<?> cls) {
        return ClassUtil.isEntity(cls);
    }

    /**
     *
     * @param cls
     * @return
     * @deprecated replaced by {@code ClassUtil.isDirtyMarker(Class)}
     * @see ClassUtil#isDirtyMarker(Class)
     */
    @Deprecated
    public static boolean isDirtyMarker(final Class<?> cls) {
        return ClassUtil.isDirtyMarker(cls);
    }

    private static final Set<Class<?>> notKryoCompatible = new HashSet<>();

    @SuppressWarnings("unchecked")
    public static <T> T clone(final T entity) {
        return (T) clone(entity.getClass(), entity);
    }

    /**
     * Deeply copy by: entity -> serialize -> String/bytes -> deserialize -> new
     * entity.
     *
     * @param targetClass
     *            a Java Object what allows access to properties using getter
     *            and setter methods.
     * @param entity
     *            a Java Object what allows access to properties using getter
     *            and setter methods.
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T clone(final Class<? extends T> targetClass, final Object entity) {
        final Class<?> srcCls = entity.getClass();
        Object copy = null;

        if (Utils.kryoParser != null && targetClass.equals(entity.getClass()) && !notKryoCompatible.contains(srcCls)) {
            try {
                copy = Utils.kryoParser.clone(entity);
            } catch (Exception e) {
                notKryoCompatible.add(srcCls);

                // ignore.
            }
        }

        if (copy == null) {
            String xml = Utils.abacusXMLParser.serialize(entity, Utils.xscForClone);
            copy = Utils.abacusXMLParser.deserialize(targetClass, xml);

            setDirtyMarker(entity, copy);
        }

        return (T) copy;
    }

    /**
     * Returns a new created instance of the same class and set with same
     * properties retrieved by 'getXXX' method in the specified {@code entity}.
     *
     * @param entity
     *            a Java Object what allows access to properties using getter
     *            and setter methods.
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T copy(final T entity) {
        return copy((Class<T>) entity.getClass(), entity);
    }

    public static <T> T copy(final T entity, final Collection<String> selectPropNames) {
        return copy((Class<T>) entity.getClass(), entity, selectPropNames);
    }

    public static <T> T copy(final Class<? extends T> targetClass, final Object entity) {
        return copy(targetClass, entity, null);
    }

    /**
     * Returns a new created instance of the specified {@code cls} and set with
     * same properties retrieved by 'getXXX' method in the specified
     * {@code entity}.
     *
     * @param targetClass
     *            a Java Object what allows access to properties using getter
     *            and setter methods.
     * @param entity
     *            a Java Object what allows access to properties using getter
     *            and setter methods.
     * @param selectPropNames
     * @return
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> T copy(final Class<? extends T> targetClass, final Object entity, final Collection<String> selectPropNames) {
        final Class<?> srcCls = entity.getClass();
        T copy = null;

        if (selectPropNames == null && Utils.kryoParser != null && targetClass.equals(srcCls) && !notKryoCompatible.contains(srcCls)) {
            try {
                copy = (T) Utils.kryoParser.copy(entity);
            } catch (Exception e) {
                notKryoCompatible.add(srcCls);

                // ignore
            }
        }

        if (copy != null) {
            return copy;
        }

        copy = N.newInstance(targetClass);

        merge(entity, copy, selectPropNames);

        setDirtyMarker(entity, copy);

        return copy;
    }

    /**
     *
     * @param targetClass
     * @param entity
     * @param ignoreUnknownProperty
     * @param ignorePropNames
     * @return
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> T copy(final Class<? extends T> targetClass, final Object entity, final boolean ignoreUnknownProperty,
            final Set<String> ignorePropNames) {
        final Class<?> srcCls = entity.getClass();
        T copy = null;

        if (ignorePropNames == null && Utils.kryoParser != null && targetClass.equals(srcCls) && !notKryoCompatible.contains(srcCls)) {
            try {
                copy = (T) Utils.kryoParser.copy(entity);
            } catch (Exception e) {
                notKryoCompatible.add(srcCls);

                // ignore
            }
        }

        if (copy != null) {
            return copy;
        }

        copy = N.newInstance(targetClass);

        merge(entity, copy, ignoreUnknownProperty, ignorePropNames);

        setDirtyMarker(entity, copy);

        return copy;
    }

    public static void merge(final Object sourceEntity, final Object targetEntity) {
        merge(sourceEntity, targetEntity, null);
    }

    /**
     * Set all the signed properties(including all primitive type properties) in
     * the specified {@code sourceEntity} to the specified {@code targetEntity}.
     *
     * @param sourceEntity
     *            a Java Object what allows access to properties using getter
     *            and setter methods.
     * @param targetEntity
     *            a Java Object what allows access to properties using getter
     *            and setter methods.
     * @param selectPropNames
     */
    @SuppressWarnings("deprecation")
    public static void merge(final Object sourceEntity, final Object targetEntity, final Collection<String> selectPropNames) {
        final Class<?> srcCls = sourceEntity.getClass();
        final boolean ignoreUnknownProperty = selectPropNames == null;

        if (selectPropNames == null) {
            if (sourceEntity instanceof DirtyMarker) {
                Set<String> signedPropNames = ((DirtyMarker) sourceEntity).signedPropNames();

                if (signedPropNames.size() == 0) {
                    // logger.warn("no property is signed in the specified source entity: "
                    // + toString(entity));
                } else {
                    Method srcPropGetMethod = null;

                    try {
                        for (String propName : signedPropNames) {
                            srcPropGetMethod = ClassUtil.getPropGetMethod(srcCls, propName);
                            ClassUtil.setPropValue(targetEntity, propName, srcPropGetMethod.invoke(sourceEntity), ignoreUnknownProperty);
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw N.toRuntimeException(e);
                    }
                }
            } else {
                final Map<String, Method> srcGetterMethodList = ClassUtil.checkPropGetMethodList(srcCls);

                try {
                    for (Map.Entry<String, Method> entry : srcGetterMethodList.entrySet()) {
                        ClassUtil.setPropValue(targetEntity, entry.getKey(), entry.getValue().invoke(sourceEntity), ignoreUnknownProperty);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw N.toRuntimeException(e);
                }
            }
        } else {
            Method srcPropGetMethod = null;

            try {
                for (String propName : selectPropNames) {
                    srcPropGetMethod = ClassUtil.getPropGetMethod(srcCls, propName);
                    ClassUtil.setPropValue(targetEntity, propName, srcPropGetMethod.invoke(sourceEntity), ignoreUnknownProperty);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw N.toRuntimeException(e);
            }
        }
    }

    /**
     *
     * @param sourceEntity
     * @param targetEntity
     * @param ignoreUnknownProperty
     * @param ignorePropNames
     */
    @SuppressWarnings("deprecation")
    public static void merge(final Object sourceEntity, final Object targetEntity, final boolean ignoreUnknownProperty, final Set<String> ignorePropNames) {
        final Class<?> srcCls = sourceEntity.getClass();

        if (sourceEntity instanceof DirtyMarker) {
            Set<String> signedPropNames = ((DirtyMarker) sourceEntity).signedPropNames();

            if (signedPropNames.size() == 0) {
                // logger.warn("no property is signed in the specified source entity: "
                // + toString(entity));
            } else {
                try {
                    Method srcPropGetMethod = null;

                    for (String propName : signedPropNames) {
                        if (ignorePropNames == null || ignorePropNames.contains(propName) == false) {
                            srcPropGetMethod = ClassUtil.getPropGetMethod(srcCls, propName);

                            ClassUtil.setPropValue(targetEntity, propName, srcPropGetMethod.invoke(sourceEntity), ignoreUnknownProperty);
                        }
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw N.toRuntimeException(e);
                }
            }
        } else {
            Map<String, Method> srcGetterMethodList = ClassUtil.checkPropGetMethodList(srcCls);

            try {
                for (Map.Entry<String, Method> entry : srcGetterMethodList.entrySet()) {
                    if (ignorePropNames == null || ignorePropNames.contains(entry.getKey()) == false) {
                        ClassUtil.setPropValue(targetEntity, entry.getKey(), entry.getValue().invoke(sourceEntity), ignoreUnknownProperty);
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw N.toRuntimeException(e);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static void setDirtyMarker(final Object source, final Object target) {
        if (source instanceof DirtyMarker && target instanceof DirtyMarker) {
            DirtyMarker dirtyMarkerSource = (DirtyMarker) source;
            DirtyMarker dirtyMarkerTarget = (DirtyMarker) target;

            dirtyMarkerTarget.signedPropNames().clear();
            dirtyMarkerTarget.signedPropNames().addAll(dirtyMarkerSource.signedPropNames());

            dirtyMarkerTarget.dirtyPropNames().clear();
            dirtyMarkerTarget.dirtyPropNames().addAll(dirtyMarkerSource.dirtyPropNames());

            EntityUtil.setVersion(dirtyMarkerTarget, dirtyMarkerSource.version());
        }
    }

    @SuppressWarnings("deprecation")
    @SafeVarargs
    public static void erase(final Object entity, final String... propNames) {
        if (entity == null || N.isNullOrEmpty(propNames) || (entity instanceof DirtyMarker && ((DirtyMarker) entity).signedPropNames().size() == 0)) {
            return;
        }

        for (String propName : propNames) {
            ClassUtil.setPropValue(entity, propName, null);
        }

        if (entity instanceof DirtyMarker) {
            final DirtyMarker dirtyMarkerEntity = (DirtyMarker) entity;

            for (String propName : propNames) {
                dirtyMarkerEntity.signedPropNames().remove(propName);
                dirtyMarkerEntity.dirtyPropNames().remove(propName);
            }
        }
    }

    @SuppressWarnings("deprecation")
    public static void erase(final Object entity, final Collection<String> propNames) {
        if (entity == null || N.isNullOrEmpty(propNames) || (entity instanceof DirtyMarker && ((DirtyMarker) entity).signedPropNames().size() == 0)) {
            return;
        }

        for (String propName : propNames) {
            ClassUtil.setPropValue(entity, propName, null);
        }

        if (entity instanceof DirtyMarker) {
            final DirtyMarker dirtyMarkerEntity = (DirtyMarker) entity;

            dirtyMarkerEntity.signedPropNames().removeAll(propNames);
            dirtyMarkerEntity.dirtyPropNames().removeAll(propNames);
        }
    }

    @SuppressWarnings("deprecation")
    public static void eraseAll(final Object entity) {
        if (entity == null) {
            return;
        }

        if (entity instanceof DirtyMarker) {
            final DirtyMarker dirtyMarkerEntity = (DirtyMarker) entity;
            final Set<String> signedPropNames = dirtyMarkerEntity.signedPropNames();

            if (signedPropNames.size() == 0) {
                // logger.warn("No property is signed in the specified source entity: " + toString(entity));
                return;
            }

            for (String propName : signedPropNames) {
                ClassUtil.setPropValue(entity, propName, null);
            }

            dirtyMarkerEntity.signedPropNames().clear();
            dirtyMarkerEntity.dirtyPropNames().clear();
        } else {
            Class<?> cls = entity.getClass();
            Map<String, Method> setterMethodList = ClassUtil.getPropSetMethodList(cls);

            if (setterMethodList.size() == 0) {
                throw new IllegalArgumentException("No property getter/setter method found in the specified entity: " + ClassUtil.getCanonicalClassName(cls));
            }

            for (Method propSetMethod : setterMethodList.values()) {
                ClassUtil.setPropValue(entity, propSetMethod, null);
            }
        }
    }

    /**
     * Returns an empty {@code List} that is immutable.
     *
     * @return
     * @see Collections#emptyList()
     */
    public static <T> List<T> emptyList() {
        return EMPTY_LIST;
    }

    /**
     * Returns an empty {@code Set} that is immutable.
     *
     * @return
     * @see Collections#emptySet()
     */
    public static <T> Set<T> emptySet() {
        return EMPTY_SET;
    }

    /**
     * Returns an empty {@code SortedSet} that is immutable.
     * 
     * @return
     * @see Collections#emptySortedSet()
     */
    public static <T> SortedSet<T> emptySortedSet() {
        return EMPTY_SORTED_SET;
    }

    /**
     * Returns an empty {@code emptyNavigableSet} that is immutable.
     * 
     * @return
     * @see Collections#emptyNavigableSet()
     */
    public static <T> NavigableSet<T> emptyNavigableSet() {
        return EMPTY_NAVIGABLE_SET;
    }

    /**
     * Returns an empty {@code Map} that is immutable.
     *
     * @return
     * @see Collections#emptyMap()
     */
    public static <K, V> Map<K, V> emptyMap() {
        return EMPTY_MAP;
    }

    /**
     * Returns an empty {@code SortedMap} that is immutable.
     * 
     * @return
     * @see Collections#emptySortedMap()
     */
    public static <K, V> SortedMap<K, V> emptySortedMap() {
        return EMPTY_SORTED_MAP;
    }

    /**
     * Returns an empty {@code NavigableMap} that is immutable.
     * 
     * @return
     * @see Collections#emptyNavigableMap()
     */
    public static <K, V> NavigableMap<K, V> emptyNavigableMap() {
        return EMPTY_NAVIGABLE_MAP;
    }

    /**
     * Returns an empty {@code Iterator} that is immutable.
     *
     * @return
     * @see Collections#emptyIterator()
     */
    public static <T> Iterator<T> emptyIterator() {
        return EMPTY_ITERATOR;
    }

    /**
     * Returns an empty {@code ListIterator} that is immutable.
     *
     * @return
     * @see Collections#emptyListIterator()
     */
    public static <T> ListIterator<T> emptyListIterator() {
        return EMPTY_LIST_ITERATOR;
    }

    private static final ByteArrayInputStream EMPTY_INPUT_STREAM = new ByteArrayInputStream(N.EMPTY_BYTE_ARRAY);

    public static InputStream emptyInputStream() {
        return EMPTY_INPUT_STREAM;
    }

    public static <T> boolean anyNull(final T a, final T b) {
        return a == null || b == null;
    }

    public static <T> boolean anyNull(final T a, final T b, final T c) {
        return a == null || b == null || c == null;
    }

    @SafeVarargs
    public static <T> boolean anyNull(final T... a) {
        if (N.isNullOrEmpty(a)) {
            return false;
        }

        for (T e : a) {
            if (e == null) {
                return true;
            }
        }

        return false;
    }

    public static <T> boolean anyNull(final Collection<T> c) {
        if (N.isNullOrEmpty(c)) {
            return false;
        }

        for (T e : c) {
            if (e == null) {
                return true;
            }
        }

        return false;
    }

    public static <T> boolean allNull(final T a, final T b) {
        return a == null && b == null;
    }

    public static <T> boolean allNull(final T a, final T b, final T c) {
        return a == null && b == null && c == null;
    }

    @SafeVarargs
    public static <T> boolean allNull(final T... a) {
        if (N.isNullOrEmpty(a)) {
            return true;
        }

        for (T e : a) {
            if (e != null) {
                return false;
            }
        }

        return true;
    }

    public static <T> boolean allNull(final Collection<T> c) {
        if (N.isNullOrEmpty(c)) {
            return true;
        }

        for (T e : c) {
            if (e != null) {
                return false;
            }
        }

        return true;
    }

    public static boolean anyNullOrEmpty(final CharSequence cs1, final CharSequence cs2, final CharSequence cs3) {
        return N.isNullOrEmpty(cs1) || N.isNullOrEmpty(cs2) || N.isNullOrEmpty(cs3);
    }

    @SafeVarargs
    public static boolean anyNullOrEmpty(final CharSequence... css) {
        if (N.isNullOrEmpty(css)) {
            return false;
        }

        for (CharSequence cs : css) {
            if (N.isNullOrEmpty(cs)) {
                return true;
            }
        }

        return false;
    }

    public static boolean anyNullOrEmpty(final Collection<? extends CharSequence> css) {
        if (N.isNullOrEmpty(css)) {
            return false;
        }

        for (CharSequence cs : css) {
            if (N.isNullOrEmpty(cs)) {
                return true;
            }
        }

        return false;
    }

    public static boolean allNullOrEmpty(final CharSequence cs1, final CharSequence cs2, final CharSequence cs3) {
        return N.isNullOrEmpty(cs1) && N.isNullOrEmpty(cs2) && N.isNullOrEmpty(cs3);
    }

    @SafeVarargs
    public static boolean allNullOrEmpty(final CharSequence... css) {
        if (N.isNullOrEmpty(css)) {
            return true;
        }

        for (CharSequence cs : css) {
            if (N.isNullOrEmpty(cs) == false) {
                return false;
            }
        }

        return true;
    }

    public static boolean allNullOrEmpty(final Collection<? extends CharSequence> css) {
        if (N.isNullOrEmpty(css)) {
            return true;
        }

        for (CharSequence cs : css) {
            if (N.isNullOrEmpty(cs) == false) {
                return false;
            }
        }

        return true;
    }

    public static <T> Nullable<T> first(final Collection<T> c) {
        if (N.isNullOrEmpty(c)) {
            return Nullable.empty();
        }

        if (c instanceof List && c instanceof RandomAccess) {
            return Nullable.of(((List<T>) c).get(0));
        } else {
            return Nullable.of(c.iterator().next());
        }
    }

    public static <T> Nullable<T> last(final Collection<T> c) {
        if (N.isNullOrEmpty(c)) {
            return Nullable.empty();
        }

        if (c instanceof List) {
            final List<T> list = (List<T>) c;

            if (c instanceof RandomAccess) {
                return Nullable.of(list.get(c.size() - 1));
            } else {
                return Nullable.of(list.listIterator(list.size()).previous());
            }
        } else if (c instanceof Deque) {
            return Nullable.of(((Deque<T>) c).descendingIterator().next());
        } else {
            return Iterators.last(c.iterator());
        }
    }

    public static <T> Optional<T> firstNonNull(final T a, final T b) {
        return a != null ? Optional.of(a) : (b != null ? Optional.of(b) : Optional.<T> empty());
    }

    public static <T> Optional<T> firstNonNull(final T a, final T b, final T c) {
        return a != null ? Optional.of(a) : (b != null ? Optional.of(b) : (c != null ? Optional.of(c) : Optional.<T> empty()));
    }

    @SafeVarargs
    public static <T> Optional<T> firstNonNull(final T... a) {
        if (N.isNullOrEmpty(a)) {
            return Optional.empty();
        }

        for (T e : a) {
            if (e != null) {
                return Optional.of(e);
            }
        }

        return Optional.empty();
    }

    public static <T> Optional<T> firstNonNull(final Collection<T> c) {
        if (N.isNullOrEmpty(c)) {
            return Optional.empty();
        }

        for (T e : c) {
            if (e != null) {
                return Optional.of(e);
            }
        }

        return Optional.empty();
    }

    public static <T> Optional<T> lastNonNull(final T a, final T b) {
        return b != null ? Optional.of(b) : (a != null ? Optional.of(a) : Optional.<T> empty());
    }

    public static <T> Optional<T> lastNonNull(final T a, final T b, final T c) {
        return c != null ? Optional.of(c) : (b != null ? Optional.of(b) : (a != null ? Optional.of(a) : Optional.<T> empty()));
    }

    @SafeVarargs
    public static <T> Optional<T> lastNonNull(final T... a) {
        if (N.isNullOrEmpty(a)) {
            return Optional.empty();
        }

        for (int i = a.length - 1; i >= 0; i--) {
            if (a[i] != null) {
                return Optional.of(a[i]);
            }
        }

        return Optional.empty();
    }

    public static <T> Optional<T> lastNonNull(final Collection<T> c) {
        if (N.isNullOrEmpty(c)) {
            return Optional.empty();
        }

        if (c instanceof List) {
            final List<T> list = (List<T>) c;

            if (c instanceof RandomAccess) {
                for (int i = c.size() - 1; i >= 0; i--) {
                    if (list.get(i) != null) {
                        return Optional.of(list.get(i));
                    }
                }
            } else {
                final ListIterator<T> iter = list.listIterator(list.size());
                T pre = null;

                while (iter.hasPrevious()) {
                    if ((pre = iter.previous()) != null) {
                        return Optional.of(pre);
                    }
                }
            }
        } else if (c instanceof Deque) {
            final Iterator<T> iter = ((Deque<T>) c).descendingIterator();
            T next = null;

            while (iter.hasNext()) {
                if ((next = iter.next()) != null) {
                    return Optional.of(next);
                }
            }
        } else {
            //    @SuppressWarnings("unchecked")
            //    final T[] a = (T[]) c.toArray();
            //    return lastNonNull(a);

            Iterators.lastNonNull(c.iterator());
        }

        return Optional.empty();
    }

    public static <K, V> Optional<Map.Entry<K, V>> firstEntry(final Map<K, V> map) {
        if (map == null || map.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(map.entrySet().iterator().next());
    }

    public static <K, V> Optional<Map.Entry<K, V>> lastEntry(final Map<K, V> map) {
        if (map == null || map.isEmpty()) {
            return Optional.empty();
        }

        return Iterators.lastNonNull(map.entrySet().iterator());
    }

    public static <T> T firstOrNullIfEmpty(final Collection<T> c) {
        if (N.isNullOrEmpty(c)) {
            return null;
        }

        if (c instanceof List && c instanceof RandomAccess) {
            return ((List<T>) c).get(0);
        } else {
            return c.iterator().next();
        }
    }

    public static <T> T lastOrNullIfEmpty(final Collection<T> c) {
        if (N.isNullOrEmpty(c)) {
            return null;
        }

        if (c instanceof List) {
            final List<T> list = (List<T>) c;

            if (c instanceof RandomAccess) {
                return list.get(c.size() - 1);
            } else {
                return list.listIterator(list.size()).previous();
            }
        } else if (c instanceof Deque) {
            return ((Deque<T>) c).descendingIterator().next();
        } else {
            final Iterator<T> iter = c.iterator();
            T e = null;

            while (iter.hasNext()) {
                e = iter.next();
            }

            return e;
        }
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map/CharSequence}, or {@code 0} if it's empty or {@code null}.
     *
     * @param s
     * @return
     */
    public static int len(final CharSequence s) {
        return s == null ? 0 : s.length();
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map/CharSequence}, or {@code 0} if it's empty or {@code null}.
     *
     * @param a
     * @return
     */
    public static int len(final boolean[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map/CharSequence}, or {@code 0} if it's empty or {@code null}.
     *
     * @param a
     * @return
     */
    public static int len(final char[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map/CharSequence}, or {@code 0} if it's empty or {@code null}.
     *
     * @param a
     * @return
     */
    public static int len(final byte[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map/CharSequence}, or {@code 0} if it's empty or {@code null}.
     *
     * @param a
     * @return
     */
    public static int len(final short[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map/CharSequence}, or {@code 0} if it's empty or {@code null}.
     *
     * @param a
     * @return
     */
    public static int len(final int[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map/CharSequence}, or {@code 0} if it's empty or {@code null}.
     *
     * @param a
     * @return
     */
    public static int len(final long[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map/CharSequence}, or {@code 0} if it's empty or {@code null}.
     *
     * @param a
     * @return
     */
    public static int len(final float[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map/CharSequence}, or {@code 0} if it's empty or {@code null}.
     *
     * @param a
     * @return
     */
    public static int len(final double[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map/CharSequence}, or {@code 0} if it's empty or {@code null}.
     *
     * @param a
     * @return
     */
    public static int len(final Object[] a) {
        return a == null ? 0 : a.length;
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map/CharSequence}, or {@code 0} if it's empty or {@code null}.
     *
     * @param c
     * @return
     */
    public static int size(final Collection<?> c) {
        return c == null ? 0 : c.size();
    }

    /**
     * Returns the length/size of the specified {@code Array/Collection/Map/CharSequence}, or {@code 0} if it's empty or {@code null}.
     *
     * @param m
     * @return
     */
    public static int size(final Map<?, ?> m) {
        return m == null ? 0 : m.size();
    } 


    /**
     * Returns an immutable empty list if the specified List is <code>null</code>, otherwise itself is returned.
     * 
     * @param list
     * @return
     */
    public static <T> List<T> nullToEmpty(final List<T> list) {
        return list == null ? N.<T> emptyList() : list;
    }

    /**
     * Returns an immutable empty set if the specified Set is <code>null</code>, otherwise itself is returned.
     * 
     * @param set
     * @return
     */
    public static <T> Set<T> nullToEmpty(final Set<T> set) {
        return set == null ? N.<T> emptySet() : set;
    }

    /**
     * Returns an immutable empty <code>SortedSet</code> if the specified SortedSet is <code>null</code>, otherwise itself is returned.
     * 
     * @param set
     * @return
     */
    public static <T> SortedSet<T> nullToEmpty(final SortedSet<T> set) {
        return set == null ? N.<T> emptySortedSet() : set;
    }

    /**
     * Returns an immutable empty <code>NavigableSet</code> if the specified NavigableSet is <code>null</code>, otherwise itself is returned.
     * 
     * @param set
     * @return
     */
    public static <T> NavigableSet<T> nullToEmpty(final NavigableSet<T> set) {
        return set == null ? N.<T> emptyNavigableSet() : set;
    }

    /**
     * Returns an immutable empty map if the specified Map is <code>null</code>, otherwise itself is returned.
     * 
     * @param map
     * @return
     */
    public static <K, V> Map<K, V> nullToEmpty(final Map<K, V> map) {
        return map == null ? N.<K, V> emptyMap() : map;
    }

    /**
     * Returns an immutable empty <code>SortedMap</code> if the specified SortedMap is <code>null</code>, otherwise itself is returned.
     * 
     * @param map
     * @return
     */
    public static <K, V> SortedMap<K, V> nullToEmpty(final SortedMap<K, V> map) {
        return map == null ? N.<K, V> emptySortedMap() : map;
    }

    /**
     * Returns an immutable empty <code>NavigableMap</code> if the specified NavigableMap is <code>null</code>, otherwise itself is returned.
     * 
     * @param map
     * @return
     */
    public static <K, V> NavigableMap<K, V> nullToEmpty(final NavigableMap<K, V> map) {
        return map == null ? N.<K, V> emptyNavigableMap() : map;
    }

    /**
     * Returns an immutable empty <code>Iterator</code> if the specified Iterator is <code>null</code>, otherwise itself is returned.
     * 
     * @param iter
     * @return
     */
    public static <T> Iterator<T> nullToEmpty(final Iterator<T> iter) {
        return iter == null ? N.<T> emptyIterator() : iter;
    }

    /**
     * Returns an immutable empty <code>ListIterator</code> if the specified ListIterator is <code>null</code>, otherwise itself is returned.
     * 
     * @param iter
     * @return
     */
    public static <T> ListIterator<T> nullToEmpty(final ListIterator<T> iter) {
        return iter == null ? N.<T> emptyListIterator() : iter;
    }

    public static String nullToEmpty(final String str) {
        return str == null ? EMPTY_STRING : str;
    }

    public static boolean[] nullToEmpty(final boolean[] a) {
        return a == null ? EMPTY_BOOLEAN_ARRAY : a;
    }

    public static char[] nullToEmpty(final char[] a) {
        return a == null ? EMPTY_CHAR_ARRAY : a;
    }

    public static byte[] nullToEmpty(final byte[] a) {
        return a == null ? EMPTY_BYTE_ARRAY : a;
    }

    public static short[] nullToEmpty(final short[] a) {
        return a == null ? EMPTY_SHORT_ARRAY : a;
    }

    public static int[] nullToEmpty(final int[] a) {
        return a == null ? EMPTY_INT_ARRAY : a;
    }

    public static long[] nullToEmpty(final long[] a) {
        return a == null ? EMPTY_LONG_ARRAY : a;
    }

    public static float[] nullToEmpty(final float[] a) {
        return a == null ? EMPTY_FLOAT_ARRAY : a;
    }

    public static double[] nullToEmpty(final double[] a) {
        return a == null ? EMPTY_DOUBLE_ARRAY : a;
    }

    public static String[] nullToEmpty(final String[] a) {
        return a == null ? EMPTY_STRING_ARRAY : a;
    }

    /**
     * 
     * @param a
     * @return
     * @deprecated replaced by {@link N#nullToEmpty(Class, Object[])}
     */
    @Deprecated
    public static Object[] nullToEmpty(final Object[] a) {
        return a == null ? EMPTY_OBJECT_ARRAY : a;
    }

    public static <T> T[] nullToEmpty(final Class<T[]> arrayType, final T[] a) {
        return a == null ? (T[]) N.newArray(arrayType.getComponentType(), 0) : a;
    }

    public static boolean isNullOrEmpty(final CharSequence s) {
        return (s == null) || (s.length() == 0);
    }

    public static boolean isNullOrEmpty(final boolean[] a) {
        return (a == null) || (a.length == 0);
    }

    public static boolean isNullOrEmpty(final char[] a) {
        return (a == null) || (a.length == 0);
    }

    public static boolean isNullOrEmpty(final byte[] a) {
        return (a == null) || (a.length == 0);
    }

    public static boolean isNullOrEmpty(final short[] a) {
        return (a == null) || (a.length == 0);
    }

    public static boolean isNullOrEmpty(final int[] a) {
        return (a == null) || (a.length == 0);
    }

    public static boolean isNullOrEmpty(final long[] a) {
        return (a == null) || (a.length == 0);
    }

    public static boolean isNullOrEmpty(final float[] a) {
        return (a == null) || (a.length == 0);
    }

    public static boolean isNullOrEmpty(final double[] a) {
        return (a == null) || (a.length == 0);
    }

    public static boolean isNullOrEmpty(final Object[] a) {
        return (a == null) || (a.length == 0);
    }

    public static boolean isNullOrEmpty(final Collection<?> c) {
        return (c == null) || (c.isEmpty());
    }

    public static boolean isNullOrEmpty(final Map<?, ?> m) {
        return (m == null) || (m.isEmpty());
    }

    @SuppressWarnings("rawtypes")
    public static boolean isNullOrEmpty(final PrimitiveList list) {
        return (list == null) || (list.isEmpty());
    }

    public static boolean isNullOrEmpty(final Multiset<?> s) {
        return (s == null) || (s.isEmpty());
    }

    public static boolean isNullOrEmpty(final LongMultiset<?> s) {
        return (s == null) || (s.isEmpty());
    }

    public static boolean isNullOrEmpty(final Multimap<?, ?, ?> m) {
        return (m == null) || (m.isEmpty());
    }

    public static boolean isNullOrEmpty(final DataSet rs) {
        return (rs == null) || (rs.isEmpty());
    }

    public static boolean isNullOrEmpty(final EntityId entityId) {
        return (entityId == null) || (entityId.isEmpty());
    }

    // DON'T change 'OrEmptyOrBlank' to 'OrBlank' because of the occurring order in the auto-completed context menu.
    public static boolean isNullOrEmptyOrBlank(final CharSequence s) {
        if (N.isNullOrEmpty(s)) {
            return true;
        }

        for (int i = 0, len = s.length(); i < len; i++) {
            if (Character.isWhitespace(s.charAt(i)) == false) {
                return false;
            }
        }

        return true;
    }

    public static boolean notNullOrEmpty(final CharSequence s) {
        return (s != null) && (s.length() > 0);
    }

    public static boolean notNullOrEmpty(final boolean[] a) {
        return (a != null) && (a.length > 0);
    }

    public static boolean notNullOrEmpty(final char[] a) {
        return (a != null) && (a.length > 0);
    }

    public static boolean notNullOrEmpty(final byte[] a) {
        return (a != null) && (a.length > 0);
    }

    public static boolean notNullOrEmpty(final short[] a) {
        return (a != null) && (a.length > 0);
    }

    public static boolean notNullOrEmpty(final int[] a) {
        return (a != null) && (a.length > 0);
    }

    public static boolean notNullOrEmpty(final long[] a) {
        return (a != null) && (a.length > 0);
    }

    public static boolean notNullOrEmpty(final float[] a) {
        return (a != null) && (a.length > 0);
    }

    public static boolean notNullOrEmpty(final double[] a) {
        return (a != null) && (a.length > 0);
    }

    public static boolean notNullOrEmpty(final Object[] a) {
        return (a != null) && (a.length > 0);
    }

    public static boolean notNullOrEmpty(final Collection<?> c) {
        return (c != null) && (c.size() > 0);
    }

    public static boolean notNullOrEmpty(final Map<?, ?> m) {
        return (m != null) && (m.size() > 0);
    }

    @SuppressWarnings("rawtypes")
    public static boolean notNullOrEmpty(final PrimitiveList list) {
        return (list != null) && (list.size() > 0);
    }

    public static boolean notNullOrEmpty(final Multiset<?> s) {
        return (s != null) && (s.size() > 0);
    }

    public static boolean notNullOrEmpty(final LongMultiset<?> s) {
        return (s != null) && (s.size() > 0);
    }

    public static boolean notNullOrEmpty(final Multimap<?, ?, ?> m) {
        return (m != null) && (m.size() > 0);
    }

    public static boolean notNullOrEmpty(final DataSet rs) {
        return (rs != null) && (rs.size() > 0);
    }

    public static boolean notNullOrEmpty(final EntityId entityId) {
        return (entityId != null) && (!entityId.isEmpty());
    }

    // DON'T change 'OrEmptyOrBlank' to 'OrBlank' because of the occurring order in the auto-completed context menu.
    public static boolean notNullOrEmptyOrBlank(final CharSequence s) {
        return !N.isNullOrEmptyOrBlank(s);
    }

    private static boolean isNullErrorMsg(final String msg) {
        // shortest message: "it is null"
        return msg.length() > 9 && msg.indexOf(WD._SPACE) > 0;
    }

    /**
     *
     * @param index
     * @param length
     * @throws IndexOutOfBoundsException
     */
    public static void checkIndex(final int index, final int length) throws IndexOutOfBoundsException {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index " + index + " is out-of-bounds for length " + length);
        }
    }

    /**
     *
     * @param fromIndex
     * @param toIndex
     * @param length
     * @throws IndexOutOfBoundsException
     */
    public static void checkFromToIndex(final int fromIndex, final int toIndex, final int length) throws IndexOutOfBoundsException {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length) {
            throw new IndexOutOfBoundsException("Index range [" + fromIndex + ", " + toIndex + "] is out-of-bounds for length " + length);
        }
    }

    /**
     *
     * @param fromIndex
     * @param size
     * @param length
     * @throws IndexOutOfBoundsException
     */
    public static void checkFromIndexSize(final int fromIndex, final int size, final int length) throws IndexOutOfBoundsException {
        if ((fromIndex < 0 || size < 0 || length < 0) || size > length - fromIndex) {
            throw new IndexOutOfBoundsException("Start Index " + fromIndex + " with size " + size + " is out-of-bounds for length " + length);
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression a boolean expression
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkArgument(boolean expression) {
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
    public static void checkArgument(boolean expression, String errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, boolean p) {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, boolean p1, boolean p2) {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, boolean p1, boolean p2, boolean p3) {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2, p3));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, char p) {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, byte p) {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, short p) {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, int p) {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, int p1, int p2) {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, int p1, int p2, int p3) {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2, p3));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, long p) {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, long p1, long p2) {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, long p1, long p2, long p3) {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2, p3));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, float p) {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, float p1, float p2) {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, float p1, float p2, float p3) {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2, p3));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, double p) {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, double p1, double p2) {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, double p1, double p2, double p3) {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2, p3));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, Object p) {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, Object p1, Object p2) {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2));
        }
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * <p>See {@link #checkArgument(boolean, String, Object...)} for details.
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, Object p1, Object p2, Object p3) {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2, p3));
        }
    }

    public static <E extends Exception> void checkArgument(boolean b, Try.Supplier<String, E> errorMessageSupplier) throws E {
        if (!b) {
            throw new IllegalArgumentException(errorMessageSupplier.get());
        }
    }

    /**
     *
     * @param obj
     * @return
     * @throws IllegalArgumentException if {@code obj} is {@code null}
     */
    public static <T> T checkArgNotNull(final T obj) {
        if (obj == null) {
            throw new IllegalArgumentException();
        }

        return obj;
    }

    /**
     *
     * @param obj
     * @param errorMessage
     * @return
     * @throws IllegalArgumentException if {@code obj} is {@code null}
     */
    public static <T> T checkArgNotNull(final T obj, final String errorMessage) {
        if (obj == null) {
            if (isNullErrorMsg(errorMessage)) {
                throw new IllegalArgumentException(errorMessage);
            } else {
                throw new IllegalArgumentException("'" + errorMessage + "' can not be null");
            }
        }

        return obj;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static <T extends CharSequence> T checkArgNotNullOrEmpty(final T arg, final String argNameOrErrorMsg) {
        if (N.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == N.INDEX_NOT_FOUND) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be null or empty");
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static boolean[] checkArgNotNullOrEmpty(final boolean[] arg, final String argNameOrErrorMsg) {
        if (N.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == N.INDEX_NOT_FOUND) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be null or empty");
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static char[] checkArgNotNullOrEmpty(final char[] arg, final String argNameOrErrorMsg) {
        if (N.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == N.INDEX_NOT_FOUND) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be null or empty");
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static byte[] checkArgNotNullOrEmpty(final byte[] arg, final String argNameOrErrorMsg) {
        if (N.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == N.INDEX_NOT_FOUND) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be null or empty");
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static short[] checkArgNotNullOrEmpty(final short[] arg, final String argNameOrErrorMsg) {
        if (N.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == N.INDEX_NOT_FOUND) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be null or empty");
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static int[] checkArgNotNullOrEmpty(final int[] arg, final String argNameOrErrorMsg) {
        if (N.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == N.INDEX_NOT_FOUND) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be null or empty");
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static long[] checkArgNotNullOrEmpty(final long[] arg, final String argNameOrErrorMsg) {
        if (N.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == N.INDEX_NOT_FOUND) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be null or empty");
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static float[] checkArgNotNullOrEmpty(final float[] arg, final String argNameOrErrorMsg) {
        if (N.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == N.INDEX_NOT_FOUND) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be null or empty");
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static double[] checkArgNotNullOrEmpty(final double[] arg, final String argNameOrErrorMsg) {
        if (N.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == N.INDEX_NOT_FOUND) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be null or empty");
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static <T> T[] checkArgNotNullOrEmpty(final T[] arg, final String argNameOrErrorMsg) {
        if (N.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == N.INDEX_NOT_FOUND) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be null or empty");
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static <T extends Collection<?>> T checkArgNotNullOrEmpty(final T arg, final String argNameOrErrorMsg) {
        if (N.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == N.INDEX_NOT_FOUND) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be null or empty");
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is {@code null} or empty, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static <T extends Map<?, ?>> T checkArgNotNullOrEmpty(final T arg, final String argNameOrErrorMsg) {
        if (N.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == N.INDEX_NOT_FOUND) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be null or empty");
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Check if the specified parameter is null or empty or blank
     *
     * @param arg
     * @param msg name of parameter or error message
     * @return the input parameter
     * @throws IllegalArgumentException if the specified parameter is null or empty.
     */
    // DON'T change 'OrEmptyOrBlank' to 'OrBlank' because of the occurring order in the auto-completed context menu.
    public static <T extends CharSequence> T checkArgNotNullOrEmptyOrBlank(final T arg, final String msg) {
        if (N.isNullOrEmptyOrBlank(arg)) {
            if (isNullErrorMsg(msg)) {
                throw new IllegalArgumentException(msg);
            } else {
                throw new IllegalArgumentException("'" + msg + "' can not be null or empty or blank");
            }
        }

        return arg;
    }

    /**
     * Checks if the specified {@code arg} is not negative, and throws {@code IllegalArgumentException} if it is.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @throws IllegalArgumentException if the specified {@code arg} is negative.
     */
    public static int checkArgNotNegative(final int arg, final String argNameOrErrorMsg) {
        if (arg < 0) {
            if (argNameOrErrorMsg.indexOf(' ') == N.INDEX_NOT_FOUND) {
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
     * @throws IllegalArgumentException if the specified {@code arg} is negative.
     */
    public static long checkArgNotNegative(final long arg, final String argNameOrErrorMsg) {
        if (arg < 0) {
            if (argNameOrErrorMsg.indexOf(' ') == N.INDEX_NOT_FOUND) {
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
     * @throws IllegalArgumentException if the specified {@code arg} is negative.
     */
    public static double checkArgNotNegative(final double arg, final String argNameOrErrorMsg) {
        if (arg < 0) {
            if (argNameOrErrorMsg.indexOf(' ') == N.INDEX_NOT_FOUND) {
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
     * @throws IllegalArgumentException if the specified {@code arg} is negative.
     */
    public static int checkArgPositive(final int arg, final String argNameOrErrorMsg) {
        if (arg <= 0) {
            if (argNameOrErrorMsg.indexOf(' ') == N.INDEX_NOT_FOUND) {
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
     * @throws IllegalArgumentException if the specified {@code arg} is negative.
     */
    public static long checkArgPositive(final long arg, final String argNameOrErrorMsg) {
        if (arg <= 0) {
            if (argNameOrErrorMsg.indexOf(' ') == N.INDEX_NOT_FOUND) {
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
     * @throws IllegalArgumentException if the specified {@code arg} is negative.
     */
    public static double checkArgPositive(final double arg, final String argNameOrErrorMsg) {
        if (arg <= 0) {
            if (argNameOrErrorMsg.indexOf(' ') == N.INDEX_NOT_FOUND) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be zero or negative: " + arg);
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
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
     */
    public static void checkState(boolean b, String errorMessageTemplate, Object p1, Object p2, Object p3) {
        if (!b) {
            throw new IllegalStateException(format(errorMessageTemplate, p1, p2, p3));
        }
    }

    public static <E extends Exception> void checkState(boolean b, Try.Supplier<String, E> errorMessageSupplier) throws E {
        if (!b) {
            throw new IllegalStateException(errorMessageSupplier.get());
        }
    }

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
            sb.append(N.toString(arg));
            sb.append(template, placeholderStart + 2, template.length());
        } else {
            sb.append(" [");
            sb.append(N.toString(arg));
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }

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
            sb.append(N.toString(arg1));
            templateStart = placeholderStart + 2;
            placeholderStart = template.indexOf(placeholder, templateStart);

            if (placeholderStart >= 0) {
                cnt++;
                sb.append(template, templateStart, placeholderStart);
                sb.append(N.toString(arg2));
                templateStart = placeholderStart + 2;
            }

            sb.append(template, templateStart, template.length());
        }

        if (cnt == 0) {
            sb.append(" [");
            sb.append(N.toString(arg1));
            sb.append(", ");
            sb.append(N.toString(arg2));
            sb.append(']');
        } else if (cnt == 1) {
            sb.append(" [");
            sb.append(N.toString(arg2));
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }

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
            sb.append(N.toString(arg1));
            templateStart = placeholderStart + 2;
            placeholderStart = template.indexOf(placeholder, templateStart);

            if (placeholderStart >= 0) {
                cnt++;
                sb.append(template, templateStart, placeholderStart);
                sb.append(N.toString(arg2));
                templateStart = placeholderStart + 2;
                placeholderStart = template.indexOf(placeholder, templateStart);

                if (placeholderStart >= 0) {
                    cnt++;
                    sb.append(template, templateStart, placeholderStart);
                    sb.append(N.toString(arg3));
                    templateStart = placeholderStart + 2;
                }
            }

            sb.append(template, templateStart, template.length());
        }

        if (cnt == 0) {
            sb.append(" [");
            sb.append(N.toString(arg1));
            sb.append(", ");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(']');
        } else if (cnt == 1) {
            sb.append(" [");
            sb.append(N.toString(arg2));
            sb.append(", ");
            sb.append(N.toString(arg3));
            sb.append(']');
        } else if (cnt == 2) {
            sb.append(" [");
            sb.append(N.toString(arg3));
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
     */
    // Note that this is somewhat-improperly used from Verify.java as well.
    static String format(String template, Object... args) {
        template = String.valueOf(template); // null -> "null"

        if (N.isNullOrEmpty(args)) {
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
            sb.append(N.toString(args[i++]));
            templateStart = placeholderStart + 2;
            placeholderStart = template.indexOf(placeholder, templateStart);
        }

        sb.append(template, templateStart, template.length());

        // if we run out of placeholders, append the extra args in square braces
        if (i < args.length) {
            sb.append(" [");
            sb.append(N.toString(args[i++]));
            while (i < args.length) {
                sb.append(", ");
                sb.append(N.toString(args[i++]));
            }
            sb.append(']');
        }

        final String result = sb.toString();

        Objectory.recycle(sb);

        return result;
    }

    public static int compare(final boolean a, final boolean b) {
        return (a == b) ? 0 : (a ? 1 : -1);
    }

    public static int compare(final byte a, final byte b) {
        return (a < b) ? -1 : ((a == b) ? 0 : 1);
    }

    public static int compare(final short a, final short b) {
        return (a < b) ? -1 : ((a == b) ? 0 : 1);
    }

    public static int compare(final int a, final int b) {
        return (a < b) ? -1 : ((a == b) ? 0 : 1);
    }

    public static int compare(final long a, final long b) {
        return (a < b) ? -1 : ((a == b) ? 0 : 1);
    }

    public static int compare(final float a, final float b) {
        return Float.compare(a, b);
    }

    public static int compare(final double a, final double b) {
        return Double.compare(a, b);
    }

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
        return a == null ? (b == null ? 0 : -1) : (b == null ? 1 : (cmp == null ? NATURAL_ORDER : cmp).compare(a, b));
    }

    /**
     * Continue to compare the pairs of values <code>(a1, b1), (a2, b2)</code> until they're not equal.
     * <code>0</code> is returned if all of the pairs of values are equal.
     *
     * @param a1
     * @param b1
     * @param a2
     * @param b2
     * @return
     */
    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>> int compare(T1 a1, T1 b1, T2 a2, T2 b2) {
        int res = N.compare(a1, b1);

        return res == 0 ? N.compare(a2, b2) : res;
    }

    /**
     * Continue to compare the pairs of values <code>(a1, b1), (a2, b2), (a3, b3)</code> until they're not equal.
     * <code>0</code> is returned if all of the pairs of values are equal.
     *
     * @param a1
     * @param b1
     * @param a2
     * @param b2
     * @param a3
     * @param b3
     * @return
     */
    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> int compare(T1 a1, T1 b1, T2 a2, T2 b2, T3 a3, T3 b3) {
        int res = 0;

        if ((res = N.compare(a1, b1)) != 0) {
            return res;
        } else if ((res = N.compare(a2, b2)) != 0) {
            return res;
        }

        return N.compare(a3, b3);
    }

    /**
     * Continue to compare the pairs of values <code>(a1, b1), (a2, b2), (a3, b3), (a4, b4)</code> until they're not equal.
     * <code>0</code> is returned if all of the pairs of values are equal.
     *
     * @param a1
     * @param b1
     * @param a2
     * @param b2
     * @param a3
     * @param b3
     * @param a4
     * @param b4
     * @return
     */
    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> int compare(T1 a1, T1 b1, T2 a2,
            T2 b2, T3 a3, T3 b3, T4 a4, T4 b4) {
        int res = 0;

        if ((res = N.compare(a1, b1)) != 0) {
            return res;
        } else if ((res = N.compare(a2, b2)) != 0) {
            return res;
        } else if ((res = N.compare(a3, b3)) != 0) {
            return res;
        }

        return N.compare(a4, b4);
    }

    /**
     * Continue to compare the pairs of values <code>(a1, b1), (a2, b2), (a3, b3), (a4, b4), (a5, b5)</code> until they're not equal.
     * <code>0</code> is returned if all of the pairs of values are equal.
     *
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
     */
    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> int compare(
            T1 a1, T1 b1, T2 a2, T2 b2, T3 a3, T3 b3, T4 a4, T4 b4, T5 a5, T5 b5) {
        int res = 0;

        if ((res = N.compare(a1, b1)) != 0) {
            return res;
        } else if ((res = N.compare(a2, b2)) != 0) {
            return res;
        } else if ((res = N.compare(a3, b3)) != 0) {
            return res;
        } else if ((res = N.compare(a4, b4)) != 0) {
            return res;
        }

        return N.compare(a5, b5);
    }

    /**
     * Continue to compare the pairs of values <code>(a1, b1), (a2, b2), (a3, b3), (a4, b4), (a5, b5), (a6, b6)</code> until they're not equal.
     * <code>0</code> is returned if all of the pairs of values are equal.
     *
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
     */
    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>, T6 extends Comparable<T6>> int compare(
            T1 a1, T1 b1, T2 a2, T2 b2, T3 a3, T3 b3, T4 a4, T4 b4, T5 a5, T5 b5, T6 a6, T6 b6) {
        int res = 0;

        if ((res = N.compare(a1, b1)) != 0) {
            return res;
        } else if ((res = N.compare(a2, b2)) != 0) {
            return res;
        } else if ((res = N.compare(a3, b3)) != 0) {
            return res;
        } else if ((res = N.compare(a4, b4)) != 0) {
            return res;
        } else if ((res = N.compare(a5, b5)) != 0) {
            return res;
        }

        return N.compare(a6, b6);
    }

    /**
     * Continue to compare the pairs of values <code>(a1, b1), (a2, b2), (a3, b3), (a4, b4), (a5, b5), (a6, b6), (a7, b7)</code> until they're not equal.
     * <code>0</code> is returned if all of the pairs of values are equal.
     *
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
     */
    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>, T6 extends Comparable<T6>, T7 extends Comparable<T7>> int compare(
            T1 a1, T1 b1, T2 a2, T2 b2, T3 a3, T3 b3, T4 a4, T4 b4, T5 a5, T5 b5, T6 a6, T6 b6, T7 a7, T7 b7) {
        int res = 0;

        if ((res = N.compare(a1, b1)) != 0) {
            return res;
        } else if ((res = N.compare(a2, b2)) != 0) {
            return res;
        } else if ((res = N.compare(a3, b3)) != 0) {
            return res;
        } else if ((res = N.compare(a4, b4)) != 0) {
            return res;
        } else if ((res = N.compare(a5, b5)) != 0) {
            return res;
        } else if ((res = N.compare(a6, b6)) != 0) {
            return res;
        }

        return N.compare(a7, b7);
    }

    public static int compare(final boolean[] a, final boolean[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? 0 : -1;
        } else if (N.isNullOrEmpty(b)) {
            return 1;
        }

        for (int i = 0, minLen = min(a.length, b.length); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] ? 1 : -1;
            }
        }

        return a.length - b.length;
    }

    public static int compare(final boolean[] a, final int fromIndexA, final boolean[] b, final int fromIndexB, final int len) {
        N.checkArgNotNegative(len, "len");
        N.checkFromIndexSize(fromIndexA, len, len(a));
        N.checkFromIndexSize(fromIndexB, len, len(b));

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

    public static int compare(final char[] a, final char[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? 0 : -1;
        } else if (N.isNullOrEmpty(b)) {
            return 1;
        }

        for (int i = 0, minLen = min(a.length, b.length); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return a.length - b.length;
    }

    public static int compare(final char[] a, final int fromIndexA, final char[] b, final int fromIndexB, final int len) {
        N.checkArgNotNegative(len, "len");
        N.checkFromIndexSize(fromIndexA, len, len(a));
        N.checkFromIndexSize(fromIndexB, len, len(b));

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

    public static int compare(final byte[] a, final byte[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? 0 : -1;
        } else if (N.isNullOrEmpty(b)) {
            return 1;
        }

        for (int i = 0, minLen = min(a.length, b.length); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return a.length - b.length;
    }

    public static int compare(final byte[] a, final int fromIndexA, final byte[] b, final int fromIndexB, final int len) {
        N.checkArgNotNegative(len, "len");
        N.checkFromIndexSize(fromIndexA, len, len(a));
        N.checkFromIndexSize(fromIndexB, len, len(b));

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

    public static int compare(final short[] a, final short[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? 0 : -1;
        } else if (N.isNullOrEmpty(b)) {
            return 1;
        }

        for (int i = 0, minLen = min(a.length, b.length); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return a.length - b.length;
    }

    public static int compare(final short[] a, final int fromIndexA, final short[] b, final int fromIndexB, final int len) {
        N.checkArgNotNegative(len, "len");
        N.checkFromIndexSize(fromIndexA, len, len(a));
        N.checkFromIndexSize(fromIndexB, len, len(b));

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

    public static int compare(final int[] a, final int[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? 0 : -1;
        } else if (N.isNullOrEmpty(b)) {
            return 1;
        }

        for (int i = 0, minLen = min(a.length, b.length); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return a.length - b.length;
    }

    public static int compare(final int[] a, final int fromIndexA, final int[] b, final int fromIndexB, final int len) {
        N.checkArgNotNegative(len, "len");
        N.checkFromIndexSize(fromIndexA, len, len(a));
        N.checkFromIndexSize(fromIndexB, len, len(b));

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

    public static int compare(final long[] a, final long[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? 0 : -1;
        } else if (N.isNullOrEmpty(b)) {
            return 1;
        }

        for (int i = 0, minLen = min(a.length, b.length); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return a.length - b.length;
    }

    public static int compare(final long[] a, final int fromIndexA, final long[] b, final int fromIndexB, final int len) {
        N.checkArgNotNegative(len, "len");
        N.checkFromIndexSize(fromIndexA, len, len(a));
        N.checkFromIndexSize(fromIndexB, len, len(b));

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

    public static int compare(final float[] a, final float[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? 0 : -1;
        } else if (N.isNullOrEmpty(b)) {
            return 1;
        }

        int value = 0;

        for (int i = 0, minLen = min(a.length, b.length); i < minLen; i++) {
            if ((value = Float.compare(a[i], b[i])) != 0) {
                return value;
            }
        }

        return a.length - b.length;
    }

    public static int compare(final float[] a, final int fromIndexA, final float[] b, final int fromIndexB, final int len) {
        N.checkArgNotNegative(len, "len");
        N.checkFromIndexSize(fromIndexA, len, len(a));
        N.checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return 0;
        }

        int value = 0;

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if ((value = Float.compare(a[i], b[j])) != 0) {
                return value;
            }
        }

        return 0;
    }

    public static int compare(final double[] a, final double[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? 0 : -1;
        } else if (N.isNullOrEmpty(b)) {
            return 1;
        }

        int value = 0;

        for (int i = 0, minLen = min(a.length, b.length); i < minLen; i++) {
            if ((value = Double.compare(a[i], b[i])) != 0) {
                return value;
            }
        }

        return a.length - b.length;
    }

    public static int compare(final double[] a, final int fromIndexA, final double[] b, final int fromIndexB, final int len) {
        N.checkArgNotNegative(len, "len");
        N.checkFromIndexSize(fromIndexA, len, len(a));
        N.checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return 0;
        }

        int value = 0;

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if ((value = Double.compare(a[i], b[j])) != 0) {
                return value;
            }
        }

        return 0;
    }

    public static <T extends Comparable<? super T>> int compare(final T[] a, final T[] b) {
        final Comparator<T> cmp = NATURAL_ORDER;

        return compare(a, b, cmp);
    }

    public static <T extends Comparable<? super T>> int compare(final T[] a, final int fromIndexA, final T[] b, final int fromIndexB, final int len) {
        final Comparator<T> cmp = NATURAL_ORDER;

        return compare(a, fromIndexA, b, fromIndexB, len, cmp);
    }

    public static <T> int compare(final T[] a, final T[] b, Comparator<? super T> cmp) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? 0 : -1;
        } else if (N.isNullOrEmpty(b)) {
            return 1;
        }

        cmp = cmp == null ? NATURAL_ORDER : cmp;

        int value = 0;

        for (int i = 0, minLen = min(a.length, b.length); i < minLen; i++) {
            if ((value = cmp.compare(a[i], b[i])) != 0) {
                return value;
            }
        }

        return a.length - b.length;
    }

    public static <T> int compare(final T[] a, final int fromIndexA, final T[] b, final int fromIndexB, final int len, Comparator<? super T> cmp) {
        N.checkArgNotNegative(len, "len");
        N.checkFromIndexSize(fromIndexA, len, len(a));
        N.checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return 0;
        }

        cmp = cmp == null ? NATURAL_ORDER : cmp;

        int value = 0;

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if ((value = cmp.compare(a[i], b[j])) != 0) {
                return value;
            }
        }

        return 0;
    }

    public static <T extends Comparable<? super T>> int compare(final Collection<T> a, final Collection<T> b) {
        final Comparator<T> cmp = NATURAL_ORDER;

        return compare(a, b, cmp);
    }

    public static <T> int compare(final Collection<T> a, final Collection<T> b, Comparator<? super T> cmp) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? 0 : -1;
        } else if (N.isNullOrEmpty(b)) {
            return 1;
        }

        cmp = cmp == null ? NATURAL_ORDER : cmp;

        final Iterator<T> iterA = a.iterator();
        final Iterator<T> iterB = b.iterator();
        int value = 0;

        for (int i = 0, minLen = min(a.size(), b.size()); i < minLen; i++) {
            if ((value = cmp.compare(iterA.next(), iterB.next())) != 0) {
                return value;
            }
        }

        return a.size() - b.size();
    }

    public static <T> int compare(final Collection<T> a, int fromIndexA, final Collection<T> b, int fromIndexB, final int len, Comparator<? super T> cmp) {
        N.checkArgNotNegative(len, "len");
        N.checkFromIndexSize(fromIndexA, len, size(a));
        N.checkFromIndexSize(fromIndexB, len, size(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return 0;
        }

        cmp = cmp == null ? NATURAL_ORDER : cmp;
        final Iterator<T> iterA = a.iterator();
        final Iterator<T> iterB = b.iterator();

        while (fromIndexA-- > 0) {
            iterA.next();
        }

        while (fromIndexB-- > 0) {
            iterB.next();
        }

        int value = 0;

        for (int i = 0; i < len; i++) {
            if ((value = cmp.compare(iterA.next(), iterB.next())) != 0) {
                return value;
            }
        }

        return 0;
    }

    public static int compareIgnoreCase(final String a, final String b) {
        return a == null ? (b == null ? 0 : -1) : (b == null ? 1 : a.compareToIgnoreCase(b));
    }

    /**
     * Method equals.
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final boolean a, final boolean b) {
        return a == b;
    }

    /**
     * Method equals.
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final char a, final char b) {
        return a == b;
    }

    /**
     * Method equals.
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final byte a, final byte b) {
        return a == b;
    }

    /**
     * Method equals.
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final short a, final short b) {
        return a == b;
    }

    /**
     * Method equals.
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final int a, final int b) {
        return a == b;
    }

    /**
     * Method equals.
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final long a, final long b) {
        return a == b;
    }

    /**
     * Method equals.
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final float a, final float b) {
        return Float.compare(a, b) == 0;
    }

    /**
     * Method equals.
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
     * @see Arrays#equals(boolean[], boolean[])
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final boolean[] a, final boolean[] b) {
        return a == b || (a != null && b != null && a.length == b.length && equals(a, 0, b, 0, a.length));
    }

    /**
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     */
    public static boolean equals(final boolean[] a, final int fromIndexA, final boolean[] b, final int fromIndexB, final int len) {
        N.checkArgNotNegative(len, "len");
        N.checkFromIndexSize(fromIndexA, len, len(a));
        N.checkFromIndexSize(fromIndexB, len, len(b));

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
     * @see Arrays#equals(char[], char[])
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final char[] a, final char[] b) {
        return a == b || (a != null && b != null && a.length == b.length && equals(a, 0, b, 0, a.length));
    }

    /**
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     */
    public static boolean equals(final char[] a, final int fromIndexA, final char[] b, final int fromIndexB, final int len) {
        N.checkArgNotNegative(len, "len");
        N.checkFromIndexSize(fromIndexA, len, len(a));
        N.checkFromIndexSize(fromIndexB, len, len(b));

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
     * @see Arrays#equals(byte[], byte[])
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final byte[] a, final byte[] b) {
        return a == b || (a != null && b != null && a.length == b.length && equals(a, 0, b, 0, a.length));
    }

    /**
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     */
    public static boolean equals(final byte[] a, final int fromIndexA, final byte[] b, final int fromIndexB, final int len) {
        N.checkArgNotNegative(len, "len");
        N.checkFromIndexSize(fromIndexA, len, len(a));
        N.checkFromIndexSize(fromIndexB, len, len(b));

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
     * @see Arrays#equals(short[], short[])
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final short[] a, final short[] b) {
        return a == b || (a != null && b != null && a.length == b.length && equals(a, 0, b, 0, a.length));
    }

    /**
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     */
    public static boolean equals(final short[] a, final int fromIndexA, final short[] b, final int fromIndexB, final int len) {
        N.checkArgNotNegative(len, "len");
        N.checkFromIndexSize(fromIndexA, len, len(a));
        N.checkFromIndexSize(fromIndexB, len, len(b));

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
     * @see Arrays#equals(int[], int[])
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final int[] a, final int[] b) {
        return a == b || (a != null && b != null && a.length == b.length && equals(a, 0, b, 0, a.length));
    }

    /**
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     */
    public static boolean equals(final int[] a, final int fromIndexA, final int[] b, final int fromIndexB, final int len) {
        N.checkArgNotNegative(len, "len");
        N.checkFromIndexSize(fromIndexA, len, len(a));
        N.checkFromIndexSize(fromIndexB, len, len(b));

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
     * @see Arrays#equals(long[], long[])
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final long[] a, final long[] b) {
        return a == b || (a != null && b != null && a.length == b.length && equals(a, 0, b, 0, a.length));
    }

    /**
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     */
    public static boolean equals(final long[] a, final int fromIndexA, final long[] b, final int fromIndexB, final int len) {
        N.checkArgNotNegative(len, "len");
        N.checkFromIndexSize(fromIndexA, len, len(a));
        N.checkFromIndexSize(fromIndexB, len, len(b));

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
     * @see Arrays#equals(float[], float[])
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final float[] a, final float[] b) {
        return a == b || (a != null && b != null && a.length == b.length && equals(a, 0, b, 0, a.length));
    }

    /**
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     */
    public static boolean equals(final float[] a, final int fromIndexA, final float[] b, final int fromIndexB, final int len) {
        N.checkArgNotNegative(len, "len");
        N.checkFromIndexSize(fromIndexA, len, len(a));
        N.checkFromIndexSize(fromIndexB, len, len(b));

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
     * @see Arrays#equals(double[], double[])
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final double[] a, final double[] b) {
        return a == b || (a != null && b != null && a.length == b.length && equals(a, 0, b, 0, a.length));
    }

    /**
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     */
    public static boolean equals(final double[] a, final int fromIndexA, final double[] b, final int fromIndexB, final int len) {
        N.checkArgNotNegative(len, "len");
        N.checkFromIndexSize(fromIndexA, len, len(a));
        N.checkFromIndexSize(fromIndexB, len, len(b));

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
     * @see Arrays#equals(Object[], Object[])
     *
     * @param a
     * @param b
     * @return boolean
     */
    public static boolean equals(final Object[] a, final Object[] b) {
        return a == b || (a != null && b != null && a.length == b.length && equals(a, 0, b, 0, a.length));
    }

    /**
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     */
    public static boolean equals(final Object[] a, final int fromIndexA, final Object[] b, final int fromIndexB, final int len) {
        N.checkArgNotNegative(len, "len");
        N.checkFromIndexSize(fromIndexA, len, len(a));
        N.checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return true;
        } else if (a.getClass().equals(b.getClass()) == false) {
            return false;
        }

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if (N.equals(a[i], b[j]) == false) {
                return false;
            }
        }

        return true;
    }

    /**
     * @see Arrays#deepEquals(Object[], Object[])
     * @param a
     * @param b
     * @return
     */
    public static boolean deepEquals(final Object[] a, final Object[] b) {
        return a == b || (a != null && b != null && a.length == b.length && deepEquals(a, 0, b, 0, a.length));
    }

    public static boolean deepEquals(final Object[] a, final int fromIndexA, final Object[] b, final int fromIndexB, final int len) {
        N.checkArgNotNegative(len, "len");
        N.checkFromIndexSize(fromIndexA, len, len(a));
        N.checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return true;
        } else if (a.getClass().equals(b.getClass()) == false) {
            return false;
        }

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if (N.deepEquals(a[i], b[j]) == false) {
                return false;
            }
        }

        return true;
    }

    public static boolean equalsIgnoreCase(final String[] a, final String[] b) {
        return (a == null || b == null) ? a == b : (a.length == b.length && equalsIgnoreCase(a, 0, b, 0, a.length));
    }

    /**
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return
     */
    public static boolean equalsIgnoreCase(final String[] a, final int fromIndexA, final String[] b, final int fromIndexB, final int len) {
        N.checkArgNotNegative(len, "len");
        N.checkFromIndexSize(fromIndexA, len, len(a));
        N.checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return true;
        } else if (a.getClass().equals(b.getClass()) == false) {
            return false;
        }

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if (((a[i] == null || b[j] == null) ? a == b : a[i].equalsIgnoreCase(b[j])) == false) {
                return false;
            }
        }

        return true;
    }

    /**
     * Method hashCode.
     *
     * @param value
     * @return int
     */
    public static int hashCode(final boolean value) {
        return value ? 1231 : 1237;
    }

    /**
     * Method hashCode.
     *
     * @param value
     * @return int
     */
    public static int hashCode(final char value) {
        return value;
    }

    /**
     * Method hashCode.
     *
     * @param value
     * @return int
     */
    public static int hashCode(final byte value) {
        return value;
    }

    /**
     * Method hashCode.
     *
     * @param value
     * @return int
     */
    public static int hashCode(final short value) {
        return value;
    }

    /**
     * Method hashCode.
     *
     * @param vaue
     * @return int
     */
    public static int hashCode(final int value) {
        return value;
    }

    /**
     * Method hashCode.
     *
     * @param value
     * @return int
     */
    public static int hashCode(final long value) {
        return (int) (value ^ (value >>> 32));
    }

    /**
     * Method hashCode.
     *
     * @param value
     * @return int
     */
    public static int hashCode(final float value) {
        return Float.floatToIntBits(value);
    }

    /**
     * Method hashCode.
     *
     * @param value
     * @return int
     */
    public static int hashCode(final double value) {
        long bits = Double.doubleToLongBits(value);

        return (int) (bits ^ (bits >>> 32));
    }

    /**
     * Method hashCode.
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
     * Method deepHashCode.
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
     * @see Arrays#hashCode(boolean[])
     * @param a
     * @return
     */
    public static int hashCode(final boolean[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

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
     * @see Arrays#hashCode(char[])
     * @param a
     * @return
     */
    public static int hashCode(final char[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

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
     * @see Arrays#hashCode(byte[])
     * @param a
     * @return
     */
    public static int hashCode(final byte[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

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
     * @see Arrays#hashCode(short[])
     * @param a
     * @return
     */
    public static int hashCode(final short[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

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
     * @see Arrays#hashCode(int[])
     * @param a
     * @return
     */
    public static int hashCode(final int[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

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
     * @see Arrays#hashCode(long[])
     * @param a
     * @return
     */
    public static int hashCode(final long[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

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
     * @see Arrays#hashCode(float[])
     * @param a
     * @return
     */
    public static int hashCode(final float[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

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
     * @see Arrays#hashCode(double[])
     * @param a
     * @return
     */
    public static int hashCode(final double[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

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
     * @see Arrays#hashCode(Object[])
     * @param a
     * @return
     */
    public static int hashCode(final Object[] a) {
        return a == null ? 0 : hashCode(a, 0, a.length);
    }

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
     * @see Arrays#deepHashCode(Object[])
     * @param a
     * @return
     */
    public static int deepHashCode(final Object[] a) {
        return a == null ? 0 : deepHashCode(a, 0, a.length);
    }

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
     * Method toString.
     *
     * @param value
     * @return int
     */
    public static String toString(final boolean value) {
        return stringOf(value);
    }

    /**
     * Method toString.
     *
     * @param value
     * @return int
     */
    public static String toString(final char value) {
        return stringOf(value);
    }

    /**
     * Method toString.
     *
     * @param value
     * @return int
     */
    public static String toString(final byte value) {
        return stringOf(value);
    }

    /**
     * Method toString.
     *
     * @param value
     * @return int
     */
    public static String toString(final short value) {
        return stringOf(value);
    }

    /**
     * Method toString.
     *
     * @param vaue
     * @return int
     */
    public static String toString(final int value) {
        return stringOf(value);
    }

    /**
     * Method toString.
     *
     * @param value
     * @return int
     */
    public static String toString(final long value) {
        return stringOf(value);
    }

    /**
     * Method toString.
     *
     * @param value
     * @return int
     */
    public static String toString(final float value) {
        return stringOf(value);
    }

    /**
     * Method toString.
     *
     * @param value
     * @return int
     */
    public static String toString(final double value) {
        return stringOf(value);
    }

    /**
     * Method toString.
     *
     * @param obj
     * @return int
     */
    public static String toString(final Object obj) {
        if (obj == null) {
            return NULL_STRING;
        } else if (obj instanceof CharSequence) {
            return obj.toString();
        }

        if (obj.getClass().isArray()) {
            return typeOf(obj.getClass()).toString(obj);
        } else if (obj instanceof Iterator) {
            final Iterator<?> iter = (Iterator<?>) obj;
            final Joiner joiner = Joiner.with(", ", "[", "]").reuseCachedBuffer(true);

            while (iter.hasNext()) {
                joiner.append(N.toString(iter.next()));
            }

            return joiner.toString();
        }

        final Integer typeIdx = CLASS_TYPE_ENUM.get(obj.getClass());

        if (typeIdx == null) {
            return obj.toString();
        }

        switch (typeIdx.intValue()) {
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

            case 27:
                return toString(((Float) obj).floatValue());

            case 28:
                return toString(((Double) obj).doubleValue());

            default:
                return obj.toString();
        }
    }

    /**
     * Method deepToString.
     *
     * @param obj
     * @return int
     */
    public static String deepToString(final Object obj) {
        if (obj == null) {
            return NULL_STRING;
        }

        if (obj.getClass().isArray()) {
            return typeOf(obj.getClass()).deepToString(obj);
        }

        return obj.toString();
    }

    /**
     * @see Arrays#toString(boolean[])
     * @param a
     * @return
     */
    public static String toString(final boolean[] a) {
        if (a == null) {
            return N.NULL_STRING;
        }

        if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    public static String toString(final boolean[] a, final int from, final int to) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, from, to);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    static void toString(final StringBuilder sb, final boolean[] a) {
        if (a == null) {
            sb.append(NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    static void toString(final StringBuilder sb, final boolean[] a, final int from, final int to) {
        sb.append(WD._BRACKET_L);

        for (int i = from; i < to; i++) {
            if (i > from) {
                sb.append(WD.COMMA_SPACE);
            }

            sb.append(a[i]);
        }

        sb.append(WD._BRACKET_R);
    }

    /**
     * @see Arrays#toString(char[])
     * @param a
     * @return
     */
    public static String toString(final char[] a) {
        if (a == null) {
            return N.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    public static String toString(final char[] a, final int from, final int to) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, from, to);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    static void toString(final StringBuilder sb, final char[] a) {
        if (a == null) {
            sb.append(NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    static void toString(final StringBuilder sb, final char[] a, final int from, final int to) {
        sb.append(WD._BRACKET_L);

        for (int i = from; i < to; i++) {
            if (i > from) {
                sb.append(WD.COMMA_SPACE);
            }

            sb.append(a[i]);
        }

        sb.append(WD._BRACKET_R);
    }

    /**
     * @see Arrays#toString(byte[])
     * @param a
     * @return
     */
    public static String toString(final byte[] a) {
        if (a == null) {
            return N.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    public static String toString(final byte[] a, final int from, final int to) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, from, to);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    static void toString(final StringBuilder sb, final byte[] a) {
        if (a == null) {
            sb.append(NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    static void toString(final StringBuilder sb, final byte[] a, final int from, final int to) {
        sb.append(WD._BRACKET_L);

        for (int i = from; i < to; i++) {
            if (i > from) {
                sb.append(WD.COMMA_SPACE);
            }

            sb.append(a[i]);
        }

        sb.append(WD._BRACKET_R);
    }

    /**
     * @see Arrays#toString(short[])
     * @param a
     * @return
     */
    public static String toString(final short[] a) {
        if (a == null) {
            return N.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    public static String toString(final short[] a, final int from, final int to) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, from, to);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    static void toString(final StringBuilder sb, final short[] a) {
        if (a == null) {
            sb.append(NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    static void toString(final StringBuilder sb, final short[] a, final int from, final int to) {
        sb.append(WD._BRACKET_L);

        for (int i = from; i < to; i++) {
            if (i > from) {
                sb.append(WD.COMMA_SPACE);
            }

            sb.append(a[i]);
        }

        sb.append(WD._BRACKET_R);
    }

    /**
     * @see Arrays#toString(int[])
     * @param a
     * @return
     */
    public static String toString(final int[] a) {
        if (a == null) {
            return N.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    public static String toString(final int[] a, final int from, final int to) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, from, to);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    static void toString(final StringBuilder sb, final int[] a) {
        if (a == null) {
            sb.append(NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    static void toString(final StringBuilder sb, final int[] a, final int from, final int to) {
        sb.append(WD._BRACKET_L);

        for (int i = from; i < to; i++) {
            if (i > from) {
                sb.append(WD.COMMA_SPACE);
            }

            sb.append(a[i]);
        }

        sb.append(WD._BRACKET_R);
    }

    /**
     * @see Arrays#toString(long[])
     * @param a
     * @return
     */
    public static String toString(final long[] a) {
        if (a == null) {
            return N.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    public static String toString(final long[] a, final int from, final int to) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, from, to);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    static void toString(final StringBuilder sb, final long[] a) {
        if (a == null) {
            sb.append(NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    static void toString(final StringBuilder sb, final long[] a, final int from, final int to) {
        sb.append(WD._BRACKET_L);

        for (int i = from; i < to; i++) {
            if (i > from) {
                sb.append(WD.COMMA_SPACE);
            }

            sb.append(a[i]);
        }

        sb.append(WD._BRACKET_R);
    }

    /**
     * @see Arrays#toString(float[])
     * @param a
     * @return
     */
    public static String toString(final float[] a) {
        if (a == null) {
            return N.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    public static String toString(final float[] a, final int from, final int to) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, from, to);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    static void toString(final StringBuilder sb, final float[] a) {
        if (a == null) {
            sb.append(NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    static void toString(final StringBuilder sb, final float[] a, final int from, final int to) {
        sb.append(WD._BRACKET_L);

        for (int i = from; i < to; i++) {
            if (i > from) {
                sb.append(WD.COMMA_SPACE);
            }

            sb.append(a[i]);
        }

        sb.append(WD._BRACKET_R);
    }

    /**
     * @see Arrays#toString(double[])
     * @param a
     * @return
     */
    public static String toString(final double[] a) {
        if (a == null) {
            return N.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    public static String toString(final double[] a, final int from, final int to) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, from, to);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    static void toString(final StringBuilder sb, final double[] a) {
        if (a == null) {
            sb.append(NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    static void toString(final StringBuilder sb, final double[] a, final int from, final int to) {
        sb.append(WD._BRACKET_L);

        for (int i = from; i < to; i++) {
            if (i > from) {
                sb.append(WD.COMMA_SPACE);
            }

            sb.append(a[i]);
        }

        sb.append(WD._BRACKET_R);
    }

    /**
     * @see Arrays#toString(Object[])
     * @param a
     * @return
     */
    public static String toString(final Object[] a) {
        if (a == null) {
            return N.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    public static String toString(final Object[] a, final int from, final int to) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, from, to);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    static void toString(final StringBuilder sb, final Object[] a) {
        if (a == null) {
            sb.append(NULL_STRING);
        } else if (a.length == 0) {
            sb.append("[]");
        } else {
            toString(sb, a, 0, a.length);
        }
    }

    static void toString(final StringBuilder sb, final Object[] a, final int from, final int to) {
        sb.append(WD._BRACKET_L);

        for (int i = from; i < to; i++) {
            if (i > from) {
                sb.append(WD.COMMA_SPACE);
            }

            sb.append(toString(a[i]));
        }

        sb.append(WD._BRACKET_R);
    }

    public static String toString(final Object a, final String defaultIfNull) {
        return a == null ? defaultIfNull : toString(a);
    }

    /**
     * @see Arrays#deepToString(Object[])
     * @param a
     * @return
     */
    public static String deepToString(final Object[] a) {
        if (a == null) {
            return N.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return deepToString(a, 0, a.length);
    }

    public static String deepToString(final Object[] a, final int from, final int to) {
        final StringBuilder sb = Objectory.createStringBuilder();
        final Set<Object[]> set = Objectory.createSet();

        try {
            deepToString(sb, a, from, to, set);

            return sb.toString();
        } finally {
            Objectory.recycle(set);
            Objectory.recycle(sb);
        }
    }

    static void deepToString(final StringBuilder sb, final Object[] a, final Set<Object[]> processedElements) {
        deepToString(sb, a, 0, a.length, processedElements);
    }

    static void deepToString(final StringBuilder sb, final Object[] a, final int from, final int to, final Set<Object[]> processedElements) {
        processedElements.add(a);

        sb.append(WD._BRACKET_L);

        Object element = null;
        Class<?> eClass = null;
        for (int i = from; i < to; i++) {
            element = a[i];

            if (i > from) {
                sb.append(WD.COMMA_SPACE);
            }

            if (element == null) {
                sb.append(N.NULL_CHAR_ARRAY);

                continue;
            }

            eClass = element.getClass();

            if (eClass.isArray()) {
                Integer enumInt = CLASS_TYPE_ENUM.get(eClass);

                int num = enumInt == null ? 0 : enumInt.intValue();

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
        if (N.isNullOrEmpty(a)) {
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
     */
    public static void reverse(final boolean[] a, int fromIndex, int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (N.isNullOrEmpty(a) || a.length == 1) {
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
        if (N.isNullOrEmpty(a)) {
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
     */
    public static void reverse(final char[] a, int fromIndex, int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (N.isNullOrEmpty(a) || a.length == 1) {
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
        if (N.isNullOrEmpty(a)) {
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
     */
    public static void reverse(final byte[] a, int fromIndex, int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (N.isNullOrEmpty(a) || a.length == 1) {
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
        if (N.isNullOrEmpty(a)) {
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
     */
    public static void reverse(final short[] a, int fromIndex, int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (N.isNullOrEmpty(a) || a.length == 1) {
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
        if (N.isNullOrEmpty(a)) {
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
     */
    public static void reverse(final int[] a, int fromIndex, int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (N.isNullOrEmpty(a) || a.length == 1) {
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
        if (N.isNullOrEmpty(a)) {
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
     */
    public static void reverse(final long[] a, int fromIndex, int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (N.isNullOrEmpty(a) || a.length == 1) {
            return;
        }

        long tmp = 0L;

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
        if (N.isNullOrEmpty(a)) {
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
     */
    public static void reverse(final float[] a, int fromIndex, int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (N.isNullOrEmpty(a) || a.length == 1) {
            return;
        }

        float tmp = 0f;

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
        if (N.isNullOrEmpty(a)) {
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
     */
    public static void reverse(final double[] a, int fromIndex, int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (N.isNullOrEmpty(a) || a.length == 1) {
            return;
        }

        double tmp = 0d;

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
        if (N.isNullOrEmpty(a)) {
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
     */
    public static void reverse(final Object[] a, int fromIndex, int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (N.isNullOrEmpty(a) || a.length == 1) {
            return;
        }

        Object tmp = null;

        for (int i = fromIndex, j = toIndex - 1; i < j; i++, j--) {
            tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
    }

    public static void reverse(final List<?> list) {
        if (N.isNullOrEmpty(list)) {
            return;
        }

        reverse(list, 0, list.size());
    }

    public static void reverse(final List<?> list, int fromIndex, int toIndex) {
        checkFromToIndex(fromIndex, toIndex, size(list));

        if (N.isNullOrEmpty(list) || list.size() == 1) {
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

    @SuppressWarnings("rawtypes")
    public static void reverse(final Collection<?> c) {
        if (N.isNullOrEmpty(c) || c.size() < 2) {
            return;
        }

        if (c instanceof List) {
            N.reverse((List) c);
        } else {
            final Object[] tmp = c.toArray();
            N.reverse(tmp);
            c.clear();
            c.addAll((List) Arrays.asList(tmp));
        }
    }

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
     * @see java.util.Collections#rotate(List, int)
     */
    public static void rotate(final List<?> list, final int distance) {
        if (list == null || list.size() <= 1 || distance % list.size() == 0) {
            return;
        }

        Collections.rotate(list, distance);
    }

    @SuppressWarnings("rawtypes")
    public static void rotate(final Collection<?> c, final int distance) {
        if (N.isNullOrEmpty(c) || c.size() < 2) {
            return;
        }

        if (c instanceof List) {
            N.rotate((List) c, distance);
        } else {
            final Object[] tmp = c.toArray();
            N.rotate(tmp, distance);
            c.clear();
            c.addAll((List) Arrays.asList(tmp));
        }
    }

    public static void shuffle(final boolean[] a) {
        shuffle(a, RAND);
    }

    public static void shuffle(final boolean[] a, final Random rnd) {
        if (N.isNullOrEmpty(a) || a.length == 1) {
            return;
        }

        for (int i = a.length; i > 1; i--) {
            swap(a, i - 1, rnd.nextInt(i));
        }
    }

    public static void shuffle(final char[] a) {
        shuffle(a, RAND);
    }

    public static void shuffle(final char[] a, final Random rnd) {
        if (N.isNullOrEmpty(a) || a.length == 1) {
            return;
        }

        for (int i = a.length; i > 1; i--) {
            swap(a, i - 1, rnd.nextInt(i));
        }
    }

    public static void shuffle(final byte[] a) {
        shuffle(a, RAND);
    }

    public static void shuffle(final byte[] a, final Random rnd) {
        if (N.isNullOrEmpty(a) || a.length == 1) {
            return;
        }

        for (int i = a.length; i > 1; i--) {
            swap(a, i - 1, rnd.nextInt(i));
        }
    }

    public static void shuffle(final short[] a) {
        shuffle(a, RAND);
    }

    public static void shuffle(final short[] a, final Random rnd) {
        if (N.isNullOrEmpty(a) || a.length == 1) {
            return;
        }

        for (int i = a.length; i > 1; i--) {
            swap(a, i - 1, rnd.nextInt(i));
        }
    }

    public static void shuffle(final int[] a) {
        shuffle(a, RAND);
    }

    public static void shuffle(final int[] a, final Random rnd) {
        if (N.isNullOrEmpty(a) || a.length == 1) {
            return;
        }

        for (int i = a.length; i > 1; i--) {
            swap(a, i - 1, rnd.nextInt(i));
        }
    }

    public static void shuffle(final long[] a) {
        shuffle(a, RAND);
    }

    public static void shuffle(final long[] a, final Random rnd) {
        if (N.isNullOrEmpty(a) || a.length == 1) {
            return;
        }

        for (int i = a.length; i > 1; i--) {
            swap(a, i - 1, rnd.nextInt(i));
        }
    }

    public static void shuffle(final float[] a) {
        shuffle(a, RAND);
    }

    public static void shuffle(final float[] a, final Random rnd) {
        if (N.isNullOrEmpty(a) || a.length == 1) {
            return;
        }

        for (int i = a.length; i > 1; i--) {
            swap(a, i - 1, rnd.nextInt(i));
        }
    }

    public static void shuffle(final double[] a) {
        shuffle(a, RAND);
    }

    public static void shuffle(final double[] a, final Random rnd) {
        if (N.isNullOrEmpty(a) || a.length == 1) {
            return;
        }

        for (int i = a.length; i > 1; i--) {
            swap(a, i - 1, rnd.nextInt(i));
        }
    }

    public static <T> void shuffle(final T[] a) {
        shuffle(a, RAND);
    }

    public static <T> void shuffle(final T[] a, final Random rnd) {
        if (N.isNullOrEmpty(a) || a.length == 1) {
            return;
        }

        for (int i = a.length; i > 1; i--) {
            swap(a, i - 1, rnd.nextInt(i));
        }
    }

    public static void shuffle(final List<?> list) {
        shuffle(list, RAND);
    }

    /**
     *
     * @see java.util.Collections#shuffle(List, Random)
     */
    public static void shuffle(final List<?> list, final Random rnd) {
        if (N.isNullOrEmpty(list) || list.size() == 1) {
            return;
        }

        Collections.shuffle(list, rnd);
    }

    @SuppressWarnings("rawtypes")
    public static void shuffle(final Collection<?> c) {
        if (N.isNullOrEmpty(c) || c.size() < 2) {
            return;
        }

        if (c instanceof List) {
            N.shuffle((List) c);
        } else {
            final Object[] tmp = c.toArray();
            N.shuffle(tmp);
            c.clear();
            c.addAll((List) Arrays.asList(tmp));
        }
    }

    @SuppressWarnings("rawtypes")
    public static void shuffle(final Collection<?> c, final Random rnd) {
        if (N.isNullOrEmpty(c) || c.size() < 2) {
            return;
        }

        if (c instanceof List) {
            N.shuffle((List) c, rnd);
        } else {
            final Object[] tmp = c.toArray();
            N.shuffle(tmp, rnd);
            c.clear();
            c.addAll((List) Arrays.asList(tmp));
        }
    }

    public static void swap(final boolean[] a, final int i, final int j) {
        final boolean tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    public static void swap(final char[] a, final int i, final int j) {
        final char tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    public static void swap(final byte[] a, final int i, final int j) {
        final byte tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    public static void swap(final short[] a, final int i, final int j) {
        final short tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    public static void swap(final int[] a, final int i, final int j) {
        final int tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    public static void swap(final long[] a, final int i, final int j) {
        final long tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    public static void swap(final float[] a, final int i, final int j) {
        final float tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    public static void swap(final double[] a, final int i, final int j) {
        final double tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    public static void swap(final Object[] a, final int i, final int j) {
        final Object tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    public static void swap(final List<?> list, final int i, final int j) {
        Collections.swap(list, i, j);
    }

    /**
     *
     * @param pair
     * @throws NullPointerExceptoin if the specified {@code pair} is {@code null}.
     */
    public static <T> void swap(final Pair<T, T> pair) {
        pair.set(pair.right, pair.left);
    }

    /**
     *
     * @param pair
     * @param predicate
     * @return
     * @throws NullPointerExceptoin if the specified {@code pair} or {@code predicate} is {@code null}.
     * @throws E
     */
    public static <T, E extends Exception> boolean swapIf(final Pair<T, T> pair, Try.Predicate<? super Pair<T, T>, E> predicate) throws E {
        if (predicate.test(pair)) {
            pair.set(pair.right, pair.left);
            return true;
        }

        return false;
    }

    /**
     *
     * @param triple
     * @throws NullPointerExceptoin if the specified {@code pair} is {@code null}.
     */
    public static <T, M> void swap(final Triple<T, M, T> triple) {
        final T left = triple.left;
        triple.setLeft(triple.right);
        triple.setRight(left);
    }

    /**
     *
     * @param triple
     * @param predicate
     * @return
     * @throws NullPointerExceptoin if the specified {@code triple} or {@code predicate} is {@code null}.
     * @throws E
     */
    public static <T, M, E extends Exception> boolean swapIf(final Triple<T, M, T> triple, Try.Predicate<? super Triple<T, M, T>, E> predicate) throws E {
        if (predicate.test(triple)) {
            final T left = triple.left;
            triple.setLeft(triple.right);
            triple.setRight(left);
            return true;
        }

        return false;
    }

    public static void fill(final boolean[] a, final boolean val) {
        Arrays.fill(a, val);
    }

    public static void fill(final boolean[] a, final int fromIndex, final int toIndex, final boolean val) {
        Arrays.fill(a, fromIndex, toIndex, val);
    }

    public static void fill(final char[] a, final char val) {
        Arrays.fill(a, val);
    }

    public static void fill(final char[] a, final int fromIndex, final int toIndex, final char val) {
        Arrays.fill(a, fromIndex, toIndex, val);
    }

    public static void fill(final byte[] a, final byte val) {
        Arrays.fill(a, val);
    }

    public static void fill(final byte[] a, final int fromIndex, final int toIndex, final byte val) {
        Arrays.fill(a, fromIndex, toIndex, val);
    }

    public static void fill(final short[] a, final short val) {
        Arrays.fill(a, val);
    }

    public static void fill(final short[] a, final int fromIndex, final int toIndex, final short val) {
        Arrays.fill(a, fromIndex, toIndex, val);
    }

    public static void fill(final int[] a, final int val) {
        Arrays.fill(a, val);
    }

    public static void fill(final int[] a, final int fromIndex, final int toIndex, final int val) {
        Arrays.fill(a, fromIndex, toIndex, val);
    }

    public static void fill(final long[] a, final long val) {
        Arrays.fill(a, val);
    }

    public static void fill(final long[] a, final int fromIndex, final int toIndex, final long val) {
        Arrays.fill(a, fromIndex, toIndex, val);
    }

    public static void fill(final float[] a, final float val) {
        Arrays.fill(a, val);
    }

    public static void fill(final float[] a, final int fromIndex, final int toIndex, final float val) {
        Arrays.fill(a, fromIndex, toIndex, val);
    }

    public static void fill(final double[] a, final double val) {
        Arrays.fill(a, val);
    }

    public static void fill(final double[] a, final int fromIndex, final int toIndex, final double val) {
        Arrays.fill(a, fromIndex, toIndex, val);
    }

    public static void fill(final Object[] a, final Object val) {
        Arrays.fill(a, val);
    }

    public static void fill(final Object[] a, final int fromIndex, final int toIndex, final Object val) {
        Arrays.fill(a, fromIndex, toIndex, val);
    }

    public static <T> void fill(final List<? super T> list, final T val) {
        fill(list, 0, list.size(), val);
    }

    /**
     * The specified value will be added/inserted into the specified List.
     * The List will be extended automatically if the size of the List is less than the specified toIndex.
     * @param list
     * @param fromIndex
     * @param toIndex
     * @param val
     */
    public static <T> void fill(final List<? super T> list, final int fromIndex, final int toIndex, final T val) {
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
     * Fill the properties of the entity with random values.
     *
     * @param entity an entity object with getter/setter method
     */
    public static void fill(Object entity) {
        TestUtil.fill(entity);
    }

    /**
     * Fill the properties of the entity with random values.
     *
     * @param entityClass entity class with getter/setter methods
     * @return
     */
    public static <T> T fill(Class<T> entityClass) {
        return TestUtil.fill(entityClass);
    }

    /**
     * Fill the properties of the entity with random values.
     *
     * @param entityClass entity class with getter/setter methods
     * @param count
     * @return
     */
    public static <T> List<T> fill(Class<T> entityClass, int count) {
        return TestUtil.fill(entityClass, count);
    }

    public static <T> List<T> repeat(final T value, final int n) {
        N.checkArgNotNegative(n, "n");

        final List<T> res = new ArrayList<>(n);
        fill(res, 0, n, value);
        return res;
    }

    /**
     * Repeats the elements in the specified Collection one by one.
     *
     * <pre>
     * <code>
     * Seq.nRepeat(N.asList(1, 2, 3), 2) => [1, 1, 2, 2, 3, 3]
     * </code>
     * </pre>
     *
     * @param c
     * @param n
     * @return
     */
    public static <T> List<T> repeatEach(final Collection<T> c, final int n) {
        N.checkArgNotNegative(n, "n");

        if (n == 0 || isNullOrEmpty(c)) {
            return new ArrayList<T>();
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
     * <pre>
     * <code>
     * Seq.repeat(N.asList(1, 2, 3), 2) => [1, 2, 3, 1, 2, 3]
     * </code>
     * </pre>
     * @param c
     * @param n
     * @return
     */
    public static <T> List<T> repeatAll(final Collection<T> c, final int n) {
        N.checkArgNotNegative(n, "n");

        if (n == 0 || isNullOrEmpty(c)) {
            return new ArrayList<T>();
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
     * Seq.nRepeatToSize(N.asList(1, 2, 3), 5) => [1, 1, 2, 2, 3]
     * </code>
     * </pre>
     *
     * @param c
     * @param size
     * @return
     */
    public static <T> List<T> repeatEachToSize(final Collection<T> c, final int size) {
        N.checkArgNotNegative(size, "size");
        checkArgument(size == 0 || notNullOrEmpty(c), "Collection can not be empty or null when size > 0");

        if (size == 0 || isNullOrEmpty(c)) {
            return new ArrayList<T>();
        }

        final int n = size / c.size();
        int mod = size % c.size();

        final List<T> result = new ArrayList<>(size);

        for (T e : c) {
            for (int i = 0, len = mod-- > 0 ? n + 1 : n; i < len; i++) {
                result.add(e);
            }

            if (result.size() == size) {
                break;
            }
        }

        return result;
    }

    /**
     *
     * <pre>
     * <code>
     * Seq.repeatToSize(N.asList(1, 2, 3), 5) => [1, 2, 3, 1, 2]
     * </code>
     * </pre>
     *
     * @param c
     * @param size
     * @return
     */
    public static <T> List<T> repeatAllToSize(final Collection<T> c, final int size) {
        N.checkArgNotNegative(size, "size");
        checkArgument(size == 0 || notNullOrEmpty(c), "Collection can not be empty or null when size > 0");

        if (size == 0 || isNullOrEmpty(c)) {
            return new ArrayList<T>();
        }

        final List<T> result = new ArrayList<>(size);

        while (result.size() < size) {
            if (c.size() <= size - result.size()) {
                result.addAll(c);
            } else {
                final Iterator<T> iter = c.iterator();

                for (int i = 0, len = size - result.size(); i < len; i++) {
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
     * @param  src The source list.
     * @param  dest The destination list.
     * @throws IndexOutOfBoundsException if the destination list is too small
     *         to contain the entire source List.
     * @throws UnsupportedOperationException if the destination list's
     *         list-iterator does not support the <tt>set</tt> operation.
     *
     * @see java.util.Collections#copy(List, List)
     */
    public static <T> void copy(final List<? extends T> src, final List<? super T> dest) {
        if (src.size() > dest.size()) {
            throw new IllegalArgumentException("Source does not fit in dest");
        }

        Collections.copy(dest, src);
    }

    public static <T> void copy(final List<? extends T> src, final int srcPos, final List<? super T> dest, final int destPos, final int length) {
        if (src.size() < srcPos + length) {
            throw new IllegalArgumentException("The size of src list less than " + (srcPos + length));
        }

        if (dest.size() < destPos + length) {
            throw new IllegalArgumentException("The size of dest list less than " + (destPos + length));
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

    public static void copy(final boolean[] src, final int srcPos, final boolean[] dest, final int destPos, final int length) {
        if (src.length < srcPos + length) {
            throw new IllegalArgumentException("The size of src array less than " + (srcPos + length));
        }

        if (dest.length < destPos + length) {
            throw new IllegalArgumentException("The size of dest array less than " + (destPos + length));
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

    public static void copy(final char[] src, final int srcPos, final char[] dest, final int destPos, final int length) {
        if (src.length < srcPos + length) {
            throw new IllegalArgumentException("The size of src array less than " + (srcPos + length));
        }

        if (dest.length < destPos + length) {
            throw new IllegalArgumentException("The size of dest array less than " + (destPos + length));
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

    public static void copy(final byte[] src, final int srcPos, final byte[] dest, final int destPos, final int length) {
        if (src.length < srcPos + length) {
            throw new IllegalArgumentException("The size of src array less than " + (srcPos + length));
        }

        if (dest.length < destPos + length) {
            throw new IllegalArgumentException("The size of dest array less than " + (destPos + length));
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

    public static void copy(final short[] src, final int srcPos, final short[] dest, final int destPos, final int length) {
        if (src.length < srcPos + length) {
            throw new IllegalArgumentException("The size of src array less than " + (srcPos + length));
        }

        if (dest.length < destPos + length) {
            throw new IllegalArgumentException("The size of dest array less than " + (destPos + length));
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

    public static void copy(final int[] src, final int srcPos, final int[] dest, final int destPos, final int length) {
        if (src.length < srcPos + length) {
            throw new IllegalArgumentException("The size of src array less than " + (srcPos + length));
        }

        if (dest.length < destPos + length) {
            throw new IllegalArgumentException("The size of dest array less than " + (destPos + length));
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

    public static void copy(final long[] src, final int srcPos, final long[] dest, final int destPos, final int length) {
        if (src.length < srcPos + length) {
            throw new IllegalArgumentException("The size of src array less than " + (srcPos + length));
        }

        if (dest.length < destPos + length) {
            throw new IllegalArgumentException("The size of dest array less than " + (destPos + length));
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

    public static void copy(final float[] src, final int srcPos, final float[] dest, final int destPos, final int length) {
        if (src.length < srcPos + length) {
            throw new IllegalArgumentException("The size of src array less than " + (srcPos + length));
        }

        if (dest.length < destPos + length) {
            throw new IllegalArgumentException("The size of dest array less than " + (destPos + length));
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

    public static void copy(final double[] src, final int srcPos, final double[] dest, final int destPos, final int length) {
        if (src.length < srcPos + length) {
            throw new IllegalArgumentException("The size of src array less than " + (srcPos + length));
        }

        if (dest.length < destPos + length) {
            throw new IllegalArgumentException("The size of dest array less than " + (destPos + length));
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

    public static void copy(final Object[] src, final int srcPos, final Object[] dest, final int destPos, final int length) {
        if (src.length < srcPos + length) {
            throw new IllegalArgumentException("The size of src array less than " + (srcPos + length));
        }

        if (dest.length < destPos + length) {
            throw new IllegalArgumentException("The size of dest array less than " + (destPos + length));
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
     * @see System#arraycopy(Object, int, Object, int, int) is called
     *
     * @param src
     * @param srcPos
     * @param dest
     * @param destPos
     * @param length
     */
    public static void copy(final Object src, final int srcPos, final Object dest, final int destPos, final int length) {
        if (Array.getLength(src) < srcPos + length) {
            throw new IllegalArgumentException("The size of src array less than " + (srcPos + length));
        }

        if (Array.getLength(dest) < destPos + length) {
            throw new IllegalArgumentException("The size of dest array less than " + (destPos + length));
        }

        System.arraycopy(src, srcPos, dest, destPos, length);
    }

    /**
     * @see Arrays#copyOf(boolean[], int)
     *
     * @param original
     * @param newLength
     * @return
     */
    public static boolean[] copyOf(final boolean[] original, final int newLength) {
        if (newLength == original.length) {
            return original.clone();
        }

        final boolean[] copy = new boolean[newLength];

        if (N.notNullOrEmpty(original)) {
            copy(original, 0, copy, 0, Math.min(original.length, newLength));
        }

        return copy;
    }

    /**
     * @see Arrays#copyOf(char[], int)
     *
     * @param original
     * @param newLength
     * @return
     */
    public static char[] copyOf(final char[] original, final int newLength) {
        if (newLength == original.length) {
            return original.clone();
        }

        final char[] copy = new char[newLength];

        if (N.notNullOrEmpty(original)) {
            copy(original, 0, copy, 0, Math.min(original.length, newLength));
        }

        return copy;
    }

    /**
     * @see Arrays#copyOf(byte[], int)
     *
     * @param original
     * @param newLength
     * @return
     */
    public static byte[] copyOf(final byte[] original, final int newLength) {
        if (newLength == original.length) {
            return original.clone();
        }

        final byte[] copy = new byte[newLength];

        if (N.notNullOrEmpty(original)) {
            copy(original, 0, copy, 0, Math.min(original.length, newLength));
        }

        return copy;
    }

    /**
     * @see Arrays#copyOf(short[], int)
     *
     * @param original
     * @param newLength
     * @return
     */
    public static short[] copyOf(final short[] original, final int newLength) {
        if (newLength == original.length) {
            return original.clone();
        }

        final short[] copy = new short[newLength];

        if (N.notNullOrEmpty(original)) {
            copy(original, 0, copy, 0, Math.min(original.length, newLength));
        }

        return copy;
    }

    /**
     * @see Arrays#copyOf(int[], int)
     *
     * @param original
     * @param newLength
     * @return
     */
    public static int[] copyOf(final int[] original, final int newLength) {
        if (newLength == original.length) {
            return original.clone();
        }

        final int[] copy = new int[newLength];

        if (N.notNullOrEmpty(original)) {
            copy(original, 0, copy, 0, Math.min(original.length, newLength));
        }

        return copy;
    }

    /**
     * @see Arrays#copyOf(long[], int)
     *
     * @param original
     * @param newLength
     * @return
     */
    public static long[] copyOf(final long[] original, final int newLength) {
        if (newLength == original.length) {
            return original.clone();
        }

        final long[] copy = new long[newLength];

        if (N.notNullOrEmpty(original)) {
            copy(original, 0, copy, 0, Math.min(original.length, newLength));
        }

        return copy;
    }

    /**
     * @see Arrays#copyOf(float[], int)
     *
     * @param original
     * @param newLength
     * @return
     */
    public static float[] copyOf(final float[] original, final int newLength) {
        if (newLength == original.length) {
            return original.clone();
        }

        final float[] copy = new float[newLength];

        if (N.notNullOrEmpty(original)) {
            copy(original, 0, copy, 0, Math.min(original.length, newLength));
        }

        return copy;
    }

    /**
     * @see Arrays#copyOf(double[], int)
     *
     * @param original
     * @param newLength
     * @return
     */
    public static double[] copyOf(final double[] original, final int newLength) {
        if (newLength == original.length) {
            return original.clone();
        }

        final double[] copy = new double[newLength];

        if (N.notNullOrEmpty(original)) {
            copy(original, 0, copy, 0, Math.min(original.length, newLength));
        }

        return copy;
    }

    /**
     * @see Arrays#copyOf(Object[], int)
     *
     * @param original
     * @param newLength
     * @return
     */
    public static <T> T[] copyOf(final T[] original, final int newLength) {
        if (newLength == original.length) {
            return original.clone();
        }

        return (T[]) copyOf(original, newLength, original.getClass());
    }

    /**
     * @see Arrays#copyOf(Object[], int, Class)
     *
     * @param original
     * @param newLength
     * @return
     */
    public static <T, U> T[] copyOf(final U[] original, final int newLength, final Class<? extends T[]> newType) {
        final T[] copy = Object[].class.equals(newType) ? (T[]) new Object[newLength] : (T[]) N.newArray(newType.getComponentType(), newLength);

        if (N.notNullOrEmpty(original)) {
            copy(original, 0, copy, 0, Math.min(original.length, newLength));
        }

        return copy;
    }

    /**
     * @see Arrays#copyOfRange(boolean[], int, int)
     *
     * @param original
     * @param from
     * @param to
     * @return
     */
    public static boolean[] copyOfRange(final boolean[] original, final int from, final int to) {
        if (from == 0 && to == original.length) {
            return original.clone();
        }

        final int newLength = to - from;
        final boolean[] copy = new boolean[newLength];
        copy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }

    /**
     * Copy all the elements in <code>original</code>, through <code>to</code>-<code>from</code>, by <code>step</code>.
     *
     * @param original
     * @param from
     * @param to
     * @param step
     * @return
     * @see N#copyOfRange(int[], int, int, int)
     */
    public static boolean[] copyOfRange(final boolean[] original, int from, final int to, final int step) {
        N.checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (from == to || from < to != step > 0) {
            return N.EMPTY_BOOLEAN_ARRAY;
        }

        if (step == 1) {
            return copyOfRange(original, from, to);
        }

        from = from > to ? N.min(original.length - 1, from) : from;
        final int len = (to - from) / step + ((to - from) % step == 0 ? 0 : 1);
        final boolean[] copy = new boolean[len];

        for (int i = 0, j = from; i < len; i++, j += step) {
            copy[i] = original[j];
        }

        return copy;
    }

    /**
     * @see Arrays#copyOfRange(char[], int, int)
     *
     * @param original
     * @param from
     * @param to
     * @return
     */
    public static char[] copyOfRange(final char[] original, final int from, final int to) {
        if (from == 0 && to == original.length) {
            return original.clone();
        }

        final int newLength = to - from;
        final char[] copy = new char[newLength];
        copy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }

    /**
     * Copy all the elements in <code>original</code>, through <code>to</code>-<code>from</code>, by <code>step</code>.
     *
     * @param original
     * @param from
     * @param to
     * @param step
     * @return
     * @see N#copyOfRange(int[], int, int, int)
     */
    public static char[] copyOfRange(final char[] original, int from, final int to, final int step) {
        N.checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (from == to || from < to != step > 0) {
            return N.EMPTY_CHAR_ARRAY;
        }

        if (step == 1) {
            return copyOfRange(original, from, to);
        }

        from = from > to ? N.min(original.length - 1, from) : from;
        final int len = (to - from) / step + ((to - from) % step == 0 ? 0 : 1);
        final char[] copy = new char[len];

        for (int i = 0, j = from; i < len; i++, j += step) {
            copy[i] = original[j];
        }

        return copy;
    }

    /**
     * @see Arrays#copyOfRange(byte[], int, int)
     *
     * @param original
     * @param from
     * @param to
     * @return
     */
    public static byte[] copyOfRange(final byte[] original, final int from, final int to) {
        if (from == 0 && to == original.length) {
            return original.clone();
        }

        final int newLength = to - from;
        final byte[] copy = new byte[newLength];
        copy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }

    /**
     * Copy all the elements in <code>original</code>, through <code>to</code>-<code>from</code>, by <code>step</code>.
     *
     * @param original
     * @param from
     * @param to
     * @param step
     * @return
     * @see N#copyOfRange(int[], int, int, int)
     */
    public static byte[] copyOfRange(final byte[] original, int from, final int to, final int step) {
        N.checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (from == to || from < to != step > 0) {
            return N.EMPTY_BYTE_ARRAY;
        }

        if (step == 1) {
            return copyOfRange(original, from, to);
        }

        from = from > to ? N.min(original.length - 1, from) : from;
        final int len = (to - from) / step + ((to - from) % step == 0 ? 0 : 1);
        final byte[] copy = new byte[len];

        for (int i = 0, j = from; i < len; i++, j += step) {
            copy[i] = original[j];
        }

        return copy;
    }

    /**
     * @see Arrays#copyOfRange(short[], int, int)
     *
     * @param original
     * @param from
     * @param to
     * @return
     */
    public static short[] copyOfRange(final short[] original, final int from, final int to) {
        if (from == 0 && to == original.length) {
            return original.clone();
        }

        final int newLength = to - from;
        final short[] copy = new short[newLength];
        copy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }

    /**
     * Copy all the elements in <code>original</code>, through <code>to</code>-<code>from</code>, by <code>step</code>.
     *
     * @param original
     * @param from
     * @param to
     * @param step
     * @return
     * @see N#copyOfRange(int[], int, int, int)
     */
    public static short[] copyOfRange(final short[] original, int from, final int to, final int step) {
        N.checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (from == to || from < to != step > 0) {
            return N.EMPTY_SHORT_ARRAY;
        }

        if (step == 1) {
            return copyOfRange(original, from, to);
        }

        from = from > to ? N.min(original.length - 1, from) : from;
        final int len = (to - from) / step + ((to - from) % step == 0 ? 0 : 1);
        final short[] copy = new short[len];

        for (int i = 0, j = from; i < len; i++, j += step) {
            copy[i] = original[j];
        }

        return copy;
    }

    /**
     * @see Arrays#copyOfRange(int[], int, int)
     *
     * @param original
     * @param from
     * @param to
     * @return
     */
    public static int[] copyOfRange(final int[] original, final int from, final int to) {
        if (from == 0 && to == original.length) {
            return original.clone();
        }

        final int newLength = to - from;
        final int[] copy = new int[newLength];
        copy(original, from, copy, 0, Math.min(original.length - from, newLength));
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
     * @param original
     * @param from
     * @param to
     * @param step
     * @return
     */
    public static int[] copyOfRange(final int[] original, int from, final int to, final int step) {
        N.checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (from == to || from < to != step > 0) {
            return N.EMPTY_INT_ARRAY;
        }

        if (step == 1) {
            return copyOfRange(original, from, to);
        }

        from = from > to ? N.min(original.length - 1, from) : from;
        final int len = (to - from) / step + ((to - from) % step == 0 ? 0 : 1);
        final int[] copy = new int[len];

        for (int i = 0, j = from; i < len; i++, j += step) {
            copy[i] = original[j];
        }

        return copy;
    }

    /**
     * @see Arrays#copyOfRange(long[], int, int)
     *
     * @param original
     * @param from
     * @param to
     * @return
     */
    public static long[] copyOfRange(final long[] original, final int from, final int to) {
        if (from == 0 && to == original.length) {
            return original.clone();
        }

        final int newLength = to - from;
        final long[] copy = new long[newLength];
        copy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }

    /**
     * Copy all the elements in <code>original</code>, through <code>to</code>-<code>from</code>, by <code>step</code>.
     *
     * @param original
     * @param from
     * @param to
     * @param step
     * @return
     * @see N#copyOfRange(int[], int, int, int)
     */
    public static long[] copyOfRange(final long[] original, int from, final int to, final int step) {
        N.checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (from == to || from < to != step > 0) {
            return N.EMPTY_LONG_ARRAY;
        }

        if (step == 1) {
            return copyOfRange(original, from, to);
        }

        from = from > to ? N.min(original.length - 1, from) : from;
        final int len = (to - from) / step + ((to - from) % step == 0 ? 0 : 1);
        final long[] copy = new long[len];

        for (int i = 0, j = from; i < len; i++, j += step) {
            copy[i] = original[j];
        }

        return copy;
    }

    /**
     * @see Arrays#copyOfRange(float[], int, int)
     *
     * @param original
     * @param from
     * @param to
     * @return
     */
    public static float[] copyOfRange(final float[] original, final int from, final int to) {
        if (from == 0 && to == original.length) {
            return original.clone();
        }

        final int newLength = to - from;
        final float[] copy = new float[newLength];
        copy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }

    /**
     * Copy all the elements in <code>original</code>, through <code>to</code>-<code>from</code>, by <code>step</code>.
     *
     * @param original
     * @param from
     * @param to
     * @param step
     * @return
     * @see N#copyOfRange(int[], int, int, int)
     */
    public static float[] copyOfRange(final float[] original, int from, final int to, final int step) {
        N.checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (from == to || from < to != step > 0) {
            return N.EMPTY_FLOAT_ARRAY;
        }

        if (step == 1) {
            return copyOfRange(original, from, to);
        }

        from = from > to ? N.min(original.length - 1, from) : from;
        final int len = (to - from) / step + ((to - from) % step == 0 ? 0 : 1);
        final float[] copy = new float[len];

        for (int i = 0, j = from; i < len; i++, j += step) {
            copy[i] = original[j];
        }

        return copy;
    }

    /**
     * @see Arrays#copyOfRange(double[], int, int)
     *
     * @param original
     * @param from
     * @param to
     * @return
     */
    public static double[] copyOfRange(final double[] original, final int from, final int to) {
        if (from == 0 && to == original.length) {
            return original.clone();
        }

        final int newLength = to - from;
        final double[] copy = new double[newLength];
        copy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }

    /**
     * Copy all the elements in <code>original</code>, through <code>to</code>-<code>from</code>, by <code>step</code>.
     *
     * @param original
     * @param from
     * @param to
     * @param step
     * @return
     * @see N#copyOfRange(int[], int, int, int)
     */
    public static double[] copyOfRange(final double[] original, int from, final int to, final int step) {
        N.checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (from == to || from < to != step > 0) {
            return N.EMPTY_DOUBLE_ARRAY;
        }

        if (step == 1) {
            return copyOfRange(original, from, to);
        }

        from = from > to ? N.min(original.length - 1, from) : from;
        final int len = (to - from) / step + ((to - from) % step == 0 ? 0 : 1);
        final double[] copy = new double[len];

        for (int i = 0, j = from; i < len; i++, j += step) {
            copy[i] = original[j];
        }

        return copy;
    }

    /**
     * @see Arrays#copyOfRange(T[], int, int)
     * @param original
     * @param from
     * @param to
     * @return
     */
    public static <T> T[] copyOfRange(final T[] original, final int from, final int to) {
        if (from == 0 && to == original.length) {
            return original.clone();
        }

        return copyOfRange(original, from, to, (Class<T[]>) original.getClass());
    }

    /**
     * Copy all the elements in <code>original</code>, through <code>to</code>-<code>from</code>, by <code>step</code>.
     *
     * @param original
     * @param from
     * @param to
     * @param step
     * @return
     */
    public static <T> T[] copyOfRange(final T[] original, final int from, final int to, final int step) {
        return copyOfRange(original, from, to, step, (Class<T[]>) original.getClass());
    }

    /**
     * {@link Arrays#copyOfRange(Object[], int, int, Class)}
     *
     * @param original
     * @param from
     * @param to
     * @param newType
     * @return
     */
    public static <T, U> T[] copyOfRange(final U[] original, final int from, final int to, final Class<? extends T[]> newType) {
        final int newLength = to - from;
        final T[] copy = Object[].class.equals(newType) ? (T[]) new Object[newLength] : (T[]) N.newArray(newType.getComponentType(), newLength);
        copy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }

    /**
     * Copy all the elements in <code>original</code>, through <code>to</code>-<code>from</code>, by <code>step</code>.
     *
     * @param original
     * @param from
     * @param to
     * @param step
     * @return
     * @see N#copyOfRange(int[], int, int, int)
     */
    public static <T> T[] copyOfRange(final T[] original, int from, final int to, final int step, final Class<? extends T[]> newType) {
        N.checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (from == to || from < to != step > 0) {
            return Object[].class.equals(newType) ? (T[]) new Object[0] : (T[]) N.newArray(newType.getComponentType(), 0);
        }

        if (step == 1) {
            return copyOfRange(original, from, to);
        }

        from = from > to ? N.min(original.length - 1, from) : from;
        final int len = (to - from) / step + ((to - from) % step == 0 ? 0 : 1);
        final T[] copy = Object[].class.equals(newType) ? (T[]) new Object[len] : (T[]) N.newArray(newType.getComponentType(), len);

        for (int i = 0, j = from; i < len; i++, j += step) {
            copy[i] = original[j];
        }

        return copy;
    }

    /**
     * @see Arrays#copyOfRange(T[], int, int)
     * @param c
     * @param from
     * @param to
     * @return
     */
    public static <T> List<T> copyOfRange(final List<T> c, final int from, final int to) {
        N.checkFromToIndex(from, to, c.size());

        final List<T> result = new ArrayList<>(to - from);
        result.addAll(c.subList(from, to));
        return result;
    }

    /**
     * Copy all the elements in <code>original</code>, through <code>to</code>-<code>from</code>, by <code>step</code>.
     *
     * @param c
     * @param from
     * @param to
     * @param step
     * @return
     */
    public static <T> List<T> copyOfRange(final List<T> c, int from, final int to, final int step) {
        N.checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from, c.size());

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (from == to || from < to != step > 0) {
            return new ArrayList<>();
        }

        if (step == 1) {
            return copyOfRange(c, from, to);
        }

        from = from > to ? N.min(c.size() - 1, from) : from;
        final int len = (to - from) / step + ((to - from) % step == 0 ? 0 : 1);
        List<T> result = null;

        if (c instanceof RandomAccess) {
            result = new ArrayList<>(len);

            for (int i = 0, j = from; i < len; i++, j += step) {
                result.add(c.get(j));
            }
        } else {
            final T[] a = (T[]) c.subList(from, to).toArray();
            result = createList(N.copyOfRange(a, 0, a.length, step));
        }

        return result;
    }

    /**
     * Create an array list by initializing its elements data with the specified array <code>a</code>.
     * The returned list may share the same elements with the specified array <code>a</code>.
     * That's to say any change on the List/Array will affect the Array/List.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    private static <T> List<T> createList(final T... a) {
        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        if (N.isListElementDataFieldSettable && N.listElementDataField != null && N.listSizeField != null) {
            final List<T> list = new ArrayList<>();

            try {
                N.listElementDataField.set(list, a);
                N.listSizeField.set(list, a.length);

                return list;
            } catch (Throwable e) {
                // ignore;
                N.isListElementDataFieldSettable = false;
            }
        }

        return N.asList(a);
    }

    /**
    *
    * @param str
    * @param from
    * @param to
    * @return
    */
    public static String copyOfRange(final String str, final int from, final int to) {
        return str.substring(from, to);
    }

    /**
    *
    * @param str
    * @param from
    * @param to
    * @param step
    * @return
    * @see N#copyOfRange(int[], int, int, int)
    */
    @SuppressWarnings("deprecation")
    public static String copyOfRange(final String str, int from, final int to, final int step) {
        N.checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from, str.length());

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (from == to || from < to != step > 0) {
            return N.EMPTY_STRING;
        }

        if (step == 1) {
            return copyOfRange(str, from, to);
        }

        return StringUtil.newString(copyOfRange(StringUtil.getCharsForReadOnly(str), from, to, step), true);
    }

    /**
     * Clone the original array. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return
     */
    public static boolean[] clone(final boolean[] original) {
        if (original == null) {
            return null;
        }

        return original.clone();
    }

    /**
     * Clone the original array. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return
     */
    public static char[] clone(final char[] original) {
        if (original == null) {
            return null;
        }

        return original.clone();
    }

    /**
     * Clone the original array. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return
     */
    public static byte[] clone(final byte[] original) {
        if (original == null) {
            return null;
        }

        return original.clone();
    }

    /**
     * Clone the original array. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return
     */
    public static short[] clone(final short[] original) {
        if (original == null) {
            return null;
        }

        return original.clone();
    }

    /**
     * Clone the original array. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return
     */
    public static int[] clone(final int[] original) {
        if (original == null) {
            return null;
        }

        return original.clone();
    }

    /**
     * Clone the original array. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return
     */
    public static long[] clone(final long[] original) {
        if (original == null) {
            return null;
        }

        return original.clone();
    }

    /**
     * Clone the original array. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return
     */
    public static float[] clone(final float[] original) {
        if (original == null) {
            return null;
        }

        return original.clone();
    }

    /**
     * Clone the original array. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return
     */
    public static double[] clone(final double[] original) {
        if (original == null) {
            return null;
        }

        return original.clone();
    }

    /**
     * Clone the original array. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return
     */
    public static <T> T[] clone(final T[] original) {
        if (original == null) {
            return null;
        }

        return original.clone();
    }

    /**
     * Clone the original array and its sub arrays. <code>null</code> is returned if the input array is <code>null</code>.
     *
     * @param original
     * @return
     */
    public static boolean[][] clone(final boolean[][] original) {
        if (original == null) {
            return null;
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
     * @return
     */
    public static char[][] clone(final char[][] original) {
        if (original == null) {
            return null;
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
     * @return
     */
    public static byte[][] clone(final byte[][] original) {
        if (original == null) {
            return null;
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
     * @return
     */
    public static short[][] clone(final short[][] original) {
        if (original == null) {
            return null;
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
     * @return
     */
    public static int[][] clone(final int[][] original) {
        if (original == null) {
            return null;
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
     * @return
     */
    public static long[][] clone(final long[][] original) {
        if (original == null) {
            return null;
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
     * @return
     */
    public static float[][] clone(final float[][] original) {
        if (original == null) {
            return null;
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
     * @return
     */
    public static double[][] clone(final double[][] original) {
        if (original == null) {
            return null;
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
     * @param original
     * @return
     */
    public static <T> T[][] clone(final T[][] original) {
        if (original == null) {
            return null;
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
     * @return
     */
    public static boolean[][][] clone(final boolean[][][] original) {
        if (original == null) {
            return null;
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
     * @return
     */
    public static char[][][] clone(final char[][][] original) {
        if (original == null) {
            return null;
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
     * @return
     */
    public static byte[][][] clone(final byte[][][] original) {
        if (original == null) {
            return null;
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
     * @return
     */
    public static short[][][] clone(final short[][][] original) {
        if (original == null) {
            return null;
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
     * @return
     */
    public static int[][][] clone(final int[][][] original) {
        if (original == null) {
            return null;
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
     * @return
     */
    public static long[][][] clone(final long[][][] original) {
        if (original == null) {
            return null;
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
     * @return
     */
    public static float[][][] clone(final float[][][] original) {
        if (original == null) {
            return null;
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
     * @return
     */
    public static double[][][] clone(final double[][][] original) {
        if (original == null) {
            return null;
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
     * @param original
     * @return
     */
    public static <T> T[][][] clone(final T[][][] original) {
        if (original == null) {
            return null;
        }

        final T[][][] cp = original.clone();

        for (int i = 0, len = cp.length; i < len; i++) {
            cp[i] = clone(original[i]);
        }

        return cp;
    }

    public static <T> T[] copy(Class<T[]> newType, Object[] a) {
        if (N.isNullOrEmpty(a)) {
            return N.newArray(newType.getComponentType(), 0);
        }

        return N.copyOf(a, a.length, newType);
    }

    public static <T> T[][] copy(Class<T[][]> newType, Object[][] a) {
        final Class<T[]> componentType = (Class<T[]>) newType.getComponentType();

        if (N.isNullOrEmpty(a)) {
            return N.newArray(componentType, 0);
        }

        final int len = N.len(a);
        final T[][] result = N.newArray(componentType, len);

        for (int i = 0; i < len; i++) {
            result[i] = copy(componentType, a[i]);
        }

        return result;
    }

    public static <T> T[][][] copy(Class<T[][][]> newType, Object[][][] a) {
        final Class<T[][]> componentType = (Class<T[][]>) newType.getComponentType();

        if (N.isNullOrEmpty(a)) {
            return N.newArray(componentType, 0);
        }

        final int len = N.len(a);
        final T[][][] result = N.newArray(componentType, len);

        for (int i = 0; i < len; i++) {
            result[i] = copy(componentType, a[i]);
        }

        return result;
    }

    public static void sort(final boolean[] a) {
        Array.sort(a);
    }

    public static void sort(final char[] a) {
        Array.sort(a);
    }

    public static void sort(final char[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
    }

    public static void sort(final byte[] a) {
        Array.sort(a);
    }

    public static void sort(final byte[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
    }

    public static void sort(final short[] a) {
        Array.sort(a);
    }

    public static void sort(final short[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
    }

    public static void sort(final int[] a) {
        Array.sort(a);
    }

    public static void sort(final int[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
    }

    public static void sort(final long[] a) {
        Array.sort(a);
    }

    public static void sort(final long[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
    }

    public static void sort(final float[] a) {
        Array.sort(a);
    }

    public static void sort(final float[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
    }

    public static void sort(final double[] a) {
        Array.sort(a);
    }

    public static void sort(final double[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
    }

    public static void sort(final Object[] a) {
        Array.sort(a);
    }

    public static void sort(final Object[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
    }

    public static <T> void sort(final T[] a, final Comparator<? super T> cmp) {
        Array.sort(a, cmp);
    }

    public static <T> void sort(final T[] a, final int fromIndex, final int toIndex, final Comparator<? super T> cmp) {
        Array.sort(a, fromIndex, toIndex, cmp);
    }

    public static <T extends Comparable<? super T>> void sort(final List<? extends T> c) {
        Array.sort(c);
    }

    public static <T extends Comparable<? super T>> void sort(final List<? extends T> c, final int fromIndex, final int toIndex) {
        Array.sort(c, fromIndex, toIndex);
    }

    public static <T> void sort(final List<? extends T> c, final Comparator<? super T> cmp) {
        Array.sort(c, cmp);
    }

    public static <T> void sort(final List<? extends T> c, final int fromIndex, final int toIndex, final Comparator<? super T> cmp) {
        Array.sort(c, fromIndex, toIndex, cmp);
    }

    @SuppressWarnings("rawtypes")
    public static <T, U extends Comparable> void sortBy(final T[] a, final Function<? super T, ? extends U> keyMapper) {
        sort(a, Comparators.comparingBy(keyMapper));
    }

    @SuppressWarnings("rawtypes")
    public static <T, U extends Comparable> void sortBy(final List<? extends T> c, final Function<? super T, ? extends U> keyMapper) {
        sort(c, Comparators.comparingBy(keyMapper));
    }

    public static <T> void sortByInt(final T[] a, final ToIntFunction<? super T> keyMapper) {
        sort(a, Comparators.comparingInt(keyMapper));
    }

    public static <T> void sortByInt(final List<? extends T> c, final ToIntFunction<? super T> keyMapper) {
        sort(c, Comparators.comparingInt(keyMapper));
    }

    public static <T> void sortByLong(final T[] a, final ToLongFunction<? super T> keyMapper) {
        sort(a, Comparators.comparingLong(keyMapper));
    }

    public static <T> void sortByLong(final List<? extends T> c, final ToLongFunction<? super T> keyMapper) {
        sort(c, Comparators.comparingLong(keyMapper));
    }

    public static <T> void sortByFloat(final T[] a, final ToFloatFunction<? super T> keyMapper) {
        sort(a, Comparators.comparingFloat(keyMapper));
    }

    public static <T> void sortByFloat(final List<? extends T> c, final ToFloatFunction<? super T> keyMapper) {
        sort(c, Comparators.comparingFloat(keyMapper));
    }

    public static <T> void sortByDouble(final T[] a, final ToDoubleFunction<? super T> keyMapper) {
        sort(a, Comparators.comparingDouble(keyMapper));
    }

    public static <T> void sortByDouble(final List<? extends T> c, final ToDoubleFunction<? super T> keyMapper) {
        sort(c, Comparators.comparingDouble(keyMapper));
    }

    public static void parallelSort(final char[] a) {
        Array.parallelSort(a);
    }

    public static void parallelSort(final char[] a, final int fromIndex, final int toIndex) {
        Array.parallelSort(a, fromIndex, toIndex);
    }

    public static void parallelSort(final byte[] a) {
        Array.parallelSort(a);
    }

    public static void parallelSort(final byte[] a, final int fromIndex, final int toIndex) {
        Array.parallelSort(a, fromIndex, toIndex);
    }

    public static void parallelSort(final short[] a) {
        Array.parallelSort(a);
    }

    public static void parallelSort(final short[] a, final int fromIndex, final int toIndex) {
        Array.parallelSort(a, fromIndex, toIndex);
    }

    public static void parallelSort(final int[] a) {
        Array.parallelSort(a);
    }

    public static void parallelSort(final int[] a, final int fromIndex, final int toIndex) {
        Array.parallelSort(a, fromIndex, toIndex);
    }

    public static void parallelSort(final long[] a) {
        Array.parallelSort(a);
    }

    public static void parallelSort(final long[] a, final int fromIndex, final int toIndex) {
        Array.parallelSort(a, fromIndex, toIndex);
    }

    public static void parallelSort(final float[] a) {
        Array.parallelSort(a);
    }

    public static void parallelSort(final float[] a, final int fromIndex, final int toIndex) {
        Array.parallelSort(a, fromIndex, toIndex);
    }

    public static void parallelSort(final double[] a) {
        Array.parallelSort(a);
    }

    public static void parallelSort(final double[] a, final int fromIndex, final int toIndex) {
        Array.parallelSort(a, fromIndex, toIndex);
    }

    public static void parallelSort(final Object[] a) {
        Array.parallelSort(a);
    }

    public static void parallelSort(final Object[] a, final int fromIndex, final int toIndex) {
        Array.parallelSort(a, fromIndex, toIndex);
    }

    public static <T> void parallelSort(final T[] a, final Comparator<? super T> cmp) {
        Array.parallelSort(a, cmp);
    }

    public static <T> void parallelSort(final T[] a, final int fromIndex, final int toIndex, final Comparator<? super T> cmp) {
        Array.parallelSort(a, fromIndex, toIndex, cmp);
    }

    public static <T extends Comparable<? super T>> void parallelSort(final List<? extends T> c) {
        Array.parallelSort(c);
    }

    public static <T extends Comparable<? super T>> void parallelSort(final List<? extends T> c, final int fromIndex, final int toIndex) {
        Array.parallelSort(c, fromIndex, toIndex);
    }

    public static <T> void parallelSort(final List<? extends T> c, final Comparator<? super T> cmp) {
        Array.parallelSort(c, cmp);
    }

    public static <T> void parallelSort(final List<? extends T> c, final int fromIndex, final int toIndex, final Comparator<? super T> cmp) {
        Array.parallelSort(c, fromIndex, toIndex, cmp);
    }

    @SuppressWarnings("rawtypes")
    public static <T, U extends Comparable> void parallelSortBy(final T[] a, final Function<? super T, ? extends U> keyMapper) {
        parallelSort(a, Comparators.comparingBy(keyMapper));
    }

    @SuppressWarnings("rawtypes")
    public static <T, U extends Comparable> void parallelSortBy(final List<? extends T> c, final Function<? super T, ? extends U> keyMapper) {
        parallelSort(c, Comparators.comparingBy(keyMapper));
    }

    public static <T> void parallelSortByInt(final T[] a, final ToIntFunction<? super T> keyMapper) {
        parallelSort(a, Comparators.comparingInt(keyMapper));
    }

    public static <T> void parallelSortByInt(final List<? extends T> c, final ToIntFunction<? super T> keyMapper) {
        parallelSort(c, Comparators.comparingInt(keyMapper));
    }

    public static <T> void parallelSortByLong(final T[] a, final ToLongFunction<? super T> keyMapper) {
        parallelSort(a, Comparators.comparingLong(keyMapper));
    }

    public static <T> void parallelSortByLong(final List<? extends T> c, final ToLongFunction<? super T> keyMapper) {
        parallelSort(c, Comparators.comparingLong(keyMapper));
    }

    public static <T> void parallelSortByFloat(final T[] a, final ToFloatFunction<? super T> keyMapper) {
        parallelSort(a, Comparators.comparingFloat(keyMapper));
    }

    public static <T> void parallelSortByFloat(final List<? extends T> c, final ToFloatFunction<? super T> keyMapper) {
        parallelSort(c, Comparators.comparingFloat(keyMapper));
    }

    public static <T> void parallelSortByDouble(final T[] a, final ToDoubleFunction<? super T> keyMapper) {
        parallelSort(a, Comparators.comparingDouble(keyMapper));
    }

    public static <T> void parallelSortByDouble(final List<? extends T> c, final ToDoubleFunction<? super T> keyMapper) {
        parallelSort(c, Comparators.comparingDouble(keyMapper));
    }

    public static void reverseSort(final boolean[] a) {
        Array.reverseSort(a);
    }

    public static void reverseSort(final char[] a) {
        Array.sort(a);
        reverse(a);
    }

    public static void reverseSort(final char[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
        reverse(a, fromIndex, toIndex);
    }

    public static void reverseSort(final byte[] a) {
        Array.sort(a);
        reverse(a);
    }

    public static void reverseSort(final byte[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
        reverse(a, fromIndex, toIndex);
    }

    public static void reverseSort(final short[] a) {
        Array.sort(a);
        reverse(a);
    }

    public static void reverseSort(final short[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
        reverse(a, fromIndex, toIndex);
    }

    public static void reverseSort(final int[] a) {
        Array.sort(a);
        reverse(a);
    }

    public static void reverseSort(final int[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
        reverse(a, fromIndex, toIndex);
    }

    public static void reverseSort(final long[] a) {
        Array.sort(a);
        reverse(a);
    }

    public static void reverseSort(final long[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
        reverse(a, fromIndex, toIndex);
    }

    public static void reverseSort(final float[] a) {
        Array.sort(a);
        reverse(a);
    }

    public static void reverseSort(final float[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
        reverse(a, fromIndex, toIndex);
    }

    public static void reverseSort(final double[] a) {
        Array.sort(a);
        reverse(a);
    }

    public static void reverseSort(final double[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
        reverse(a, fromIndex, toIndex);
    }

    public static void reverseSort(final Object[] a) {
        //        Array.sort(a);
        //        reverse(a);

        sort(a, Fn.reversedOrder());
    }

    public static void reverseSort(final Object[] a, final int fromIndex, final int toIndex) {
        //        Array.sort(a, fromIndex, toIndex);
        //        reverse(a, fromIndex, toIndex);

        sort(a, fromIndex, toIndex, Fn.reversedOrder());
    }

    public static <T extends Comparable<? super T>> void reverseSort(final List<? extends T> c) {
        //        Array.sort(c);
        //        reverse(c);

        sort(c, Fn.reversedOrder());
    }

    public static <T extends Comparable<? super T>> void reverseSort(final List<? extends T> c, final int fromIndex, final int toIndex) {
        //        Array.sort(c, fromIndex, toIndex);
        //        reverse(c, fromIndex, toIndex);

        sort(c, fromIndex, toIndex, Fn.reversedOrder());
    }

    @SuppressWarnings("rawtypes")
    public static <T, U extends Comparable> void reverseSortBy(final T[] a, final Function<? super T, ? extends U> keyMapper) {
        sort(a, Comparators.reversedComparingBy(keyMapper));
    }

    @SuppressWarnings("rawtypes")
    public static <T, U extends Comparable> void reverseSortBy(final List<? extends T> c, final Function<? super T, ? extends U> keyMapper) {
        sort(c, Comparators.reversedComparingBy(keyMapper));
    }

    /*
    public static void bucketSort(final char[] a) {
        M.bucketSort(a);
    }
    
    public static void bucketSort(final char[] a, final int fromIndex, final int toIndex) {
        M.bucketSort(a, fromIndex, toIndex);
    }
    
    public static void bucketSort(final byte[] a) {
        M.bucketSort(a);
    }
    
    public static void bucketSort(final byte[] a, final int fromIndex, final int toIndex) {
        M.bucketSort(a, fromIndex, toIndex);
    }
    
    public static void bucketSort(final short[] a) {
        M.bucketSort(a);
    }
    
    public static void bucketSort(final short[] a, final int fromIndex, final int toIndex) {
        M.bucketSort(a, fromIndex, toIndex);
    }
    */

    public static void bucketSort(final int[] a) {
        Array.bucketSort(a);
    }

    public static void bucketSort(final int[] a, final int fromIndex, final int toIndex) {
        Array.bucketSort(a, fromIndex, toIndex);
    }

    public static void bucketSort(final long[] a) {
        Array.bucketSort(a);
    }

    public static void bucketSort(final long[] a, final int fromIndex, final int toIndex) {
        Array.bucketSort(a, fromIndex, toIndex);
    }

    public static void bucketSort(final float[] a) {
        Array.bucketSort(a);
    }

    public static void bucketSort(final float[] a, final int fromIndex, final int toIndex) {
        Array.bucketSort(a, fromIndex, toIndex);
    }

    public static void bucketSort(final double[] a) {
        Array.bucketSort(a);
    }

    public static void bucketSort(final double[] a, final int fromIndex, final int toIndex) {
        Array.bucketSort(a, fromIndex, toIndex);
    }

    /**
     * Note: All the objects with same value will be replaced with first element with the same value.
     *
     * @param a
     */
    public static void bucketSort(final Object[] a) {
        Array.bucketSort(a);
    }

    /**
     * Note: All the objects with same value will be replaced with first element with the same value.
     *
     * @param a the elements in the array must implements the <code>Comparable</code> interface.
     * @param fromIndex
     * @param toIndex
     */
    public static void bucketSort(final Object[] a, final int fromIndex, final int toIndex) {
        Array.bucketSort(a, fromIndex, toIndex);
    }

    public static <T> void bucketSort(final T[] a, final Comparator<? super T> cmp) {
        Array.bucketSort(a, cmp);
    }

    /**
     * Note: All the objects with same value will be replaced with first element with the same value.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param cmp
     */
    public static <T> void bucketSort(final T[] a, final int fromIndex, final int toIndex, final Comparator<? super T> cmp) {
        Array.bucketSort(a, fromIndex, toIndex, cmp);
    }

    /**
     * Note: All the objects with same value will be replaced with first element with the same value.
     *
     * @param c
     */
    public static <T extends Comparable<T>> void bucketSort(final List<T> c) {
        Array.bucketSort(c);
    }

    /**
     * Note: All the objects with same value will be replaced with first element with the same value.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     */
    public static <T extends Comparable<T>> void bucketSort(final List<T> c, final int fromIndex, final int toIndex) {
        Array.bucketSort(c, fromIndex, toIndex);
    }

    /**
     * Note: All the objects with same value will be replaced with first element with the same value.
     *
     * @param c
     * @param cmp
     */
    public static <T> void bucketSort(final List<? extends T> c, final Comparator<? super T> cmp) {
        Array.bucketSort(c, cmp);
    }

    /**
     * Note: All the objects with same value will be replaced with first element with the same value.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param cmp
     */
    public static <T> void bucketSort(final List<? extends T> c, final int fromIndex, final int toIndex, final Comparator<? super T> cmp) {
        Array.bucketSort(c, fromIndex, toIndex, cmp);
    }

    @SuppressWarnings("rawtypes")
    public static <T, U extends Comparable> void bucketSortBy(final T[] a, final Function<? super T, ? extends U> keyMapper) {
        bucketSort(a, Comparators.comparingBy(keyMapper));
    }

    @SuppressWarnings("rawtypes")
    public static <T, U extends Comparable> void bucketSortBy(final List<? extends T> c, final Function<? super T, ? extends U> keyMapper) {
        bucketSort(c, Comparators.comparingBy(keyMapper));
    }

    /**
     * {@link Arrays#binarySearch(boolean[], boolean)}
     *
     * @param a
     * @param key
     * @return
     */
    static int binarySearch(final boolean[] a, final boolean key) {
        return Array.binarySearch(a, key);
    }

    /**
     * {@link Arrays#binarySearch(char[], char)}
     *
     * @param a
     * @param key
     * @return
     */
    public static int binarySearch(final char[] a, final char key) {
        return Array.binarySearch(a, key);
    }

    /**
     * {@link Arrays#binarySearch(char[], int, int, char)}
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param key
     * @return
     */
    public static int binarySearch(final char[] a, final int fromIndex, final int toIndex, final char key) {
        return Array.binarySearch(a, fromIndex, toIndex, key);
    }

    /**
     * {@link Arrays#binarySearch(byte[], byte)}
     *
     * @param a
     * @param key
     * @return
     */
    public static int binarySearch(final byte[] a, final byte key) {
        return Array.binarySearch(a, key);
    }

    /**
     * {@link Arrays#binarySearch(byte[], int, int, byte)}
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param key
     * @return
     */
    public static int binarySearch(final byte[] a, final int fromIndex, final int toIndex, final byte key) {
        return Array.binarySearch(a, fromIndex, toIndex, key);
    }

    /**
     * {@link Arrays#binarySearch(short[], short)}
     *
     * @param a
     * @param key
     * @return
     */
    public static int binarySearch(final short[] a, final short key) {
        return Array.binarySearch(a, key);
    }

    /**
     * {@link Arrays#binarySearch(short[], int, int, short)}
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param key
     * @return
     */
    public static int binarySearch(final short[] a, final int fromIndex, final int toIndex, final short key) {
        return Array.binarySearch(a, fromIndex, toIndex, key);
    }

    /**
     * {@link Arrays#binarySearch(int[], int)}
     *
     * @param a
     * @param key
     * @return
     */
    public static int binarySearch(final int[] a, final int key) {
        return Array.binarySearch(a, key);
    }

    /**
     * {@link Arrays#binarySearch(int[], int, int, int)}
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param key
     * @return
     */
    public static int binarySearch(final int[] a, final int fromIndex, final int toIndex, final int key) {
        return Array.binarySearch(a, fromIndex, toIndex, key);
    }

    /**
     * {@link Arrays#binarySearch(long[], long)}
     *
     * @param a
     * @param key
     * @return
     */
    public static int binarySearch(final long[] a, final long key) {
        return Array.binarySearch(a, key);
    }

    /**
     * {@link Arrays#binarySearch(long[], int, int, long)}
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param key
     * @return
     */
    public static int binarySearch(final long[] a, final int fromIndex, final int toIndex, final long key) {
        return Array.binarySearch(a, fromIndex, toIndex, key);
    }

    /**
     * {@link Arrays#binarySearch(float[], float)}
     *
     * @param a
     * @param key
     * @return
     */
    public static int binarySearch(final float[] a, final float key) {
        return Array.binarySearch(a, key);
    }

    /**
     * {@link Arrays#binarySearch(float[], int, int, float)}
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param key
     * @return
     */
    public static int binarySearch(final float[] a, final int fromIndex, final int toIndex, final float key) {
        return Array.binarySearch(a, fromIndex, toIndex, key);
    }

    /**
     * {@link Arrays#binarySearch(double[], double)}
     *
     * @param a
     * @param key
     * @return
     */
    public static int binarySearch(final double[] a, final double key) {
        return Array.binarySearch(a, key);
    }

    /**
     * {@link Arrays#binarySearch(double[], int, int, double)}
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param key
     * @return
     */
    public static int binarySearch(final double[] a, final int fromIndex, final int toIndex, final double key) {
        return Array.binarySearch(a, fromIndex, toIndex, key);
    }

    /**
     * {@link Arrays#binarySearch(Object[], Object)}
     *
     * @param a
     * @param key
     * @return
     */
    public static int binarySearch(final Object[] a, final Object key) {
        return Array.binarySearch(a, key);
    }

    /**
     * {@link Arrays#binarySearch(Object[], int, int, Object)}
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param key
     * @return
     */
    public static int binarySearch(final Object[] a, final int fromIndex, final int toIndex, final Object key) {
        return Array.binarySearch(a, fromIndex, toIndex, key);
    }

    /**
     * {@link Arrays#binarySearch(Object[], Object, Comparator)}
     *
     * @param a
     * @param key
     * @param cmp
     * @return
     */
    public static <T> int binarySearch(final T[] a, final T key, final Comparator<? super T> cmp) {
        return Array.binarySearch(a, key, cmp);
    }

    /**
     * {@link Arrays#binarySearch(Object[], int, int, Object, Comparator)}
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param key
     * @param c
     * @return
     */
    public static <T> int binarySearch(final T[] a, final int fromIndex, final int toIndex, final T key, final Comparator<? super T> cmp) {
        return Array.binarySearch(a, fromIndex, toIndex, key, cmp);
    }

    /**
     * {@link Collections#binarySearch(List, Object)}
     *
     * @param items
     * @param key
     * @return
     */
    public static <T extends Comparable<? super T>> int binarySearch(final List<? extends T> c, final T key) {
        return Array.binarySearch(c, key);
    }

    public static <T extends Comparable<? super T>> int binarySearch(final List<? extends T> c, final int fromIndex, final int toIndex, final T key) {
        return Array.binarySearch(c, fromIndex, toIndex, key);
    }

    public static <T> int binarySearch(final List<? extends T> c, final T key, final Comparator<? super T> cmp) {
        return Array.binarySearch(c, key, cmp);
    }

    /**
     *
     * @param c
     * @param key
     * @param fromIndex
     * @param toIndex
     * @param cmp
     * @return
     * @see Collections#binarySearch(List, Object, Comparator)
     */
    public static <T> int binarySearch(final List<? extends T> c, final int fromIndex, final int toIndex, final T key, final Comparator<? super T> cmp) {
        return Array.binarySearch(c, fromIndex, toIndex, key, cmp);
    }

    @SuppressWarnings("rawtypes")
    public static <T, U extends Comparable> int binarySearchBy(final T[] a, final T key, final Function<? super T, ? extends U> keyMapper) {
        return binarySearch(a, key, Comparators.comparingBy(keyMapper));
    }

    @SuppressWarnings("rawtypes")
    public static <T, U extends Comparable> int binarySearchBy(final List<? extends T> c, final T key, final Function<? super T, ? extends U> keyMapper) {
        return binarySearch(c, key, Comparators.comparingBy(keyMapper));
    }

    public static int indexOf(final boolean[] a, final boolean e) {
        return indexOf(a, 0, e);
    }

    /**
     *
     * @param a
     * @param fromIndex
     *            the index from which to start the search.
     * @param e
     * @return
     */
    public static int indexOf(final boolean[] a, final int fromIndex, final boolean e) {
        if (N.isNullOrEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = fromIndex, len = a.length; i < len; i++) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    public static int indexOf(final char[] a, final char e) {
        return indexOf(a, 0, e);
    }

    /**
     *
     * @param a
     * @param fromIndex
     *            the index from which to start the search.
     * @param e
     * @return
     */
    public static int indexOf(final char[] a, final int fromIndex, final char e) {
        if (N.isNullOrEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = fromIndex, len = a.length; i < len; i++) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    public static int indexOf(final byte[] a, final byte e) {
        return indexOf(a, 0, e);

    }

    /**
     *
     * @param a
     * @param fromIndex
     *            the index from which to start the search.
     * @param e
     * @return
     */
    public static int indexOf(final byte[] a, final int fromIndex, final byte e) {
        if (N.isNullOrEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = fromIndex, len = a.length; i < len; i++) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    public static int indexOf(final short[] a, final short e) {
        return indexOf(a, 0, e);
    }

    /**
     *
     * @param a
     * @param fromIndex
     *            the index from which to start the search.
     * @param e
     * @return
     */
    public static int indexOf(final short[] a, final int fromIndex, final short e) {
        if (N.isNullOrEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = fromIndex, len = a.length; i < len; i++) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    public static int indexOf(final int[] a, final int e) {
        return indexOf(a, 0, e);
    }

    /**
     *
     * @param a
     * @param fromIndex
     *            the index from which to start the search.
     * @param e
     * @return
     */
    public static int indexOf(final int[] a, final int fromIndex, final int e) {
        if (N.isNullOrEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = fromIndex, len = a.length; i < len; i++) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    public static int indexOf(final long[] a, final long e) {
        return indexOf(a, 0, e);
    }

    /**
     *
     * @param a
     * @param fromIndex
     *            the index from which to start the search.
     * @param e
     * @return
     */
    public static int indexOf(final long[] a, final int fromIndex, final long e) {
        if (N.isNullOrEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = fromIndex, len = a.length; i < len; i++) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    public static int indexOf(final float[] a, final float e) {
        return indexOf(a, 0, e);
    }

    /**
     *
     * @param a
     * @param fromIndex
     *            the index from which to start the search.
     * @param e
     * @return
     */
    public static int indexOf(final float[] a, final int fromIndex, final float e) {
        if (N.isNullOrEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = fromIndex, len = a.length; i < len; i++) {
            if (Float.compare(a[i], e) == 0) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    public static int indexOf(final double[] a, final double e) {
        return indexOf(a, 0, e);
    }

    /**
     *
     * @param a
     * @param fromIndex
     *            the index from which to start the search.
     * @param e
     * @return
     */
    public static int indexOf(final double[] a, final int fromIndex, final double e) {
        if (N.isNullOrEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = fromIndex, len = a.length; i < len; i++) {
            if (Double.compare(a[i], e) == 0) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    public static int indexOf(final Object[] a, final Object e) {
        return indexOf(a, 0, e);
    }

    /**
     *
     * @param a
     * @param fromIndex
     *            the index from which to start the search.
     * @param e
     * @return
     */
    public static int indexOf(final Object[] a, final int fromIndex, final Object e) {
        if (N.isNullOrEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = fromIndex, len = a.length; i < len; i++) {
            if (equals(a[i], e)) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    public static int indexOf(final List<?> list, final Object e) {
        return indexOf(list, 0, e);
    }

    /**
     *
     * @param list
     * @param fromIndex
     *            the index from which to start the search.
     * @param e
     * @return
     */
    public static int indexOf(final List<?> list, final int fromIndex, final Object e) {
        if (N.isNullOrEmpty(list)) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = fromIndex, len = list.size(); i < len; i++) {
            if (equals(list.get(i), e)) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     *
     * @see java.util.Collections#indexOfSubList(List, List)
     *
     */
    public static int indexOfSubList(final List<?> sourceList, final List<?> targetSubList) {
        if (N.isNullOrEmpty(sourceList) || N.isNullOrEmpty(targetSubList)) {
            return N.INDEX_NOT_FOUND;
        }

        return Collections.indexOfSubList(sourceList, targetSubList);
    }

    public static int lastIndexOf(final boolean[] a, final boolean e) {
        return lastIndexOf(a, a.length - 1, e);
    }

    /**
     *
     * @param a
     * @param fromIndex
     *            the start index to traverse backwards from
     * @param e
     * @return
     */
    public static int lastIndexOf(final boolean[] a, final int fromIndex, final boolean e) {
        if (N.isNullOrEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = min(fromIndex, a.length - 1); i >= 0; i--) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    public static int lastIndexOf(final char[] a, final char e) {
        if (N.isNullOrEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, e);
    }

    /**
     *
     * @param a
     * @param fromIndex
     *            the start index to traverse backwards from
     * @param e
     * @return
     */
    public static int lastIndexOf(final char[] a, final int fromIndex, final char e) {
        if (N.isNullOrEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = min(fromIndex, a.length - 1); i >= 0; i--) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    public static int lastIndexOf(final byte[] a, final byte e) {
        if (N.isNullOrEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, e);

    }

    /**
     *
     * @param a
     * @param fromIndex
     *            the start index to traverse backwards from
     * @param e
     * @return
     */
    public static int lastIndexOf(final byte[] a, final int fromIndex, final byte e) {
        if (N.isNullOrEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = min(fromIndex, a.length - 1); i >= 0; i--) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    public static int lastIndexOf(final short[] a, final short e) {
        if (N.isNullOrEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, e);
    }

    /**
     *
     * @param a
     * @param fromIndex
     *            the start index to traverse backwards from
     * @param e
     * @return
     */
    public static int lastIndexOf(final short[] a, final int fromIndex, final short e) {
        if (N.isNullOrEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = min(fromIndex, a.length - 1); i >= 0; i--) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    public static int lastIndexOf(final int[] a, final int e) {
        if (N.isNullOrEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, e);
    }

    /**
     *
     * @param a
     * @param fromIndex
     *            the start index to traverse backwards from
     * @param e
     * @return
     */
    public static int lastIndexOf(final int[] a, final int fromIndex, final int e) {
        if (N.isNullOrEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = min(fromIndex, a.length - 1); i >= 0; i--) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    public static int lastIndexOf(final long[] a, final long e) {
        if (N.isNullOrEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, e);
    }

    /**
     *
     * @param a
     * @param fromIndex
     *            the start index to traverse backwards from
     * @param e
     * @return
     */
    public static int lastIndexOf(final long[] a, final int fromIndex, final long e) {
        if (N.isNullOrEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = min(fromIndex, a.length - 1); i >= 0; i--) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    public static int lastIndexOf(final float[] a, final float e) {
        if (N.isNullOrEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, e);
    }

    /**
     *
     * @param a
     * @param fromIndex
     *            the start index to traverse backwards from
     * @param e
     * @return
     */
    public static int lastIndexOf(final float[] a, final int fromIndex, final float e) {
        if (N.isNullOrEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = min(fromIndex, a.length - 1); i >= 0; i--) {
            if (Float.compare(a[i], e) == 0) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    public static int lastIndexOf(final double[] a, final double e) {
        if (N.isNullOrEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, e);
    }

    /**
     *
     * @param a
     * @param fromIndex
     *            the start index to traverse backwards from
     * @param e
     * @return
     */
    public static int lastIndexOf(final double[] a, final int fromIndex, final double e) {
        if (N.isNullOrEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = min(fromIndex, a.length - 1); i >= 0; i--) {
            if (Double.compare(a[i], e) == 0) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    public static int lastIndexOf(final Object[] a, final Object e) {
        if (N.isNullOrEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, e);
    }

    /**
     *
     * @param a
     * @param fromIndex
     *            the start index to traverse backwards from
     * @param e
     * @return
     */
    public static int lastIndexOf(final Object[] a, final int fromIndex, final Object e) {
        if (N.isNullOrEmpty(a)) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = min(fromIndex, a.length - 1); i >= 0; i--) {
            if (equals(a[i], e)) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    public static int lastIndexOf(final List<?> list, final Object e) {
        if (N.isNullOrEmpty(list)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(list, list.size() - 1, e);
    }

    /**
     *
     * @param list
     * @param fromIndex
     *            the start index to traverse backwards from
     * @param e
     * @return
     */
    public static int lastIndexOf(final List<?> list, final int fromIndex, final Object e) {
        if (N.isNullOrEmpty(list)) {
            return N.INDEX_NOT_FOUND;
        }

        for (int i = min(fromIndex, list.size() - 1); i >= 0; i--) {
            if (equals(list.get(i), e)) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     *
     * @see java.util.Collections#lastIndexOfSubList(List, List)
     */
    public static int lastIndexOfSubList(final List<?> sourceList, final List<?> targetSubList) {
        if (N.isNullOrEmpty(sourceList) || N.isNullOrEmpty(targetSubList)) {
            return N.INDEX_NOT_FOUND;
        }

        return Collections.lastIndexOfSubList(sourceList, targetSubList);
    }

    public static int occurrencesOf(final boolean[] a, final boolean objectToFind) {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int occurrences = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == objectToFind) {
                occurrences++;
            }
        }

        return occurrences;
    }

    public static int occurrencesOf(final char[] a, final char objectToFind) {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int occurrences = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == objectToFind) {
                occurrences++;
            }
        }

        return occurrences;
    }

    public static int occurrencesOf(final byte[] a, final byte objectToFind) {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int occurrences = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == objectToFind) {
                occurrences++;
            }
        }

        return occurrences;
    }

    public static int occurrencesOf(final short[] a, final short objectToFind) {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int occurrences = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == objectToFind) {
                occurrences++;
            }
        }

        return occurrences;
    }

    public static int occurrencesOf(final int[] a, final int objectToFind) {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int occurrences = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == objectToFind) {
                occurrences++;
            }
        }

        return occurrences;
    }

    public static int occurrencesOf(final long[] a, final long objectToFind) {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int occurrences = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == objectToFind) {
                occurrences++;
            }
        }

        return occurrences;
    }

    public static int occurrencesOf(final float[] a, final float objectToFind) {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int occurrences = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (Float.compare(a[i], objectToFind) == 0) {
                occurrences++;
            }
        }

        return occurrences;
    }

    public static int occurrencesOf(final double[] a, final double objectToFind) {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int occurrences = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (Double.compare(a[i], objectToFind) == 0) {
                occurrences++;
            }
        }

        return occurrences;
    }

    public static int occurrencesOf(final Object[] a, final Object objectToFind) {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int occurrences = 0;

        if (objectToFind == null) {
            for (int i = 0, len = a.length; i < len; i++) {
                if (a[i] == null) {
                    occurrences++;
                }
            }
        } else {
            for (int i = 0, len = a.length; i < len; i++) {
                if (objectToFind.equals(a[i])) {
                    occurrences++;
                }
            }
        }

        return occurrences;
    }

    /**
     *
     * @param c
     * @param objectToFind
     * @return
     *
     * @see java.util.Collections#frequency(Collection, Object)
     */
    public static int occurrencesOf(final Collection<?> c, final Object objectToFind) {
        if (N.isNullOrEmpty(c)) {
            return 0;
        }

        return Collections.frequency(c, objectToFind);
    }

    public static boolean contains(final boolean[] a, final boolean objectToFind) {
        return indexOf(a, objectToFind) != INDEX_NOT_FOUND;
    }

    public static boolean contains(final char[] a, final char objectToFind) {
        return indexOf(a, objectToFind) != INDEX_NOT_FOUND;
    }

    public static boolean contains(final byte[] a, final byte objectToFind) {
        return indexOf(a, objectToFind) != INDEX_NOT_FOUND;
    }

    public static boolean contains(final short[] a, final short objectToFind) {
        return indexOf(a, objectToFind) != INDEX_NOT_FOUND;
    }

    public static boolean contains(final int[] a, final int objectToFind) {
        return indexOf(a, objectToFind) != INDEX_NOT_FOUND;
    }

    public static boolean contains(final long[] a, final long objectToFind) {
        return indexOf(a, objectToFind) != INDEX_NOT_FOUND;
    }

    public static boolean contains(final float[] a, final float objectToFind) {
        return indexOf(a, objectToFind) != INDEX_NOT_FOUND;
    }

    public static boolean contains(final double[] a, final double objectToFind) {
        return indexOf(a, objectToFind) != INDEX_NOT_FOUND;
    }

    public static boolean contains(final Object[] a, final Object objectToFind) {
        return indexOf(a, objectToFind) != INDEX_NOT_FOUND;
    }

    public static boolean contains(final Collection<?> c, final Object e) {
        if (N.isNullOrEmpty(c)) {
            return false;
        }

        return c.contains(e);
    }

    public static <E extends Exception> void forEach(final int startInclusive, final int endExclusive, Try.IntConsumer<E> action) throws E {
        forEach(startInclusive, endExclusive, 1, action);
    }

    public static <E extends Exception> void forEach(final int startInclusive, final int endExclusive, final int step, Try.IntConsumer<E> action) throws E {
        N.checkArgument(step != 0, "The input parameter 'step' can not be zero");

        if (endExclusive == startInclusive || endExclusive > startInclusive != step > 0) {
            return;
        }

        long len = (endExclusive * 1L - startInclusive) / step + ((endExclusive * 1L - startInclusive) % step == 0 ? 0 : 1);
        int start = startInclusive;

        while (len-- > 0) {
            action.accept(start);
            start += step;
        }
    }

    public static <T, E extends Exception> void forEach(final int startInclusive, final int endExclusive, final T a, Try.ObjIntConsumer<? super T, E> action)
            throws E {
        forEach(startInclusive, endExclusive, 1, a, action);
    }

    public static <T, E extends Exception> void forEach(final int startInclusive, final int endExclusive, final int step, final T a,
            Try.ObjIntConsumer<? super T, E> action) throws E {
        N.checkArgument(step != 0, "The input parameter 'step' can not be zero");

        if (endExclusive == startInclusive || endExclusive > startInclusive != step > 0) {
            return;
        }

        long len = (endExclusive * 1L - startInclusive) / step + ((endExclusive * 1L - startInclusive) % step == 0 ? 0 : 1);
        int start = startInclusive;

        while (len-- > 0) {
            action.accept(a, start);
            start += step;
        }
    }

    public static <T, E extends Exception> void forEach(final T[] a, final Try.Consumer<? super T, E> action) throws E {
        N.checkArgNotNull(action);

        if (N.isNullOrEmpty(a)) {
            return;
        }

        for (T e : a) {
            action.accept(e);
        }
    }

    public static <T, E extends Exception> void forEach(final T[] a, final int fromIndex, final int toIndex, final Try.Consumer<? super T, E> action) throws E {
        N.checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex, len(a));
        N.checkArgNotNull(action);

        if (N.isNullOrEmpty(a)) {
            return;
        }

        if (fromIndex <= toIndex) {
            for (int i = fromIndex; i < toIndex; i++) {
                action.accept(a[i]);
            }
        } else {
            for (int i = min(a.length - 1, toIndex); i > toIndex; i--) {
                action.accept(a[i]);
            }
        }
    }

    public static <T, E extends Exception> void forEach(final T[] a, final Try.IndexedConsumer<? super T, E> action) throws E {
        N.checkArgNotNull(action);

        if (N.isNullOrEmpty(a)) {
            return;
        }

        forEach(a, 0, a.length, action);
    }

    public static <T, E extends Exception> void forEach(final T[] a, final int fromIndex, final int toIndex, final Try.IndexedConsumer<? super T, E> action)
            throws E {
        N.checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex, len(a));
        N.checkArgNotNull(action);

        if (N.isNullOrEmpty(a)) {
            return;
        }

        if (fromIndex <= toIndex) {
            for (int i = fromIndex; i < toIndex; i++) {
                action.accept(i, a[i]);
            }
        } else {
            for (int i = min(a.length - 1, toIndex); i > toIndex; i--) {
                action.accept(i, a[i]);
            }
        }
    }

    public static <T, C extends Collection<? extends T>, E extends Exception> void forEach(final C c, final Try.Consumer<? super T, E> action) throws E {
        N.checkArgNotNull(action);

        if (N.isNullOrEmpty(c)) {
            return;
        }

        for (T e : c) {
            action.accept(e);
        }
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * Note: This is NOT a replacement of traditional for loop statement.
     * The traditional for loop is still recommended in regular programming.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param action
     */
    public static <T, C extends Collection<? extends T>, E extends Exception> void forEach(final C c, int fromIndex, final int toIndex,
            final Try.Consumer<? super T, E> action) throws E {
        N.checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex, size(c));
        N.checkArgNotNull(action);

        if (N.isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return;
        }

        fromIndex = N.min(c.size() - 1, fromIndex);

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            if (fromIndex <= toIndex) {
                for (int i = fromIndex; i < toIndex; i++) {
                    action.accept(list.get(i));
                }
            } else {
                for (int i = fromIndex; i > toIndex; i--) {
                    action.accept(list.get(i));
                }
            }
        } else {
            final Iterator<? extends T> iter = c.iterator();
            int idx = 0;

            if (fromIndex <= toIndex) {
                while (idx < fromIndex && iter.hasNext()) {
                    iter.next();
                    idx++;
                }

                while (iter.hasNext()) {
                    action.accept(iter.next());

                    if (++idx >= toIndex) {
                        break;
                    }
                }
            } else {
                while (idx <= toIndex && iter.hasNext()) {
                    iter.next();
                    idx++;
                }

                final T[] a = (T[]) new Object[fromIndex - toIndex];

                while (iter.hasNext()) {
                    a[idx - 1 - toIndex] = iter.next();

                    if (idx++ >= fromIndex) {
                        break;
                    }
                }

                for (int i = a.length - 1; i >= 0; i--) {
                    action.accept(a[i]);
                }
            }
        }
    }

    public static <T, C extends Collection<? extends T>, E extends Exception> void forEach(final C c, final Try.IndexedConsumer<? super T, E> action) throws E {
        N.checkArgNotNull(action);

        if (N.isNullOrEmpty(c)) {
            return;
        }

        int idx = 0;
        for (T e : c) {
            action.accept(idx++, e);
        }
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * Note: This is NOT a replacement of traditional for loop statement.
     * The traditional for loop is still recommended in regular programming.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param action
     */
    public static <T, C extends Collection<? extends T>, E extends Exception> void forEach(final C c, int fromIndex, final int toIndex,
            final Try.IndexedConsumer<? super T, E> action) throws E {
        N.checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex, size(c));
        N.checkArgNotNull(action);

        if (N.isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return;
        }

        fromIndex = N.min(c.size() - 1, fromIndex);

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            if (fromIndex <= toIndex) {
                for (int i = fromIndex; i < toIndex; i++) {
                    action.accept(i, list.get(i));
                }
            } else {
                for (int i = fromIndex; i > toIndex; i--) {
                    action.accept(i, list.get(i));
                }
            }
        } else {
            final Iterator<? extends T> iter = c.iterator();
            int idx = 0;

            if (fromIndex < toIndex) {
                while (idx < fromIndex && iter.hasNext()) {
                    iter.next();
                    idx++;
                }

                while (iter.hasNext()) {
                    action.accept(idx, iter.next());

                    if (++idx >= toIndex) {
                        break;
                    }
                }
            } else {
                while (idx <= toIndex && iter.hasNext()) {
                    iter.next();
                    idx++;
                }

                final T[] a = (T[]) new Object[fromIndex - toIndex];

                while (iter.hasNext()) {
                    a[idx - 1 - toIndex] = iter.next();

                    if (idx++ >= fromIndex) {
                        break;
                    }
                }

                for (int i = a.length - 1; i >= 0; i--) {
                    action.accept(i + toIndex + 1, a[i]);
                }
            }
        }
    }

    public static <T, U, E extends Exception, E2 extends Exception> void forEach(final T[] a,
            final Try.Function<? super T, ? extends Collection<U>, E> flatMapper, final Try.BiConsumer<? super T, ? super U, E2> action) throws E, E2 {
        N.checkArgNotNull(flatMapper);
        N.checkArgNotNull(action);

        if (N.isNullOrEmpty(a)) {
            return;
        }

        for (T e : a) {
            final Collection<U> c2 = flatMapper.apply(e);

            if (N.notNullOrEmpty(c2)) {
                for (U u : c2) {
                    action.accept(e, u);
                }
            }
        }
    }

    public static <T, U, E extends Exception, E2 extends Exception> void forEach(final Collection<T> c,
            final Try.Function<? super T, ? extends Collection<U>, E> flatMapper, final Try.BiConsumer<? super T, ? super U, E2> action) throws E, E2 {
        N.checkArgNotNull(flatMapper);
        N.checkArgNotNull(action);

        if (N.isNullOrEmpty(c)) {
            return;
        }

        for (T e : c) {
            final Collection<U> c2 = flatMapper.apply(e);

            if (N.notNullOrEmpty(c2)) {
                for (U u : c2) {
                    action.accept(e, u);
                }
            }
        }
    }

    public static <T, T2, T3, E extends Exception, E2 extends Exception, E3 extends Exception> void forEach(final T[] a,
            final Try.Function<? super T, ? extends Collection<T2>, E> flatMapper, final Try.Function<? super T2, ? extends Collection<T3>, E2> flatMapper2,
            final Try.TriConsumer<? super T, ? super T2, ? super T3, E3> action) throws E, E2, E3 {
        N.checkArgNotNull(flatMapper);
        N.checkArgNotNull(flatMapper2);
        N.checkArgNotNull(action);

        if (N.isNullOrEmpty(a)) {
            return;
        }

        for (T e : a) {
            final Collection<T2> c2 = flatMapper.apply(e);

            if (N.notNullOrEmpty(c2)) {
                for (T2 t2 : c2) {
                    final Collection<T3> c3 = flatMapper2.apply(t2);

                    if (N.notNullOrEmpty(c3)) {
                        for (T3 t3 : c3) {
                            action.accept(e, t2, t3);
                        }
                    }
                }
            }
        }
    }

    public static <T, T2, T3, E extends Exception, E2 extends Exception, E3 extends Exception> void forEach(final Collection<T> c,
            final Try.Function<? super T, ? extends Collection<T2>, E> flatMapper, final Try.Function<? super T2, ? extends Collection<T3>, E2> flatMapper2,
            final Try.TriConsumer<? super T, ? super T2, ? super T3, E3> action) throws E, E2, E3 {
        N.checkArgNotNull(flatMapper);
        N.checkArgNotNull(flatMapper2);
        N.checkArgNotNull(action);

        if (N.isNullOrEmpty(c)) {
            return;
        }

        for (T e : c) {
            final Collection<T2> c2 = flatMapper.apply(e);

            if (N.notNullOrEmpty(c2)) {
                for (T2 t2 : c2) {
                    final Collection<T3> c3 = flatMapper2.apply(t2);

                    if (N.notNullOrEmpty(c3)) {
                        for (T3 t3 : c3) {
                            action.accept(e, t2, t3);
                        }
                    }
                }
            }
        }
    }

    public static <A, B, E extends Exception> void forEach(final A[] a, final B[] b, final Try.BiConsumer<? super A, ? super B, E> action) throws E {
        N.checkArgNotNull(action);

        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
            return;
        }

        for (int i = 0, minLen = N.min(a.length, b.length); i < minLen; i++) {
            action.accept(a[i], b[i]);
        }
    }

    public static <A, B, E extends Exception> void forEach(final Collection<A> a, final Collection<B> b, final Try.BiConsumer<? super A, ? super B, E> action)
            throws E {
        N.checkArgNotNull(action);

        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
            return;
        }

        final Iterator<A> iterA = a.iterator();
        final Iterator<B> iterB = b.iterator();

        for (int i = 0, minLen = N.min(a.size(), b.size()); i < minLen; i++) {
            action.accept(iterA.next(), iterB.next());
        }
    }

    public static <A, B, C, E extends Exception> void forEach(final A[] a, final B[] b, final C[] c,
            final Try.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
        N.checkArgNotNull(action);

        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b) || N.isNullOrEmpty(c)) {
            return;
        }

        for (int i = 0, minLen = N.min(a.length, b.length, c.length); i < minLen; i++) {
            action.accept(a[i], b[i], c[i]);
        }
    }

    public static <A, B, C, E extends Exception> void forEach(final Collection<A> a, final Collection<B> b, final Collection<C> c,
            final Try.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
        N.checkArgNotNull(action);

        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b) || N.isNullOrEmpty(c)) {
            return;
        }

        final Iterator<A> iterA = a.iterator();
        final Iterator<B> iterB = b.iterator();
        final Iterator<C> iterC = c.iterator();

        for (int i = 0, minLen = N.min(a.size(), b.size(), c.size()); i < minLen; i++) {
            action.accept(iterA.next(), iterB.next(), iterC.next());
        }
    }

    public static <A, B, E extends Exception> void forEach(final A[] a, final B[] b, final A valueForNoneA, final B valueForNoneB,
            final Try.BiConsumer<? super A, ? super B, E> action) throws E {
        N.checkArgNotNull(action);

        final int lenA = len(a);
        final int lenB = len(b);

        for (int i = 0, maxLen = N.max(lenA, lenB); i < maxLen; i++) {
            action.accept(i < lenA ? a[i] : valueForNoneA, i < lenB ? b[i] : valueForNoneB);
        }
    }

    public static <A, B, E extends Exception> void forEach(final Collection<A> a, final Collection<B> b, final A valueForNoneA, final B valueForNoneB,
            final Try.BiConsumer<? super A, ? super B, E> action) throws E {
        N.checkArgNotNull(action);

        final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a.iterator();
        final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b.iterator();
        final int lenA = size(a);
        final int lenB = size(b);

        for (int i = 0, maxLen = N.max(lenA, lenB); i < maxLen; i++) {
            action.accept(i < lenA ? iterA.next() : valueForNoneA, i < lenB ? iterB.next() : valueForNoneB);
        }
    }

    public static <A, B, C, E extends Exception> void forEach(final A[] a, final B[] b, final C[] c, final A valueForNoneA, final B valueForNoneB,
            final C valueForNoneC, final Try.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
        N.checkArgNotNull(action);

        final int lenA = len(a);
        final int lenB = len(b);
        final int lenC = len(c);

        for (int i = 0, maxLen = N.max(lenA, lenB, lenC); i < maxLen; i++) {
            action.accept(i < lenA ? a[i] : valueForNoneA, i < lenB ? b[i] : valueForNoneB, i < lenC ? c[i] : valueForNoneC);
        }
    }

    public static <A, B, C, E extends Exception> void forEach(final Collection<A> a, final Collection<B> b, final Collection<C> c, final A valueForNoneA,
            final B valueForNoneB, final C valueForNoneC, final Try.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
        N.checkArgNotNull(action);

        final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a.iterator();
        final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b.iterator();
        final Iterator<C> iterC = c == null ? ObjIterator.<C> empty() : c.iterator();
        final int lenA = size(a);
        final int lenB = size(b);
        final int lenC = size(c);

        for (int i = 0, maxLen = N.max(lenA, lenB, lenC); i < maxLen; i++) {
            action.accept(i < lenA ? iterA.next() : valueForNoneA, i < lenB ? iterB.next() : valueForNoneB, i < lenC ? iterC.next() : valueForNoneC);
        }
    }

    public static <T, E extends Exception> void forEachNonNull(final T[] a, final Try.Consumer<? super T, E> action) throws E {
        N.checkArgNotNull(action);

        if (N.isNullOrEmpty(a)) {
            return;
        }

        for (T e : a) {
            if (e != null) {
                action.accept(e);
            }
        }
    }

    public static <T, E extends Exception> void forEachNonNull(final Collection<T> c, final Try.Consumer<? super T, E> action) throws E {
        N.checkArgNotNull(action);

        if (N.isNullOrEmpty(c)) {
            return;
        }

        for (T e : c) {
            if (e != null) {
                action.accept(e);
            }
        }
    }

    public static <T, U, E extends Exception, E2 extends Exception> void forEachNonNull(final T[] a,
            final Try.Function<? super T, ? extends Collection<U>, E> flatMapper, final Try.BiConsumer<? super T, ? super U, E2> action) throws E, E2 {
        N.checkArgNotNull(flatMapper);
        N.checkArgNotNull(action);

        if (N.isNullOrEmpty(a)) {
            return;
        }

        for (T e : a) {
            if (e != null) {
                final Collection<U> c2 = flatMapper.apply(e);

                if (N.notNullOrEmpty(c2)) {
                    for (U u : c2) {
                        if (u != null) {
                            action.accept(e, u);
                        }
                    }
                }
            }
        }
    }

    public static <T, U, E extends Exception, E2 extends Exception> void forEachNonNull(final Collection<T> c,
            final Try.Function<? super T, ? extends Collection<U>, E> flatMapper, final Try.BiConsumer<? super T, ? super U, E2> action) throws E, E2 {
        N.checkArgNotNull(flatMapper);
        N.checkArgNotNull(action);

        if (N.isNullOrEmpty(c)) {
            return;
        }

        for (T e : c) {
            if (e != null) {
                final Collection<U> c2 = flatMapper.apply(e);

                if (N.notNullOrEmpty(c2)) {
                    for (U u : c2) {
                        if (u != null) {
                            action.accept(e, u);
                        }
                    }
                }
            }
        }
    }

    public static <T, T2, T3, E extends Exception, E2 extends Exception, E3 extends Exception> void forEachNonNull(final T[] a,
            final Try.Function<? super T, ? extends Collection<T2>, E> flatMapper, final Try.Function<? super T2, ? extends Collection<T3>, E2> flatMapper2,
            final Try.TriConsumer<? super T, ? super T2, ? super T3, E3> action) throws E, E2, E3 {
        N.checkArgNotNull(flatMapper);
        N.checkArgNotNull(flatMapper2);
        N.checkArgNotNull(action);

        if (N.isNullOrEmpty(a)) {
            return;
        }

        for (T e : a) {
            if (e != null) {
                final Collection<T2> c2 = flatMapper.apply(e);

                if (N.notNullOrEmpty(c2)) {
                    for (T2 t2 : c2) {
                        if (t2 != null) {
                            final Collection<T3> c3 = flatMapper2.apply(t2);

                            if (N.notNullOrEmpty(c3)) {
                                for (T3 t3 : c3) {
                                    if (t3 != null) {
                                        action.accept(e, t2, t3);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static <T, T2, T3, E extends Exception, E2 extends Exception, E3 extends Exception> void forEachNonNull(final Collection<T> c,
            final Try.Function<? super T, ? extends Collection<T2>, E> flatMapper, final Try.Function<? super T2, ? extends Collection<T3>, E2> flatMapper2,
            final Try.TriConsumer<? super T, ? super T2, ? super T3, E3> action) throws E, E2, E3 {
        N.checkArgNotNull(flatMapper);
        N.checkArgNotNull(flatMapper2);
        N.checkArgNotNull(action);

        if (N.isNullOrEmpty(c)) {
            return;
        }

        for (T e : c) {
            if (e != null) {
                final Collection<T2> c2 = flatMapper.apply(e);

                if (N.notNullOrEmpty(c2)) {
                    for (T2 t2 : c2) {
                        if (t2 != null) {
                            final Collection<T3> c3 = flatMapper2.apply(t2);

                            if (N.notNullOrEmpty(c3)) {
                                for (T3 t3 : c3) {
                                    if (t3 != null) {
                                        action.accept(e, t2, t3);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static <E extends Exception> BooleanList filter(final boolean[] a, final Try.BooleanPredicate<E> filter) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new BooleanList();
        }

        return filter(a, 0, a.length, filter);
    }

    public static <E extends Exception> BooleanList filter(final boolean[] a, final Try.BooleanPredicate<E> filter, final int max) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new BooleanList();
        }

        return filter(a, 0, a.length, filter, max);
    }

    public static <E extends Exception> BooleanList filter(final boolean[] a, final int fromIndex, final int toIndex, final Try.BooleanPredicate<E> filter)
            throws E {
        return filter(a, fromIndex, toIndex, filter, Integer.MAX_VALUE);
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max maximum return result.
     * @return
     */
    public static <E extends Exception> BooleanList filter(final boolean[] a, final int fromIndex, final int toIndex, final Try.BooleanPredicate<E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new BooleanList();
        }

        final BooleanList result = new BooleanList(min(9, max, (toIndex - fromIndex)));

        for (int i = fromIndex, cnt = 0; i < toIndex && cnt < max; i++) {
            if (filter.test(a[i])) {
                result.add(a[i]);
                cnt++;
            }
        }

        return result;
    }

    public static <E extends Exception> CharList filter(final char[] a, final Try.CharPredicate<E> filter) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new CharList();
        }

        return filter(a, 0, a.length, filter);
    }

    public static <E extends Exception> CharList filter(final char[] a, final Try.CharPredicate<E> filter, final int max) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new CharList();
        }

        return filter(a, 0, a.length, filter, max);
    }

    public static <E extends Exception> CharList filter(final char[] a, final int fromIndex, final int toIndex, final Try.CharPredicate<E> filter) throws E {
        return filter(a, fromIndex, toIndex, filter, Integer.MAX_VALUE);
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max maximum return result.
     * @return
     */
    public static <E extends Exception> CharList filter(final char[] a, final int fromIndex, final int toIndex, final Try.CharPredicate<E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new CharList();
        }

        final CharList result = new CharList(min(9, max, (toIndex - fromIndex)));

        for (int i = fromIndex, cnt = 0; i < toIndex && cnt < max; i++) {
            if (filter.test(a[i])) {
                result.add(a[i]);
                cnt++;
            }
        }

        return result;
    }

    public static <E extends Exception> ByteList filter(final byte[] a, final Try.BytePredicate<E> filter) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new ByteList();
        }

        return filter(a, 0, a.length, filter);
    }

    public static <E extends Exception> ByteList filter(final byte[] a, final Try.BytePredicate<E> filter, final int max) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new ByteList();
        }

        return filter(a, 0, a.length, filter, max);
    }

    public static <E extends Exception> ByteList filter(final byte[] a, final int fromIndex, final int toIndex, final Try.BytePredicate<E> filter) throws E {
        return filter(a, fromIndex, toIndex, filter, Integer.MAX_VALUE);
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max maximum return result.
     * @return
     */
    public static <E extends Exception> ByteList filter(final byte[] a, final int fromIndex, final int toIndex, final Try.BytePredicate<E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new ByteList();
        }

        final ByteList result = new ByteList(min(9, max, (toIndex - fromIndex)));

        for (int i = fromIndex, cnt = 0; i < toIndex && cnt < max; i++) {
            if (filter.test(a[i])) {
                result.add(a[i]);
                cnt++;
            }
        }

        return result;
    }

    public static <E extends Exception> ShortList filter(final short[] a, final Try.ShortPredicate<E> filter) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new ShortList();
        }

        return filter(a, 0, a.length, filter);
    }

    public static <E extends Exception> ShortList filter(final short[] a, final Try.ShortPredicate<E> filter, final int max) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new ShortList();
        }

        return filter(a, 0, a.length, filter, max);
    }

    public static <E extends Exception> ShortList filter(final short[] a, final int fromIndex, final int toIndex, final Try.ShortPredicate<E> filter) throws E {
        return filter(a, fromIndex, toIndex, filter, Integer.MAX_VALUE);
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max maximum return result.
     * @return
     */
    public static <E extends Exception> ShortList filter(final short[] a, final int fromIndex, final int toIndex, final Try.ShortPredicate<E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new ShortList();
        }

        final ShortList result = new ShortList(min(9, max, (toIndex - fromIndex)));

        for (int i = fromIndex, cnt = 0; i < toIndex && cnt < max; i++) {
            if (filter.test(a[i])) {
                result.add(a[i]);
                cnt++;
            }
        }

        return result;
    }

    public static <E extends Exception> IntList filter(final int[] a, final Try.IntPredicate<E> filter) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new IntList();
        }

        return filter(a, 0, a.length, filter);
    }

    public static <E extends Exception> IntList filter(final int[] a, final Try.IntPredicate<E> filter, final int max) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new IntList();
        }

        return filter(a, 0, a.length, filter, max);
    }

    public static <E extends Exception> IntList filter(final int[] a, final int fromIndex, final int toIndex, final Try.IntPredicate<E> filter) throws E {
        return filter(a, fromIndex, toIndex, filter, Integer.MAX_VALUE);
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max maximum return result.
     * @return
     */
    public static <E extends Exception> IntList filter(final int[] a, final int fromIndex, final int toIndex, final Try.IntPredicate<E> filter, final int max)
            throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new IntList();
        }

        final IntList result = new IntList(min(9, max, (toIndex - fromIndex)));

        for (int i = fromIndex, cnt = 0; i < toIndex && cnt < max; i++) {
            if (filter.test(a[i])) {
                result.add(a[i]);
                cnt++;
            }
        }

        return result;
    }

    public static <E extends Exception> LongList filter(final long[] a, final Try.LongPredicate<E> filter) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new LongList();
        }

        return filter(a, 0, a.length, filter);
    }

    public static <E extends Exception> LongList filter(final long[] a, final Try.LongPredicate<E> filter, final int max) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new LongList();
        }

        return filter(a, 0, a.length, filter, max);
    }

    public static <E extends Exception> LongList filter(final long[] a, final int fromIndex, final int toIndex, final Try.LongPredicate<E> filter) throws E {
        return filter(a, fromIndex, toIndex, filter, Integer.MAX_VALUE);
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max maximum return result.
     * @return
     */
    public static <E extends Exception> LongList filter(final long[] a, final int fromIndex, final int toIndex, final Try.LongPredicate<E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new LongList();
        }

        final LongList result = new LongList(min(9, max, (toIndex - fromIndex)));

        for (int i = fromIndex, cnt = 0; i < toIndex && cnt < max; i++) {
            if (filter.test(a[i])) {
                result.add(a[i]);
                cnt++;
            }
        }

        return result;
    }

    public static <E extends Exception> FloatList filter(final float[] a, final Try.FloatPredicate<E> filter) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new FloatList();
        }

        return filter(a, 0, a.length, filter);
    }

    public static <E extends Exception> FloatList filter(final float[] a, final Try.FloatPredicate<E> filter, final int max) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new FloatList();
        }

        return filter(a, 0, a.length, filter, max);
    }

    public static <E extends Exception> FloatList filter(final float[] a, final int fromIndex, final int toIndex, final Try.FloatPredicate<E> filter) throws E {
        return filter(a, fromIndex, toIndex, filter, Integer.MAX_VALUE);
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max maximum return result.
     * @return
     */
    public static <E extends Exception> FloatList filter(final float[] a, final int fromIndex, final int toIndex, final Try.FloatPredicate<E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new FloatList();
        }

        final FloatList result = new FloatList(min(9, max, (toIndex - fromIndex)));

        for (int i = fromIndex, cnt = 0; i < toIndex && cnt < max; i++) {
            if (filter.test(a[i])) {
                result.add(a[i]);
                cnt++;
            }
        }

        return result;
    }

    public static <E extends Exception> DoubleList filter(final double[] a, final Try.DoublePredicate<E> filter) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new DoubleList();
        }

        return filter(a, 0, a.length, filter);
    }

    public static <E extends Exception> DoubleList filter(final double[] a, final Try.DoublePredicate<E> filter, final int max) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new DoubleList();
        }

        return filter(a, 0, a.length, filter, max);
    }

    public static <E extends Exception> DoubleList filter(final double[] a, final int fromIndex, final int toIndex, final Try.DoublePredicate<E> filter)
            throws E {
        return filter(a, fromIndex, toIndex, filter, Integer.MAX_VALUE);
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max maximum return result.
     * @return
     */
    public static <E extends Exception> DoubleList filter(final double[] a, final int fromIndex, final int toIndex, final Try.DoublePredicate<E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new DoubleList();
        }

        final DoubleList result = new DoubleList(min(9, max, (toIndex - fromIndex)));

        for (int i = fromIndex, cnt = 0; i < toIndex && cnt < max; i++) {
            if (filter.test(a[i])) {
                result.add(a[i]);
                cnt++;
            }
        }

        return result;
    }

    public static <T, E extends Exception> List<T> filter(final T[] a, final Try.Predicate<? super T, E> filter) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        return filter(a, filter, Integer.MAX_VALUE);
    }

    public static <T, E extends Exception> List<T> filter(final T[] a, final Try.Predicate<? super T, E> filter, final int max) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        return filter(a, 0, a.length, filter, max);
    }

    public static <T, E extends Exception> List<T> filter(final T[] a, final int fromIndex, final int toIndex, final Try.Predicate<? super T, E> filter)
            throws E {
        return filter(a, fromIndex, toIndex, filter, Integer.MAX_VALUE);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max
     * @return
     */
    public static <T, E extends Exception> List<T> filter(final T[] a, final int fromIndex, final int toIndex, final Try.Predicate<? super T, E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final List<T> result = new ArrayList<>(min(9, max, (toIndex - fromIndex)));

        for (int i = fromIndex, cnt = 0; i < toIndex && cnt < max; i++) {
            if (filter.test(a[i])) {
                result.add(a[i]);
                cnt++;
            }
        }

        return result;
    }

    public static <T, E extends Exception> List<T> filter(final Collection<? extends T> c, final Try.Predicate<? super T, E> filter) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(c)) {
            return new ArrayList<>();
        }

        return filter(c, filter, Integer.MAX_VALUE);
    }

    public static <T, E extends Exception> List<T> filter(final Collection<? extends T> c, final Try.Predicate<? super T, E> filter, final int max) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(c)) {
            return new ArrayList<>();
        }

        return filter(c, 0, c.size(), filter, max);
    }

    public static <T, E extends Exception> List<T> filter(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.Predicate<? super T, E> filter) throws E {
        return filter(c, fromIndex, toIndex, filter, Integer.MAX_VALUE);
    }

    public static <T, E extends Exception> List<T> filter(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.Predicate<? super T, E> filter, final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return new ArrayList<>();
        }

        final List<T> result = new ArrayList<>(min(9, max, (toIndex - fromIndex)));

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;
            T e = null;

            for (int i = fromIndex, cnt = 0; i < toIndex && cnt < max; i++) {
                e = list.get(i);

                if (filter.test(e)) {
                    result.add(e);
                    cnt++;
                }
            }
        } else {
            int idx = 0;
            int cnt = 0;
            for (T e : c) {
                if (cnt >= max) {
                    break;
                }

                if (idx++ < fromIndex) {
                    continue;
                }

                if (filter.test(e)) {
                    result.add(e);
                    cnt++;
                }

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return result;
    }

    public static <T, R extends Collection<T>, E extends Exception> R filter(final T[] a, final Try.Predicate<? super T, E> filter,
            final IntFunction<R> supplier) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return supplier.apply(0);
        }

        return filter(a, filter, Integer.MAX_VALUE, supplier);
    }

    public static <T, R extends Collection<T>, E extends Exception> R filter(final T[] a, final Try.Predicate<? super T, E> filter, final int max,
            final IntFunction<R> supplier) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return supplier.apply(0);
        }

        return filter(a, 0, a.length, filter, max, supplier);
    }

    public static <T, R extends Collection<T>, E extends Exception> R filter(final T[] a, final int fromIndex, final int toIndex,
            final Try.Predicate<? super T, E> filter, final IntFunction<R> supplier) throws E {
        return filter(a, fromIndex, toIndex, filter, Integer.MAX_VALUE, supplier);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max
     * @param supplier
     * @return
     */
    public static <T, R extends Collection<T>, E extends Exception> R filter(final T[] a, final int fromIndex, final int toIndex,
            final Try.Predicate<? super T, E> filter, final int max, final IntFunction<R> supplier) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return supplier.apply(0);
        }

        final R result = supplier.apply(N.min(9, max, (toIndex - fromIndex)));

        for (int i = fromIndex, cnt = 0; i < toIndex && cnt < max; i++) {
            if (filter.test(a[i])) {
                result.add(a[i]);
                cnt++;
            }
        }

        return result;
    }

    public static <T, R extends Collection<T>, E extends Exception> R filter(final Collection<? extends T> c, final Try.Predicate<? super T, E> filter,
            final IntFunction<R> supplier) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(c)) {
            return supplier.apply(0);
        }

        return filter(c, filter, Integer.MAX_VALUE, supplier);
    }

    public static <T, R extends Collection<T>, E extends Exception> R filter(final Collection<? extends T> c, final Try.Predicate<? super T, E> filter,
            final int max, final IntFunction<R> supplier) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(c)) {
            return supplier.apply(0);
        }

        return filter(c, 0, c.size(), filter, max, supplier);
    }

    public static <T, R extends Collection<T>, E extends Exception> R filter(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.Predicate<? super T, E> filter, final IntFunction<R> supplier) throws E {
        return filter(c, fromIndex, toIndex, filter, Integer.MAX_VALUE, supplier);
    }

    public static <T, R extends Collection<T>, E extends Exception> R filter(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.Predicate<? super T, E> filter, final int max, final IntFunction<R> supplier) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return supplier.apply(0);
        }

        final R result = supplier.apply(N.min(9, max, (toIndex - fromIndex)));

        if ((N.isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) || (fromIndex == toIndex && fromIndex < c.size())) {
            return result;
        }

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;
            T e = null;

            for (int i = fromIndex, cnt = 0; i < toIndex && cnt < max; i++) {
                e = list.get(i);

                if (filter.test(e)) {
                    result.add(e);
                    cnt++;
                }
            }
        } else {
            int idx = 0;
            int cnt = 0;
            for (T e : c) {
                if (cnt >= max) {
                    break;
                }

                if (idx++ < fromIndex) {
                    continue;
                }

                if (filter.test(e)) {
                    result.add(e);
                    cnt++;
                }

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return result;
    }

    public static <T, E extends Exception> BooleanList mapToBoolean(final T[] a, final Try.ToBooleanFunction<? super T, E> func) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return new BooleanList();
        }

        return mapToBoolean(a, 0, a.length, func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     */
    public static <T, E extends Exception> BooleanList mapToBoolean(final T[] a, final int fromIndex, final int toIndex,
            final Try.ToBooleanFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return new BooleanList();
        }

        final BooleanList result = new BooleanList(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(func.applyAsBoolean(a[i]));
        }

        return result;
    }

    public static <T, E extends Exception> BooleanList mapToBoolean(final Collection<? extends T> c, final Try.ToBooleanFunction<? super T, E> func) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c)) {
            return new BooleanList();
        }

        return mapToBoolean(c, 0, c.size(), func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     */
    public static <T, E extends Exception> BooleanList mapToBoolean(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.ToBooleanFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return new BooleanList();
        }

        final BooleanList result = new BooleanList(toIndex - fromIndex);

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(func.applyAsBoolean(list.get(i)));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                result.add(func.applyAsBoolean(e));

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return result;
    }

    public static <T, E extends Exception> CharList mapToChar(final T[] a, final Try.ToCharFunction<? super T, E> func) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return new CharList();
        }

        return mapToChar(a, 0, a.length, func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     */
    public static <T, E extends Exception> CharList mapToChar(final T[] a, final int fromIndex, final int toIndex, final Try.ToCharFunction<? super T, E> func)
            throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return new CharList();
        }

        final CharList result = new CharList(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(func.applyAsChar(a[i]));
        }

        return result;
    }

    public static <T, E extends Exception> CharList mapToChar(final Collection<? extends T> c, final Try.ToCharFunction<? super T, E> func) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c)) {
            return new CharList();
        }

        return mapToChar(c, 0, c.size(), func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     */
    public static <T, E extends Exception> CharList mapToChar(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.ToCharFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return new CharList();
        }

        final CharList result = new CharList(toIndex - fromIndex);

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(func.applyAsChar(list.get(i)));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                result.add(func.applyAsChar(e));

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return result;
    }

    public static <T, E extends Exception> ByteList mapToByte(final T[] a, final Try.ToByteFunction<? super T, E> func) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return new ByteList();
        }

        return mapToByte(a, 0, a.length, func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     */
    public static <T, E extends Exception> ByteList mapToByte(final T[] a, final int fromIndex, final int toIndex, final Try.ToByteFunction<? super T, E> func)
            throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return new ByteList();
        }

        final ByteList result = new ByteList(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(func.applyAsByte(a[i]));
        }

        return result;
    }

    public static <T, E extends Exception> ByteList mapToByte(final Collection<? extends T> c, final Try.ToByteFunction<? super T, E> func) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c)) {
            return new ByteList();
        }

        return mapToByte(c, 0, c.size(), func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     */
    public static <T, E extends Exception> ByteList mapToByte(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.ToByteFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return new ByteList();
        }

        final ByteList result = new ByteList(toIndex - fromIndex);

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(func.applyAsByte(list.get(i)));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                result.add(func.applyAsByte(e));

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return result;
    }

    public static <T, E extends Exception> ShortList mapToShort(final T[] a, final Try.ToShortFunction<? super T, E> func) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return new ShortList();
        }

        return mapToShort(a, 0, a.length, func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     */
    public static <T, E extends Exception> ShortList mapToShort(final T[] a, final int fromIndex, final int toIndex,
            final Try.ToShortFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return new ShortList();
        }

        final ShortList result = new ShortList(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(func.applyAsShort(a[i]));
        }

        return result;
    }

    public static <T, E extends Exception> ShortList mapToShort(final Collection<? extends T> c, final Try.ToShortFunction<? super T, E> func) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c)) {
            return new ShortList();
        }

        return mapToShort(c, 0, c.size(), func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     */
    public static <T, E extends Exception> ShortList mapToShort(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.ToShortFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return new ShortList();
        }

        final ShortList result = new ShortList(toIndex - fromIndex);

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(func.applyAsShort(list.get(i)));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                result.add(func.applyAsShort(e));

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return result;
    }

    public static <T, E extends Exception> IntList mapToInt(final T[] a, final Try.ToIntFunction<? super T, E> func) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return new IntList();
        }

        return mapToInt(a, 0, a.length, func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     */
    public static <T, E extends Exception> IntList mapToInt(final T[] a, final int fromIndex, final int toIndex, final Try.ToIntFunction<? super T, E> func)
            throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return new IntList();
        }

        final IntList result = new IntList(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(func.applyAsInt(a[i]));
        }

        return result;
    }

    public static <T, E extends Exception> IntList mapToInt(final Collection<? extends T> c, final Try.ToIntFunction<? super T, E> func) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c)) {
            return new IntList();
        }

        return mapToInt(c, 0, c.size(), func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     */
    public static <T, E extends Exception> IntList mapToInt(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.ToIntFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return new IntList();
        }

        final IntList result = new IntList(toIndex - fromIndex);

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(func.applyAsInt(list.get(i)));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                result.add(func.applyAsInt(e));

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return result;
    }

    public static <T, E extends Exception> LongList mapToLong(final T[] a, final Try.ToLongFunction<? super T, E> func) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return new LongList();
        }

        return mapToLong(a, 0, a.length, func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     */
    public static <T, E extends Exception> LongList mapToLong(final T[] a, final int fromIndex, final int toIndex, final Try.ToLongFunction<? super T, E> func)
            throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return new LongList();
        }

        final LongList result = new LongList(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(func.applyAsLong(a[i]));
        }

        return result;
    }

    public static <T, E extends Exception> LongList mapToLong(final Collection<? extends T> c, final Try.ToLongFunction<? super T, E> func) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c)) {
            return new LongList();
        }

        return mapToLong(c, 0, c.size(), func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     */
    public static <T, E extends Exception> LongList mapToLong(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.ToLongFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return new LongList();
        }

        final LongList result = new LongList(toIndex - fromIndex);

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(func.applyAsLong(list.get(i)));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                result.add(func.applyAsLong(e));

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return result;
    }

    public static <T, E extends Exception> FloatList mapToFloat(final T[] a, final Try.ToFloatFunction<? super T, E> func) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return new FloatList();
        }

        return mapToFloat(a, 0, a.length, func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     */
    public static <T, E extends Exception> FloatList mapToFloat(final T[] a, final int fromIndex, final int toIndex,
            final Try.ToFloatFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return new FloatList();
        }

        final FloatList result = new FloatList(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(func.applyAsFloat(a[i]));
        }

        return result;
    }

    public static <T, E extends Exception> FloatList mapToFloat(final Collection<? extends T> c, final Try.ToFloatFunction<? super T, E> func) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c)) {
            return new FloatList();
        }

        return mapToFloat(c, 0, c.size(), func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     */
    public static <T, E extends Exception> FloatList mapToFloat(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.ToFloatFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return new FloatList();
        }

        final FloatList result = new FloatList(toIndex - fromIndex);

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(func.applyAsFloat(list.get(i)));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                result.add(func.applyAsFloat(e));

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return result;
    }

    public static <T, E extends Exception> DoubleList mapToDouble(final T[] a, final Try.ToDoubleFunction<? super T, E> func) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return new DoubleList();
        }

        return mapToDouble(a, 0, a.length, func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     */
    public static <T, E extends Exception> DoubleList mapToDouble(final T[] a, final int fromIndex, final int toIndex,
            final Try.ToDoubleFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return new DoubleList();
        }

        final DoubleList result = new DoubleList(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(func.applyAsDouble(a[i]));
        }

        return result;
    }

    public static <T, E extends Exception> DoubleList mapToDouble(final Collection<? extends T> c, final Try.ToDoubleFunction<? super T, E> func) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c)) {
            return new DoubleList();
        }

        return mapToDouble(c, 0, c.size(), func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     */
    public static <T, E extends Exception> DoubleList mapToDouble(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.ToDoubleFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return new DoubleList();
        }

        final DoubleList result = new DoubleList(toIndex - fromIndex);

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(func.applyAsDouble(list.get(i)));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                result.add(func.applyAsDouble(e));

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return result;
    }

    public static <T, R, E extends Exception> List<R> map(final T[] a, final Try.Function<? super T, ? extends R, E> func) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        return map(a, 0, a.length, func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     */
    public static <T, R, E extends Exception> List<R> map(final T[] a, final int fromIndex, final int toIndex,
            final Try.Function<? super T, ? extends R, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final List<R> result = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(func.apply(a[i]));
        }

        return result;
    }

    public static <T, R, E extends Exception> List<R> map(final Collection<? extends T> c, final Try.Function<? super T, ? extends R, E> func) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c)) {
            return new ArrayList<>();
        }

        return map(c, 0, c.size(), func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     */
    public static <T, R, E extends Exception> List<R> map(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.Function<? super T, ? extends R, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return new ArrayList<>();
        }

        final List<R> result = new ArrayList<>(toIndex - fromIndex);

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(func.apply(list.get(i)));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                result.add(func.apply(e));

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return result;
    }

    public static <T, R, C extends Collection<R>, E extends Exception> C map(final T[] a, final Try.Function<? super T, ? extends R, E> func,
            final IntFunction<? extends C> supplier) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return supplier.apply(0);
        }

        return map(a, 0, a.length, func, supplier);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @param max
     * @param supplier
     * @return
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C map(final T[] a, final int fromIndex, final int toIndex,
            final Try.Function<? super T, ? extends R, E> func, final IntFunction<? extends C> supplier) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(func.apply(a[i]));
        }

        return result;
    }

    public static <T, R, C extends Collection<R>, E extends Exception> C map(final Collection<? extends T> c,
            final Try.Function<? super T, ? extends R, E> func, final IntFunction<? extends C> supplier) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c)) {
            return supplier.apply(0);
        }

        return map(c, 0, c.size(), func, supplier);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @param max
     * @param supplier
     * @return
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C map(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.Function<? super T, ? extends R, E> func, final IntFunction<? extends C> supplier) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(func.apply(list.get(i)));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                result.add(func.apply(e));

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return result;
    }

    public static <T, R, E extends Exception> List<R> flatMap(final T[] a, final Try.Function<? super T, ? extends Collection<? extends R>, E> func) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        return flatMap(a, 0, a.length, func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     */
    public static <T, R, E extends Exception> List<R> flatMap(final T[] a, final int fromIndex, final int toIndex,
            final Try.Function<? super T, ? extends Collection<? extends R>, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final List<R> result = new ArrayList<>(toIndex - fromIndex);
        Collection<? extends R> mr = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (N.notNullOrEmpty(mr = func.apply(a[i]))) {
                result.addAll(mr);
            }
        }

        return result;
    }

    public static <T, R, E extends Exception> List<R> flatMap(final Collection<? extends T> c,
            final Try.Function<? super T, ? extends Collection<? extends R>, E> func) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c)) {
            return new ArrayList<>();
        }

        return flatMap(c, 0, c.size(), func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     */
    public static <T, R, E extends Exception> List<R> flatMap(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.Function<? super T, ? extends Collection<? extends R>, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return new ArrayList<>();
        }

        final List<R> result = new ArrayList<>(toIndex - fromIndex);
        Collection<? extends R> mr = null;

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                if (N.notNullOrEmpty(mr = func.apply(list.get(i)))) {
                    result.addAll(mr);
                }
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }
                if (N.notNullOrEmpty(mr = func.apply(e))) {
                    result.addAll(mr);
                }

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return result;
    }

    public static <T, R, C extends Collection<R>, E extends Exception> C flatMap(final T[] a,
            final Try.Function<? super T, ? extends Collection<? extends R>, E> func, final IntFunction<? extends C> supplier) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return supplier.apply(0);
        }

        return flatMap(a, 0, a.length, func, supplier);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @param mr  x
     * @param supplier
     * @return
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C flatMap(final T[] a, final int fromIndex, final int toIndex,
            final Try.Function<? super T, ? extends Collection<? extends R>, E> func, final IntFunction<? extends C> supplier) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);
        Collection<? extends R> mr = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (N.notNullOrEmpty(mr = func.apply(a[i]))) {
                result.addAll(mr);
            }
        }

        return result;
    }

    public static <T, R, C extends Collection<R>, E extends Exception> C flatMap(final Collection<? extends T> c,
            final Try.Function<? super T, ? extends Collection<? extends R>, E> func, final IntFunction<? extends C> supplier) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c)) {
            return supplier.apply(0);
        }

        return flatMap(c, 0, c.size(), func, supplier);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @param mr  x
     * @param supplier
     * @return
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C flatMap(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.Function<? super T, ? extends Collection<? extends R>, E> func, final IntFunction<? extends C> supplier) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);
        Collection<? extends R> mr = null;

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                if (N.notNullOrEmpty(mr = func.apply(list.get(i)))) {
                    result.addAll(mr);
                }
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                if (N.notNullOrEmpty(mr = func.apply(e))) {
                    result.addAll(mr);
                }

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return result;
    }

    public static <T, T2, R, C extends Collection<R>, E extends Exception, E2 extends Exception> List<R> flatMap(final T[] a,
            final Try.Function<? super T, ? extends Collection<? extends T2>, E> func,
            final Try.Function<? super T2, ? extends Collection<? extends R>, E2> func2) throws E, E2 {

        return flatMap(a, func, func2, Factory.<R> ofList());
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param func
     * @param func2
     * @param supplier
     * @return
     * @throws E
     * @throws E2
     */
    public static <T, T2, R, C extends Collection<R>, E extends Exception, E2 extends Exception> C flatMap(final T[] a,
            final Try.Function<? super T, ? extends Collection<? extends T2>, E> func,
            final Try.Function<? super T2, ? extends Collection<? extends R>, E2> func2, final IntFunction<? extends C> supplier) throws E, E2 {
        N.checkArgNotNull(func);
        N.checkArgNotNull(func2);

        if (N.isNullOrEmpty(a)) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(a.length);

        for (T e : a) {
            final Collection<? extends T2> c1 = func.apply(e);

            if (N.notNullOrEmpty(c1)) {
                for (T2 e2 : c1) {
                    final Collection<? extends R> c2 = func2.apply(e2);

                    if (N.notNullOrEmpty(c2)) {
                        result.addAll(c2);
                    }
                }
            }
        }

        return result;
    }

    public static <T, T2, R, C extends Collection<R>, E extends Exception, E2 extends Exception> List<R> flatMap(final Collection<? extends T> c,
            final Try.Function<? super T, ? extends Collection<? extends T2>, E> func,
            final Try.Function<? super T2, ? extends Collection<? extends R>, E2> func2) throws E, E2 {

        return flatMap(c, func, func2, Factory.<R> ofList());
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param func
     * @param func2
     * @param supplier
     * @return
     * @throws E
     * @throws E2
     */
    public static <T, T2, R, C extends Collection<R>, E extends Exception, E2 extends Exception> C flatMap(final Collection<? extends T> c,
            final Try.Function<? super T, ? extends Collection<? extends T2>, E> func,
            final Try.Function<? super T2, ? extends Collection<? extends R>, E2> func2, final IntFunction<? extends C> supplier) throws E, E2 {
        N.checkArgNotNull(func);
        N.checkArgNotNull(func2);

        if (N.isNullOrEmpty(c)) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(c.size());

        for (T e : c) {
            final Collection<? extends T2> c1 = func.apply(e);

            if (N.notNullOrEmpty(c1)) {
                for (T2 e2 : c1) {
                    final Collection<? extends R> c2 = func2.apply(e2);

                    if (N.notNullOrEmpty(c2)) {
                        result.addAll(c2);
                    }
                }
            }
        }

        return result;
    }

    public static <T, R, E extends Exception> List<R> flattMap(final T[] a, final Try.Function<? super T, ? extends R[], E> func) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        return flattMap(a, 0, a.length, func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     */
    public static <T, R, E extends Exception> List<R> flattMap(final T[] a, final int fromIndex, final int toIndex,
            final Try.Function<? super T, ? extends R[], E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final List<R> result = new ArrayList<>(toIndex - fromIndex);
        R[] mr = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (N.notNullOrEmpty(mr = func.apply(a[i]))) {
                result.addAll(Arrays.asList(mr));
            }
        }

        return result;
    }

    public static <T, R, E extends Exception> List<R> flattMap(final Collection<? extends T> c, final Try.Function<? super T, ? extends R[], E> func) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c)) {
            return new ArrayList<>();
        }

        return flattMap(c, 0, c.size(), func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     */
    public static <T, R, E extends Exception> List<R> flattMap(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.Function<? super T, ? extends R[], E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return new ArrayList<>();
        }

        final List<R> result = new ArrayList<>(toIndex - fromIndex);
        R[] mr = null;

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                if (N.notNullOrEmpty(mr = func.apply(list.get(i)))) {
                    result.addAll(Arrays.asList(mr));
                }
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }
                if (N.notNullOrEmpty(mr = func.apply(e))) {
                    result.addAll(Arrays.asList(mr));
                }

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return result;
    }

    public static <T, R, C extends Collection<R>, E extends Exception> C flattMap(final T[] a, final Try.Function<? super T, ? extends R[], E> func,
            final IntFunction<? extends C> supplier) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return supplier.apply(0);
        }

        return flattMap(a, 0, a.length, func, supplier);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @param mr  x
     * @param supplier
     * @return
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C flattMap(final T[] a, final int fromIndex, final int toIndex,
            final Try.Function<? super T, ? extends R[], E> func, final IntFunction<? extends C> supplier) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(a)) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);
        R[] mr = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (N.notNullOrEmpty(mr = func.apply(a[i]))) {
                result.addAll(Arrays.asList(mr));
            }
        }

        return result;
    }

    public static <T, R, C extends Collection<R>, E extends Exception> C flattMap(final Collection<? extends T> c,
            final Try.Function<? super T, ? extends R[], E> func, final IntFunction<? extends C> supplier) throws E {
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c)) {
            return supplier.apply(0);
        }

        return flattMap(c, 0, c.size(), func, supplier);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @param mr  x
     * @param supplier
     * @return
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C flattMap(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.Function<? super T, ? extends R[], E> func, final IntFunction<? extends C> supplier) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        N.checkArgNotNull(func);

        if (N.isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);
        R[] mr = null;

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                if (N.notNullOrEmpty(mr = func.apply(list.get(i)))) {
                    result.addAll(Arrays.asList(mr));
                }
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                if (N.notNullOrEmpty(mr = func.apply(e))) {
                    result.addAll(Arrays.asList(mr));
                }

                if (idx >= toIndex) {
                    break;
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
    public static <T extends Number> int sumInt(final T[] a) {
        return N.sumInt(a, Fn.numToInt());
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> int sumInt(final T[] a, final int fromIndex, final int toIndex) {
        return N.sumInt(a, fromIndex, toIndex, Fn.numToInt());
    }

    public static <T, E extends Exception> int sumInt(final T[] a, final Try.ToIntFunction<? super T, E> func) throws E {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        return sumInt(a, 0, a.length, func);
    }

    public static <T, E extends Exception> int sumInt(final T[] a, final int fromIndex, final int toIndex, final Try.ToIntFunction<? super T, E> func)
            throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return 0;
        }

        long result = 0;

        for (int i = fromIndex; i < toIndex; i++) {
            result += func.applyAsInt(a[i]);
        }

        return N.toIntExact(result);
    }

    /**
     *
     * @param c
     * @return
     */
    public static <T extends Number> int sumInt(final Collection<? extends T> c) {
        return N.sumInt(c, Fn.numToInt());
    }

    /**
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> int sumInt(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        return N.sumInt(c, fromIndex, toIndex, Fn.numToInt());
    }

    public static <T, E extends Exception> int sumInt(final Collection<? extends T> c, final Try.ToIntFunction<? super T, E> func) throws E {
        if (N.isNullOrEmpty(c)) {
            return 0;
        }

        long result = 0;

        for (T e : c) {
            result += func.applyAsInt(e);
        }

        return N.toIntExact(result);
    }

    public static <T, E extends Exception> int sumInt(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.ToIntFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (fromIndex == toIndex) {
            return 0;
        }

        long result = 0;

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                result += func.applyAsInt(list.get(i));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                result += func.applyAsInt(e);

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return N.toIntExact(result);
    }

    /**
     *
     * @param a
     * @return
     */
    public static <T extends Number> long sumLong(final T[] a) {
        return N.sumLong(a, Fn.numToLong());
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> long sumLong(final T[] a, final int fromIndex, final int toIndex) {
        return N.sumLong(a, fromIndex, toIndex, Fn.numToLong());
    }

    public static <T, E extends Exception> long sumLong(final T[] a, final Try.ToLongFunction<? super T, E> func) throws E {
        if (N.isNullOrEmpty(a)) {
            return 0L;
        }

        return sumLong(a, 0, a.length, func);
    }

    public static <T, E extends Exception> long sumLong(final T[] a, final int fromIndex, final int toIndex, final Try.ToLongFunction<? super T, E> func)
            throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return 0L;
        }

        long result = 0;

        for (int i = fromIndex; i < toIndex; i++) {
            result += func.applyAsLong(a[i]);
        }

        return result;
    }

    /**
     *
     * @param c
     * @return
     */
    public static <T extends Number> long sumLong(final Collection<? extends T> c) {
        return N.sumLong(c, Fn.numToLong());
    }

    /**
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> long sumLong(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        return N.sumLong(c, fromIndex, toIndex, Fn.numToLong());
    }

    public static <T, E extends Exception> long sumLong(final Collection<? extends T> c, final Try.ToLongFunction<? super T, E> func) throws E {
        if (N.isNullOrEmpty(c)) {
            return 0L;
        }

        long result = 0;

        for (T e : c) {
            result += func.applyAsLong(e);
        }

        return result;
    }

    public static <T, E extends Exception> long sumLong(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.ToLongFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (fromIndex == toIndex) {
            return 0L;
        }

        long result = 0;

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                result += func.applyAsLong(list.get(i));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                result += func.applyAsLong(e);

                if (idx >= toIndex) {
                    break;
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
    public static <T extends Number> double sumDouble(final T[] a) {
        return N.sumDouble(a, Fn.numToDouble());
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> double sumDouble(final T[] a, final int fromIndex, final int toIndex) {
        return N.sumDouble(a, fromIndex, toIndex, Fn.numToDouble());
    }

    public static <T, E extends Exception> double sumDouble(final T[] a, final Try.ToDoubleFunction<? super T, E> func) throws E {
        if (N.isNullOrEmpty(a)) {
            return 0D;
        }

        return sumDouble(a, 0, a.length, func);
    }

    public static <T, E extends Exception> double sumDouble(final T[] a, final int fromIndex, final int toIndex, final Try.ToDoubleFunction<? super T, E> func)
            throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return 0D;
        }

        final KahanSummation summation = new KahanSummation();

        for (int i = fromIndex; i < toIndex; i++) {
            summation.add(func.applyAsDouble(a[i]));
        }

        return summation.sum();
    }

    /**
     *
     * @param c
     * @return
     */
    public static <T extends Number> double sumDouble(final Collection<? extends T> c) {
        return N.sumDouble(c, Fn.numToDouble());
    }

    /**
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> double sumDouble(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        return N.sumDouble(c, fromIndex, toIndex, Fn.numToDouble());
    }

    public static <T, E extends Exception> double sumDouble(final Collection<? extends T> c, final Try.ToDoubleFunction<? super T, E> func) throws E {
        if (N.isNullOrEmpty(c)) {
            return 0D;
        }

        final KahanSummation summation = new KahanSummation();

        for (T e : c) {
            summation.add(func.applyAsDouble(e));
        }

        return summation.sum();
    }

    public static <T, E extends Exception> double sumDouble(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.ToDoubleFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (fromIndex == toIndex) {
            return 0D;
        }

        final KahanSummation summation = new KahanSummation();

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                summation.add(func.applyAsDouble(list.get(i)));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                summation.add(func.applyAsDouble(e));

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return summation.sum();
    }

    /**
     *
     * @param a
     * @return
     */
    public static <T extends Number> OptionalDouble averageInt(final T[] a) {
        return N.averageInt(a, Fn.numToInt());
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> OptionalDouble averageInt(final T[] a, final int fromIndex, final int toIndex) {
        return N.averageInt(a, fromIndex, toIndex, Fn.numToInt());
    }

    public static <T, E extends Exception> OptionalDouble averageInt(final T[] a, final Try.ToIntFunction<? super T, E> func) throws E {
        if (N.isNullOrEmpty(a)) {
            return OptionalDouble.empty();
        }

        return averageInt(a, 0, a.length, func);
    }

    public static <T, E extends Exception> OptionalDouble averageInt(final T[] a, final int fromIndex, final int toIndex,
            final Try.ToIntFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return OptionalDouble.empty();
        }

        long sum = 0;

        for (int i = fromIndex; i < toIndex; i++) {
            sum += func.applyAsInt(a[i]);
        }

        return OptionalDouble.of(((double) sum) / (toIndex - fromIndex));
    }

    /**
     *
     * @param c
     * @return
     */
    public static <T extends Number> OptionalDouble averageInt(final Collection<? extends T> c) {
        return N.averageInt(c, Fn.numToInt());
    }

    /**
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> OptionalDouble averageInt(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        return N.averageInt(c, fromIndex, toIndex, Fn.numToInt());
    }

    public static <T, E extends Exception> OptionalDouble averageInt(final Collection<? extends T> c, final Try.ToIntFunction<? super T, E> func) throws E {
        if (N.isNullOrEmpty(c)) {
            return OptionalDouble.empty();
        }

        long sum = 0;

        for (T e : c) {
            sum += func.applyAsInt(e);
        }

        return OptionalDouble.of(((double) sum) / c.size());
    }

    public static <T, E extends Exception> OptionalDouble averageInt(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.ToIntFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (fromIndex == toIndex) {
            return OptionalDouble.empty();
        }

        long sum = 0;

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                sum += func.applyAsInt(list.get(i));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                sum += func.applyAsInt(e);

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return OptionalDouble.of(((double) sum) / (toIndex - fromIndex));
    }

    /**
     *
     * @param a
     * @return
     */
    public static <T extends Number> OptionalDouble averageLong(final T[] a) {
        return N.averageLong(a, Fn.numToLong());
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> OptionalDouble averageLong(final T[] a, final int fromIndex, final int toIndex) {
        return N.averageLong(a, fromIndex, toIndex, Fn.numToLong());
    }

    public static <T, E extends Exception> OptionalDouble averageLong(final T[] a, final Try.ToLongFunction<? super T, E> func) throws E {
        if (N.isNullOrEmpty(a)) {
            return OptionalDouble.empty();
        }

        return averageLong(a, 0, a.length, func);
    }

    public static <T, E extends Exception> OptionalDouble averageLong(final T[] a, final int fromIndex, final int toIndex,
            final Try.ToLongFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(((double) sumLong(a, fromIndex, toIndex, func)) / (toIndex - fromIndex));
    }

    /**
     *
     * @param c
     * @return
     */
    public static <T extends Number> OptionalDouble averageLong(final Collection<? extends T> c) {
        return N.averageLong(c, Fn.numToLong());
    }

    /**
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> OptionalDouble averageLong(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        return N.averageLong(c, fromIndex, toIndex, Fn.numToLong());
    }

    public static <T, E extends Exception> OptionalDouble averageLong(final Collection<? extends T> c, final Try.ToLongFunction<? super T, E> func) throws E {
        if (N.isNullOrEmpty(c)) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(((double) sumLong(c, func)) / c.size());
    }

    public static <T, E extends Exception> OptionalDouble averageLong(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.ToLongFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (fromIndex == toIndex) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(((double) sumLong(c, fromIndex, toIndex, func)) / (toIndex - fromIndex));
    }

    /**
     *
     * @param a
     * @return
     */
    public static <T extends Number> OptionalDouble averageDouble(final T[] a) {
        return N.averageDouble(a, Fn.numToDouble());
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> OptionalDouble averageDouble(final T[] a, final int fromIndex, final int toIndex) {
        return N.averageDouble(a, fromIndex, toIndex, Fn.numToDouble());
    }

    public static <T, E extends Exception> OptionalDouble averageDouble(final T[] a, final Try.ToDoubleFunction<? super T, E> func) throws E {
        if (N.isNullOrEmpty(a)) {
            return OptionalDouble.empty();
        }

        return averageDouble(a, 0, a.length, func);
    }

    public static <T, E extends Exception> OptionalDouble averageDouble(final T[] a, final int fromIndex, final int toIndex,
            final Try.ToDoubleFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return OptionalDouble.empty();
        }

        final KahanSummation summation = new KahanSummation();

        for (int i = fromIndex; i < toIndex; i++) {
            summation.add(func.applyAsDouble(a[i]));
        }

        return summation.average();
    }

    /**
     *
     * @param c
     * @return
     */
    public static <T extends Number> OptionalDouble averageDouble(final Collection<? extends T> c) {
        return N.averageDouble(c, Fn.numToDouble());
    }

    /**
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> OptionalDouble averageDouble(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        return N.averageDouble(c, fromIndex, toIndex, Fn.numToDouble());
    }

    public static <T, E extends Exception> OptionalDouble averageDouble(final Collection<? extends T> c, final Try.ToDoubleFunction<? super T, E> func)
            throws E {
        if (N.isNullOrEmpty(c)) {
            return OptionalDouble.empty();
        }

        final KahanSummation summation = new KahanSummation();

        for (T e : c) {
            summation.add(func.applyAsDouble(e));
        }

        return summation.average();
    }

    public static <T, E extends Exception> OptionalDouble averageDouble(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.ToDoubleFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (fromIndex == toIndex) {
            return OptionalDouble.empty();
        }

        final KahanSummation summation = new KahanSummation();

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                summation.add(func.applyAsDouble(list.get(i)));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                summation.add(func.applyAsDouble(e));

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return summation.average();
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param filter
     * @return
     */
    public static <E extends Exception> int count(final boolean[] a, final Try.BooleanPredicate<E> filter) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        return count(a, 0, a.length, filter);
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     */
    public static <E extends Exception> int count(final boolean[] a, final int fromIndex, final int toIndex, final Try.BooleanPredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int count = 0;

        for (int i = fromIndex; i < toIndex; i++) {
            if (filter.test(a[i])) {
                count++;
            }
        }

        return count;
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param filter
     * @return
     */
    public static <E extends Exception> int count(final char[] a, final Try.CharPredicate<E> filter) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        return count(a, 0, a.length, filter);
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     */
    public static <E extends Exception> int count(final char[] a, final int fromIndex, final int toIndex, final Try.CharPredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int count = 0;

        for (int i = fromIndex; i < toIndex; i++) {
            if (filter.test(a[i])) {
                count++;
            }
        }

        return count;
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param filter
     * @return
     */
    public static <E extends Exception> int count(final byte[] a, final Try.BytePredicate<E> filter) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        return count(a, 0, a.length, filter);
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     */
    public static <E extends Exception> int count(final byte[] a, final int fromIndex, final int toIndex, final Try.BytePredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int count = 0;

        for (int i = fromIndex; i < toIndex; i++) {
            if (filter.test(a[i])) {
                count++;
            }
        }

        return count;
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param filter
     * @return
     */
    public static <E extends Exception> int count(final short[] a, final Try.ShortPredicate<E> filter) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        return count(a, 0, a.length, filter);
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     */
    public static <E extends Exception> int count(final short[] a, final int fromIndex, final int toIndex, final Try.ShortPredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int count = 0;

        for (int i = fromIndex; i < toIndex; i++) {
            if (filter.test(a[i])) {
                count++;
            }
        }

        return count;
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param filter
     * @return
     */
    public static <E extends Exception> int count(final int[] a, final Try.IntPredicate<E> filter) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        return count(a, 0, a.length, filter);
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     */
    public static <E extends Exception> int count(final int[] a, final int fromIndex, final int toIndex, final Try.IntPredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int count = 0;

        for (int i = fromIndex; i < toIndex; i++) {
            if (filter.test(a[i])) {
                count++;
            }
        }

        return count;
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param filter
     * @return
     */
    public static <E extends Exception> int count(final long[] a, final Try.LongPredicate<E> filter) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        return count(a, 0, a.length, filter);
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     */
    public static <E extends Exception> int count(final long[] a, final int fromIndex, final int toIndex, final Try.LongPredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int count = 0;

        for (int i = fromIndex; i < toIndex; i++) {
            if (filter.test(a[i])) {
                count++;
            }
        }

        return count;
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param filter
     * @return
     */
    public static <E extends Exception> int count(final float[] a, final Try.FloatPredicate<E> filter) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        return count(a, 0, a.length, filter);
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     */
    public static <E extends Exception> int count(final float[] a, final int fromIndex, final int toIndex, final Try.FloatPredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int count = 0;

        for (int i = fromIndex; i < toIndex; i++) {
            if (filter.test(a[i])) {
                count++;
            }
        }

        return count;
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param filter
     * @return
     */
    public static <E extends Exception> int count(final double[] a, final Try.DoublePredicate<E> filter) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        return count(a, 0, a.length, filter);
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     */
    public static <E extends Exception> int count(final double[] a, final int fromIndex, final int toIndex, final Try.DoublePredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int count = 0;

        for (int i = fromIndex; i < toIndex; i++) {
            if (filter.test(a[i])) {
                count++;
            }
        }

        return count;
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param filter
     * @return
     */
    public static <T, E extends Exception> int count(final T[] a, final Try.Predicate<? super T, E> filter) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        return count(a, 0, a.length, filter);
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     */
    public static <T, E extends Exception> int count(final T[] a, final int fromIndex, final int toIndex, final Try.Predicate<? super T, E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int count = 0;

        for (int i = fromIndex; i < toIndex; i++) {
            if (filter.test(a[i])) {
                count++;
            }
        }

        return count;
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param c
     * @param filter
     * @return
     */
    public static <T, E extends Exception> int count(final Collection<? extends T> c, final Try.Predicate<? super T, E> filter) throws E {
        N.checkArgNotNull(filter);

        if (N.isNullOrEmpty(c)) {
            return 0;
        }

        return count(c, 0, c.size(), filter);
    }

    /**
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     */
    public static <T, E extends Exception> int count(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.Predicate<? super T, E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        N.checkArgNotNull(filter);

        if ((N.isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) || (fromIndex == toIndex && fromIndex < c.size())) {
            return 0;
        }

        int count = 0;

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                if (filter.test(list.get(i))) {
                    count++;
                }
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                if (filter.test(e)) {
                    count++;
                }

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return count;
    }

    public static short[] top(final short[] a, final int n) {
        return top(a, n, null);
    }

    public static short[] top(final short[] a, final int n, final Comparator<? super Short> cmp) {
        return top(a, 0, len(a), n, cmp);
    }

    public static short[] top(final short[] a, final int fromIndex, final int toIndex, final int n) {
        return top(a, fromIndex, toIndex, n, null);
    }

    public static short[] top(final short[] a, final int fromIndex, final int toIndex, final int n, final Comparator<? super Short> cmp) {
        N.checkArgNotNegative(n, "n");

        if (n == 0) {
            return N.EMPTY_SHORT_ARRAY;
        } else if (n >= toIndex - fromIndex) {
            return N.copyOfRange(a, fromIndex, toIndex);
        }

        final Comparator<? super Short> comparator = cmp == null ? Comparators.NATURAL_ORDER : cmp;
        final Queue<Short> heap = new PriorityQueue<>(n, comparator);

        for (int i = fromIndex; i < toIndex; i++) {
            if (heap.size() >= n) {
                if (comparator.compare(heap.peek(), a[i]) < 0) {
                    heap.poll();
                    heap.add(a[i]);
                }
            } else {
                heap.offer(a[i]);
            }
        }

        final Iterator<Short> iter = heap.iterator();
        final short[] res = new short[n];
        int idx = 0;

        while (iter.hasNext()) {
            res[idx++] = iter.next();
        }

        return res;
    }

    public static int[] top(final int[] a, final int n) {
        return top(a, n, null);
    }

    public static int[] top(final int[] a, final int n, final Comparator<? super Integer> cmp) {
        return top(a, 0, len(a), n, cmp);
    }

    public static int[] top(final int[] a, final int fromIndex, final int toIndex, final int n) {
        return top(a, fromIndex, toIndex, n, null);
    }

    public static int[] top(final int[] a, final int fromIndex, final int toIndex, final int n, final Comparator<? super Integer> cmp) {
        N.checkArgNotNegative(n, "n");

        if (n == 0) {
            return N.EMPTY_INT_ARRAY;
        } else if (n >= toIndex - fromIndex) {
            return N.copyOfRange(a, fromIndex, toIndex);
        }

        final Comparator<? super Integer> comparator = cmp == null ? Comparators.NATURAL_ORDER : cmp;
        final Queue<Integer> heap = new PriorityQueue<>(n, comparator);

        for (int i = fromIndex; i < toIndex; i++) {
            if (heap.size() >= n) {
                if (comparator.compare(heap.peek(), a[i]) < 0) {
                    heap.poll();
                    heap.add(a[i]);
                }
            } else {
                heap.offer(a[i]);
            }
        }

        final Iterator<Integer> iter = heap.iterator();
        final int[] res = new int[n];
        int idx = 0;

        while (iter.hasNext()) {
            res[idx++] = iter.next();
        }

        return res;
    }

    public static long[] top(final long[] a, final int n) {
        return top(a, n, null);
    }

    public static long[] top(final long[] a, final int n, final Comparator<? super Long> cmp) {
        return top(a, 0, len(a), n, cmp);
    }

    public static long[] top(final long[] a, final int fromIndex, final int toIndex, final int n) {
        return top(a, fromIndex, toIndex, n, null);
    }

    public static long[] top(final long[] a, final int fromIndex, final int toIndex, final int n, final Comparator<? super Long> cmp) {
        N.checkArgNotNegative(n, "n");

        if (n == 0) {
            return N.EMPTY_LONG_ARRAY;
        } else if (n >= toIndex - fromIndex) {
            return N.copyOfRange(a, fromIndex, toIndex);
        }

        final Comparator<? super Long> comparator = cmp == null ? Comparators.NATURAL_ORDER : cmp;
        final Queue<Long> heap = new PriorityQueue<>(n, comparator);

        for (int i = fromIndex; i < toIndex; i++) {
            if (heap.size() >= n) {
                if (comparator.compare(heap.peek(), a[i]) < 0) {
                    heap.poll();
                    heap.add(a[i]);
                }
            } else {
                heap.offer(a[i]);
            }
        }

        final Iterator<Long> iter = heap.iterator();
        final long[] res = new long[n];
        int idx = 0;

        while (iter.hasNext()) {
            res[idx++] = iter.next();
        }

        return res;
    }

    public static float[] top(final float[] a, final int n) {
        return top(a, n, null);
    }

    public static float[] top(final float[] a, final int n, final Comparator<? super Float> cmp) {
        return top(a, 0, len(a), n, cmp);
    }

    public static float[] top(final float[] a, final int fromIndex, final int toIndex, final int n) {
        return top(a, fromIndex, toIndex, n, null);
    }

    public static float[] top(final float[] a, final int fromIndex, final int toIndex, final int n, final Comparator<? super Float> cmp) {
        N.checkArgNotNegative(n, "n");

        if (n == 0) {
            return N.EMPTY_FLOAT_ARRAY;
        } else if (n >= toIndex - fromIndex) {
            return N.copyOfRange(a, fromIndex, toIndex);
        }

        final Comparator<? super Float> comparator = cmp == null ? Comparators.NATURAL_ORDER : cmp;
        final Queue<Float> heap = new PriorityQueue<>(n, comparator);

        for (int i = fromIndex; i < toIndex; i++) {
            if (heap.size() >= n) {
                if (comparator.compare(heap.peek(), a[i]) < 0) {
                    heap.poll();
                    heap.add(a[i]);
                }
            } else {
                heap.offer(a[i]);
            }
        }

        final Iterator<Float> iter = heap.iterator();
        final float[] res = new float[n];
        int idx = 0;

        while (iter.hasNext()) {
            res[idx++] = iter.next();
        }

        return res;
    }

    public static double[] top(final double[] a, final int n) {
        return top(a, n, null);
    }

    public static double[] top(final double[] a, final int n, final Comparator<? super Double> cmp) {
        return top(a, 0, len(a), n, cmp);
    }

    public static double[] top(final double[] a, final int fromIndex, final int toIndex, final int n) {
        return top(a, fromIndex, toIndex, n, null);
    }

    public static double[] top(final double[] a, final int fromIndex, final int toIndex, final int n, final Comparator<? super Double> cmp) {
        N.checkArgNotNegative(n, "n");

        if (n == 0) {
            return N.EMPTY_DOUBLE_ARRAY;
        } else if (n >= toIndex - fromIndex) {
            return N.copyOfRange(a, fromIndex, toIndex);
        }

        final Comparator<? super Double> comparator = cmp == null ? Comparators.NATURAL_ORDER : cmp;
        final Queue<Double> heap = new PriorityQueue<>(n, comparator);

        for (int i = fromIndex; i < toIndex; i++) {
            if (heap.size() >= n) {
                if (comparator.compare(heap.peek(), a[i]) < 0) {
                    heap.poll();
                    heap.add(a[i]);
                }
            } else {
                heap.offer(a[i]);
            }
        }

        final Iterator<Double> iter = heap.iterator();
        final double[] res = new double[n];
        int idx = 0;

        while (iter.hasNext()) {
            res[idx++] = iter.next();
        }

        return res;
    }

    public static <T extends Comparable<T>> List<T> top(final T[] a, final int n) {
        return top(a, n, N.NATURAL_ORDER);
    }

    public static <T> List<T> top(final T[] a, final int n, final Comparator<? super T> cmp) {
        return top(a, 0, len(a), n, cmp);
    }

    public static <T extends Comparable<T>> List<T> top(final T[] a, final int fromIndex, final int toIndex, final int n) {
        return top(a, fromIndex, toIndex, n, N.NATURAL_ORDER);
    }

    public static <T> List<T> top(final T[] a, final int fromIndex, final int toIndex, final int n, final Comparator<? super T> cmp) {
        N.checkArgNotNegative(n, "n");

        if (n == 0) {
            return new ArrayList<>();
        } else if (n >= toIndex - fromIndex) {
            return N.toList(a, fromIndex, toIndex);
        }

        final Comparator<? super T> comparator = cmp == null ? Comparators.NATURAL_ORDER : cmp;
        final Queue<T> heap = new PriorityQueue<>(n, comparator);

        for (int i = fromIndex; i < toIndex; i++) {
            if (heap.size() >= n) {
                if (comparator.compare(heap.peek(), a[i]) < 0) {
                    heap.poll();
                    heap.add(a[i]);
                }
            } else {
                heap.offer(a[i]);
            }
        }

        return createList((T[]) heap.toArray(N.EMPTY_OBJECT_ARRAY));
    }

    public static <T extends Comparable<T>> List<T> top(final Collection<? extends T> c, final int n) {
        return top(c, n, null);
    }

    public static <T> List<T> top(final Collection<? extends T> c, final int n, final Comparator<? super T> cmp) {
        return top(c, 0, size(c), n, cmp);
    }

    public static <T extends Comparable<T>> List<T> top(final Collection<? extends T> c, final int fromIndex, final int toIndex, final int n) {
        return top(c, fromIndex, toIndex, n, null);
    }

    public static <T> List<T> top(final Collection<? extends T> c, final int fromIndex, final int toIndex, final int n, final Comparator<? super T> cmp) {
        N.checkArgNotNegative(n, "n");

        if (n == 0) {
            return new ArrayList<>();
        } else if (n >= toIndex - fromIndex) {
            if (fromIndex == 0 && toIndex == c.size()) {
                return new ArrayList<>(c);
            } else {
                final List<T> res = new ArrayList<>(toIndex - fromIndex);
                final Iterator<? extends T> iter = c.iterator();
                T e = null;

                for (int i = 0; i < toIndex && iter.hasNext(); i++) {
                    e = iter.next();

                    if (i < fromIndex) {
                        continue;
                    }

                    res.add(e);
                }

                return res;
            }
        }

        final Comparator<? super T> comparator = cmp == null ? Comparators.NATURAL_ORDER : cmp;
        final Queue<T> heap = new PriorityQueue<>(n, comparator);

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;
            T e = null;

            for (int i = fromIndex; i < toIndex; i++) {
                e = list.get(i);

                if (heap.size() >= n) {
                    if (comparator.compare(heap.peek(), e) < 0) {
                        heap.poll();
                        heap.add(e);
                    }
                } else {
                    heap.offer(e);
                }
            }
        } else {
            final Iterator<? extends T> iter = c.iterator();
            T e = null;

            for (int i = 0; i < toIndex && iter.hasNext(); i++) {
                e = iter.next();

                if (i < fromIndex) {
                    continue;
                }

                if (heap.size() >= n) {
                    if (comparator.compare(heap.peek(), e) < 0) {
                        heap.poll();
                        heap.add(e);
                    }
                } else {
                    heap.offer(e);
                }
            }
        }

        return createList((T[]) heap.toArray(N.EMPTY_OBJECT_ARRAY));
    }

    /**
     * The present order is kept in the result list.
     *
     * @param a
     * @param n
     * @return
     */
    public static <T extends Comparable<T>> List<T> topp(final T[] a, final int n) {
        return topp(a, n, N.NATURAL_ORDER);
    }

    /**
     * The present order is kept in the result list.
     *
     * @param a
     * @param n
     * @param cmp
     * @return
     */
    public static <T> List<T> topp(final T[] a, final int n, final Comparator<? super T> cmp) {
        return topp(a, 0, len(a), n, cmp);
    }

    /**
     * The present order is kept in the result list.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param n
     * @return
     */
    public static <T extends Comparable<T>> List<T> topp(final T[] a, final int fromIndex, final int toIndex, final int n) {
        return topp(a, fromIndex, toIndex, n, N.NATURAL_ORDER);
    }

    /**
     * The present order is kept in the result list.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param n
     * @param cmp
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> List<T> topp(final T[] a, final int fromIndex, final int toIndex, final int n, final Comparator<? super T> cmp) {
        N.checkArgNotNegative(n, "n");

        if (n == 0) {
            return new ArrayList<>();
        } else if (n >= toIndex - fromIndex) {
            return N.toList(a, fromIndex, toIndex);
        }

        final Comparator<Indexed<T>> comparator = cmp == null ? (Comparator) new Comparator<Indexed<Comparable>>() {
            @Override
            public int compare(final Indexed<Comparable> o1, final Indexed<Comparable> o2) {
                return N.compare(o1.value(), o2.value());
            }
        } : new Comparator<Indexed<T>>() {
            @Override
            public int compare(final Indexed<T> o1, final Indexed<T> o2) {
                return cmp.compare(o1.value(), o2.value());
            }
        };

        final Queue<Indexed<T>> heap = new PriorityQueue<>(n, comparator);
        Indexed<T> indexed = null;

        for (int i = fromIndex; i < toIndex; i++) {
            indexed = Indexed.of(a[i], i);

            if (heap.size() >= n) {
                if (comparator.compare(heap.peek(), indexed) < 0) {
                    heap.poll();
                    heap.add(indexed);
                }
            } else {
                heap.offer(indexed);
            }
        }

        final Indexed<T>[] arrayOfIndexed = heap.toArray(new Indexed[heap.size()]);

        N.sort(arrayOfIndexed, new Comparator<Indexed<T>>() {
            @Override
            public int compare(final Indexed<T> o1, final Indexed<T> o2) {
                return o1.index() - o2.index();
            }
        });

        final List<T> res = new ArrayList<>(arrayOfIndexed.length);

        for (int i = 0, len = arrayOfIndexed.length; i < len; i++) {
            res.add(arrayOfIndexed[i].value());
        }

        return res;
    }

    /**
     * The present order is kept in the result list.
     *
     * @param c
     * @param n
     * @return
     */
    public static <T extends Comparable<T>> List<T> topp(final Collection<? extends T> c, final int n) {
        return topp(c, n, null);
    }

    /**
     * The present order is kept in the result list.
     *
     * @param c
     * @param n
     * @param cmp
     * @return
     */
    public static <T> List<T> topp(final Collection<? extends T> c, final int n, final Comparator<? super T> cmp) {
        return topp(c, 0, size(c), n, cmp);
    }

    /**
     * The present order is kept in the result list.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param n
     * @return
     */
    public static <T extends Comparable<T>> List<T> topp(final Collection<? extends T> c, final int fromIndex, final int toIndex, final int n) {
        return topp(c, fromIndex, toIndex, n, null);
    }

    /**
     * The present order is kept in the result list.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param n
     * @param cmp
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> List<T> topp(final Collection<? extends T> c, final int fromIndex, final int toIndex, final int n, final Comparator<? super T> cmp) {
        N.checkArgNotNegative(n, "n");

        if (n == 0) {
            return new ArrayList<>();
        } else if (n >= toIndex - fromIndex) {
            if (fromIndex == 0 && toIndex == c.size()) {
                return new ArrayList<>(c);
            } else {
                final List<T> res = new ArrayList<>(toIndex - fromIndex);
                final Iterator<? extends T> iter = c.iterator();
                T e = null;

                for (int i = 0; i < toIndex && iter.hasNext(); i++) {
                    e = iter.next();

                    if (i < fromIndex) {
                        continue;
                    }

                    res.add(e);
                }

                return res;
            }
        }

        final Comparator<Indexed<T>> comparator = cmp == null ? (Comparator) new Comparator<Indexed<Comparable>>() {
            @Override
            public int compare(final Indexed<Comparable> o1, final Indexed<Comparable> o2) {
                return N.compare(o1.value(), o2.value());
            }
        } : new Comparator<Indexed<T>>() {
            @Override
            public int compare(final Indexed<T> o1, final Indexed<T> o2) {
                return cmp.compare(o1.value(), o2.value());
            }
        };

        final Queue<Indexed<T>> heap = new PriorityQueue<>(n, comparator);

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;
            Indexed<T> indexed = null;
            T e = null;

            for (int i = fromIndex; i < toIndex; i++) {
                e = list.get(i);

                indexed = Indexed.of(e, i);

                if (heap.size() >= n) {
                    if (comparator.compare(heap.peek(), indexed) < 0) {
                        heap.poll();
                        heap.add(indexed);
                    }
                } else {
                    heap.offer(indexed);
                }
            }
        } else {
            final Iterator<? extends T> iter = c.iterator();
            Indexed<T> indexed = null;
            T e = null;

            for (int i = 0; i < toIndex && iter.hasNext(); i++) {
                e = iter.next();

                if (i < fromIndex) {
                    continue;
                }

                indexed = Indexed.of(e, i);

                if (heap.size() >= n) {
                    if (comparator.compare(heap.peek(), indexed) < 0) {
                        heap.poll();
                        heap.add(indexed);
                    }
                } else {
                    heap.offer(indexed);
                }
            }
        }

        final Indexed<T>[] arrayOfIndexed = heap.toArray(new Indexed[heap.size()]);

        N.sort(arrayOfIndexed, new Comparator<Indexed<T>>() {
            @Override
            public int compare(final Indexed<T> o1, final Indexed<T> o2) {
                return o1.index() - o2.index();
            }
        });

        final List<T> res = new ArrayList<>(arrayOfIndexed.length);

        for (int i = 0, len = arrayOfIndexed.length; i < len; i++) {
            res.add(arrayOfIndexed[i].value());
        }

        return res;
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @return
     */
    public static boolean[] distinct(final boolean[] a) {
        return distinct(a, 0, len(a));
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static boolean[] distinct(final boolean[] a, final int fromIndex, final int toIndex) {
        return N.removeDuplicates(a, fromIndex, toIndex, false);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @return
     */
    public static char[] distinct(final char[] a) {
        return distinct(a, 0, len(a));
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static char[] distinct(final char[] a, final int fromIndex, final int toIndex) {
        return N.removeDuplicates(a, fromIndex, toIndex, false);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @return
     */
    public static byte[] distinct(final byte[] a) {
        return distinct(a, 0, len(a));
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static byte[] distinct(final byte[] a, final int fromIndex, final int toIndex) {
        return N.removeDuplicates(a, fromIndex, toIndex, false);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @return
     */
    public static short[] distinct(final short[] a) {
        return distinct(a, 0, len(a));
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static short[] distinct(final short[] a, final int fromIndex, final int toIndex) {
        return N.removeDuplicates(a, fromIndex, toIndex, false);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @return
     */
    public static int[] distinct(final int[] a) {
        return distinct(a, 0, len(a));
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static int[] distinct(final int[] a, final int fromIndex, final int toIndex) {
        return N.removeDuplicates(a, fromIndex, toIndex, false);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @return
     */
    public static long[] distinct(final long[] a) {
        return distinct(a, 0, len(a));
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static long[] distinct(final long[] a, final int fromIndex, final int toIndex) {
        return N.removeDuplicates(a, fromIndex, toIndex, false);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @return
     */
    public static float[] distinct(final float[] a) {
        return distinct(a, 0, len(a));
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static float[] distinct(final float[] a, final int fromIndex, final int toIndex) {
        return N.removeDuplicates(a, fromIndex, toIndex, false);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @return
     */
    public static double[] distinct(final double[] a) {
        return distinct(a, 0, len(a));
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static double[] distinct(final double[] a, final int fromIndex, final int toIndex) {
        return N.removeDuplicates(a, fromIndex, toIndex, false);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @return
     */
    public static <T> List<T> distinct(final T[] a) {
        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        return distinct(a, 0, a.length);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T> List<T> distinct(final T[] a, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final List<T> result = new ArrayList<>();
        final Set<Object> set = new HashSet<>();

        for (int i = fromIndex; i < toIndex; i++) {
            if (set.add(hashKey(a[i]))) {
                result.add(a[i]);
            }
        }

        return result;
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param c
     * @return
     */
    public static <T> List<T> distinct(final Collection<? extends T> c) {
        if (N.isNullOrEmpty(c)) {
            return new ArrayList<>();
        }

        return distinct(c, 0, c.size());
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T> List<T> distinct(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (N.isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return new ArrayList<>();
        }

        final List<T> result = new ArrayList<>();
        final Set<Object> set = new HashSet<>();
        T e = null;

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                e = list.get(i);

                if (set.add(hashKey(e))) {
                    result.add(e);
                }
            }
        } else {
            final Iterator<? extends T> it = c.iterator();

            for (int i = 0; i < toIndex && it.hasNext(); i++) {
                e = it.next();

                if (i < fromIndex) {
                    continue;
                }

                if (set.add(hashKey(e))) {
                    result.add(e);
                }
            }
        }

        return result;
    }

    /**
     * Distinct by the value mapped from <code>keyMapper</code>.
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param keyMapper don't change value of the input parameter.
     * @return
     */
    public static <T, E extends Exception> List<T> distinctBy(final T[] a, final Try.Function<? super T, ?, E> keyMapper) throws E {
        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        return distinctBy(a, 0, a.length, keyMapper);
    }

    /**
     * Distinct by the value mapped from <code>keyMapper</code>.
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param keyMapper don't change value of the input parameter.
     * @return
     */
    public static <T, E extends Exception> List<T> distinctBy(final T[] a, final int fromIndex, final int toIndex,
            final Try.Function<? super T, ?, E> keyMapper) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final List<T> result = new ArrayList<>();
        final Set<Object> set = new HashSet<>();

        for (int i = fromIndex; i < toIndex; i++) {
            if (set.add(hashKey(keyMapper.apply(a[i])))) {
                result.add(a[i]);
            }
        }

        return result;
    }

    /**
     * Distinct by the value mapped from <code>keyMapper</code>.
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param c
     * @param keyMapper don't change value of the input parameter.
     * @return
     */
    public static <T, E extends Exception> List<T> distinctBy(final Collection<? extends T> c, final Try.Function<? super T, ?, E> keyMapper) throws E {
        if (N.isNullOrEmpty(c)) {
            return new ArrayList<>();
        }

        return distinctBy(c, 0, c.size(), keyMapper);
    }

    /**
     * Distinct by the value mapped from <code>keyMapper</code>.
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param keyMapper don't change value of the input parameter.
     * @return
     */
    public static <T, E extends Exception> List<T> distinctBy(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Try.Function<? super T, ?, E> keyMapper) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (N.isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return new ArrayList<>();
        }

        final List<T> result = new ArrayList<>();
        final Set<Object> set = new HashSet<>();
        T e = null;

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                e = list.get(i);

                if (set.add(hashKey(keyMapper.apply(e)))) {
                    result.add(e);
                }
            }
        } else {
            final Iterator<? extends T> it = c.iterator();

            for (int i = 0; i < toIndex && it.hasNext(); i++) {
                e = it.next();

                if (i < fromIndex) {
                    continue;
                }

                if (set.add(hashKey(keyMapper.apply(e)))) {
                    result.add(e);
                }
            }
        }

        return result;
    }

    private static Object hashKey(Object obj) {
        return obj == null || obj.getClass().isArray() == false ? obj : Wrapper.of(obj);
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same size (the final list may be smaller),
     * or an empty List if the specified array is null or empty.
     *
     * @param a
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<boolean[]> split(final boolean[] a, final int chunkSize) {
        N.checkArgPositive(chunkSize, "chunkSize");

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = a.length;
        final List<boolean[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int from = 0, toIndex = a.length; from < toIndex; from += chunkSize) {
            res.add(copyOfRange(a, from, from <= toIndex - chunkSize ? from + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is null or empty.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<boolean[]> split(final boolean[] a, final int fromIndex, final int toIndex, final int chunkSize) {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgPositive(chunkSize, "chunkSize");

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = toIndex - fromIndex;
        final List<boolean[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int from = fromIndex; from < toIndex; from += chunkSize) {
            res.add(copyOfRange(a, from, from <= toIndex - chunkSize ? from + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is null or empty.
     *
     * @param a
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<char[]> split(final char[] a, final int chunkSize) {
        N.checkArgPositive(chunkSize, "chunkSize");

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = a.length;
        final List<char[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int from = 0, toIndex = a.length; from < toIndex; from += chunkSize) {
            res.add(copyOfRange(a, from, from <= toIndex - chunkSize ? from + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is null or empty.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<char[]> split(final char[] a, final int fromIndex, final int toIndex, final int chunkSize) {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgPositive(chunkSize, "chunkSize");

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = toIndex - fromIndex;
        final List<char[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int from = fromIndex; from < toIndex; from += chunkSize) {
            res.add(copyOfRange(a, from, from <= toIndex - chunkSize ? from + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is null or empty.
     *
     * @param a
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<byte[]> split(final byte[] a, final int chunkSize) {
        N.checkArgPositive(chunkSize, "chunkSize");

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = a.length;
        final List<byte[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int from = 0, toIndex = a.length; from < toIndex; from += chunkSize) {
            res.add(copyOfRange(a, from, from <= toIndex - chunkSize ? from + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is null or empty.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<byte[]> split(final byte[] a, final int fromIndex, final int toIndex, final int chunkSize) {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgPositive(chunkSize, "chunkSize");

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = toIndex - fromIndex;
        final List<byte[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int from = fromIndex; from < toIndex; from += chunkSize) {
            res.add(copyOfRange(a, from, from <= toIndex - chunkSize ? from + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is null or empty.
     *
     * @param a
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<short[]> split(final short[] a, final int chunkSize) {
        N.checkArgPositive(chunkSize, "chunkSize");

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = a.length;
        final List<short[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int from = 0, toIndex = a.length; from < toIndex; from += chunkSize) {
            res.add(copyOfRange(a, from, from <= toIndex - chunkSize ? from + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is null or empty.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<short[]> split(final short[] a, final int fromIndex, final int toIndex, final int chunkSize) {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgPositive(chunkSize, "chunkSize");

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = toIndex - fromIndex;
        final List<short[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int from = fromIndex; from < toIndex; from += chunkSize) {
            res.add(copyOfRange(a, from, from <= toIndex - chunkSize ? from + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is null or empty.
     *
     * @param a
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<int[]> split(final int[] a, final int chunkSize) {
        N.checkArgPositive(chunkSize, "chunkSize");

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = a.length;
        final List<int[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int from = 0, toIndex = a.length; from < toIndex; from += chunkSize) {
            res.add(copyOfRange(a, from, from <= toIndex - chunkSize ? from + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is null or empty.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<int[]> split(final int[] a, final int fromIndex, final int toIndex, final int chunkSize) {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgPositive(chunkSize, "chunkSize");

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = toIndex - fromIndex;
        final List<int[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int from = fromIndex; from < toIndex; from += chunkSize) {
            res.add(copyOfRange(a, from, from <= toIndex - chunkSize ? from + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is null or empty.
     *
     * @param a
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<long[]> split(final long[] a, final int chunkSize) {
        N.checkArgPositive(chunkSize, "chunkSize");

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = a.length;
        final List<long[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int from = 0, toIndex = a.length; from < toIndex; from += chunkSize) {
            res.add(copyOfRange(a, from, from <= toIndex - chunkSize ? from + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is null or empty.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<long[]> split(final long[] a, final int fromIndex, final int toIndex, final int chunkSize) {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgPositive(chunkSize, "chunkSize");

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = toIndex - fromIndex;
        final List<long[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int from = fromIndex; from < toIndex; from += chunkSize) {
            res.add(copyOfRange(a, from, from <= toIndex - chunkSize ? from + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is null or empty.
     *
     * @param a
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<float[]> split(final float[] a, final int chunkSize) {
        N.checkArgPositive(chunkSize, "chunkSize");

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = a.length;
        final List<float[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int from = 0, toIndex = a.length; from < toIndex; from += chunkSize) {
            res.add(copyOfRange(a, from, from <= toIndex - chunkSize ? from + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is null or empty.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<float[]> split(final float[] a, final int fromIndex, final int toIndex, final int chunkSize) {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgPositive(chunkSize, "chunkSize");

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = toIndex - fromIndex;
        final List<float[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int from = fromIndex; from < toIndex; from += chunkSize) {
            res.add(copyOfRange(a, from, from <= toIndex - chunkSize ? from + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is null or empty.
     *
     * @param a
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<double[]> split(final double[] a, final int chunkSize) {
        N.checkArgPositive(chunkSize, "chunkSize");

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = a.length;
        final List<double[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int from = 0, toIndex = a.length; from < toIndex; from += chunkSize) {
            res.add(copyOfRange(a, from, from <= toIndex - chunkSize ? from + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is null or empty.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<double[]> split(final double[] a, final int fromIndex, final int toIndex, final int chunkSize) {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgPositive(chunkSize, "chunkSize");

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = toIndex - fromIndex;
        final List<double[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int from = fromIndex; from < toIndex; from += chunkSize) {
            res.add(copyOfRange(a, from, from <= toIndex - chunkSize ? from + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is null or empty.
     *
     * @param a
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static <T> List<T[]> split(final T[] a, final int chunkSize) {
        N.checkArgPositive(chunkSize, "chunkSize");

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = a.length;
        final List<T[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int from = 0, toIndex = a.length; from < toIndex; from += chunkSize) {
            res.add(copyOfRange(a, from, from <= toIndex - chunkSize ? from + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is null or empty.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static <T> List<T[]> split(final T[] a, final int fromIndex, final int toIndex, final int chunkSize) {
        checkFromToIndex(fromIndex, toIndex, len(a));
        N.checkArgPositive(chunkSize, "chunkSize");

        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = toIndex - fromIndex;
        final List<T[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int from = fromIndex; from < toIndex; from += chunkSize) {
            res.add(copyOfRange(a, from, from <= toIndex - chunkSize ? from + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub lists of a collection, each of the same chunkSize (the final list may be smaller).
     * or an empty List if the specified collection is null or empty. The order of elements in the original collection is kept
     *
     * @param c
     * @param chunkSize the desired size of each sub list (the last may be smaller).
     * @return
     */
    public static <T> List<List<T>> split(final Collection<? extends T> c, final int chunkSize) {
        N.checkArgPositive(chunkSize, "chunkSize");

        if (N.isNullOrEmpty(c)) {
            return new ArrayList<>();
        }

        return split(c, 0, c.size(), chunkSize);
    }

    /**
     * Returns consecutive sub lists of a collection, each of the same chunkSize (the final list may be smaller).
     * or an empty List if the specified collection is null or empty. The order of elements in the original collection is kept
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each sub list (the last may be smaller).
     * @return
     */
    public static <T> List<List<T>> split(final Collection<? extends T> c, final int fromIndex, final int toIndex, final int chunkSize) {
        checkFromToIndex(fromIndex, toIndex, size(c));
        N.checkArgPositive(chunkSize, "chunkSize");

        if (N.isNullOrEmpty(c)) {
            return new ArrayList<>();
        }

        final int len = toIndex - fromIndex;
        final List<List<T>> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        if (c instanceof List) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i += chunkSize) {
                res.add(new ArrayList<>(list.subList(i, i <= toIndex - chunkSize ? i + chunkSize : toIndex)));
            }
        } else {
            final Iterator<? extends T> iter = c.iterator();

            for (int i = 0; i < toIndex; i += chunkSize) {
                if (i < fromIndex) {
                    iter.next();
                    i++;
                    continue;
                }

                final List<T> subList = new ArrayList<>(N.min(chunkSize, toIndex - i));

                for (int j = i, to = i <= toIndex - chunkSize ? i + chunkSize : toIndex; j < to; j++) {
                    subList.add(iter.next());
                }

                res.add(subList);
            }
        }

        return res;
    }

    /**
     * Returns consecutive substring of the specified string, each of the same length (the final list may be smaller),
     * or an empty array if the specified string is null or empty.
     *
     * @param str
     * @param chunkSize the desired size of each sub String (the last may be smaller).
     * @return
     */
    public static List<String> split(final CharSequence str, final int chunkSize) {
        N.checkArgPositive(chunkSize, "chunkSize");

        if (N.isNullOrEmpty(str)) {
            return new ArrayList<>();
        }

        return split(str, 0, str.length(), chunkSize);
    }

    /**
     * Returns consecutive substring of the specified string, each of the same length (the final list may be smaller),
     * or an empty array if the specified string is null or empty.
     *
     * @param str
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each sub String (the last may be smaller).
     * @return
     */
    public static List<String> split(final CharSequence str, final int fromIndex, final int toIndex, final int chunkSize) {
        N.checkFromToIndex(fromIndex, toIndex, len(str));
        N.checkArgPositive(chunkSize, "chunkSize");

        if (N.isNullOrEmpty(str)) {
            return new ArrayList<>();
        }

        final int len = toIndex - fromIndex;
        final List<String> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int from = fromIndex; from < toIndex; from += chunkSize) {
            res.add(str.subSequence(from, from <= toIndex - chunkSize ? from + chunkSize : toIndex).toString());
        }

        return res;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#intersection(IntList)
     */
    public static boolean[] intersection(final boolean[] a, final boolean[] b) {
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
            return N.isNullOrEmpty(a) ? a : N.EMPTY_BOOLEAN_ARRAY;
        }

        return BooleanList.of(a).intersection(BooleanList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#intersection(IntList)
     */
    public static char[] intersection(final char[] a, final char[] b) {
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
            return N.EMPTY_CHAR_ARRAY;
        }

        return CharList.of(a).intersection(CharList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#intersection(IntList)
     */
    public static byte[] intersection(final byte[] a, final byte[] b) {
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
            return N.EMPTY_BYTE_ARRAY;
        }

        return ByteList.of(a).intersection(ByteList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#intersection(IntList)
     */
    public static short[] intersection(final short[] a, final short[] b) {
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
            return N.EMPTY_SHORT_ARRAY;
        }

        return ShortList.of(a).intersection(ShortList.of(b)).trimToSize().array();
    }

    /**
     * Returns a new array with all the elements in <code>b</code> removed by occurrences.
     *
     * <pre>
     * int[] a = {0, 1, 2, 2, 3};
     * int[] b = {2, 5, 1};
     * int[] c = retainAll(a, b); // The elements c in a will b: [1, 2, 2].
     *
     * int[] a = {0, 1, 2, 2, 3};
     * int[] b = {2, 5, 1};
     * int[] c = intersection(a, b); // The elements c in a will b: [1, 2].
     * </pre>
     *
     * @param a
     * @param b
     * @return
     * @see IntList#intersection(IntList)
     */
    public static int[] intersection(final int[] a, final int[] b) {
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
            return N.EMPTY_INT_ARRAY;
        }

        return IntList.of(a).intersection(IntList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#intersection(IntList)
     */
    public static long[] intersection(final long[] a, final long[] b) {
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
            return N.EMPTY_LONG_ARRAY;
        }

        return LongList.of(a).intersection(LongList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#intersection(IntList)
     */
    public static float[] intersection(final float[] a, final float[] b) {
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
            return N.EMPTY_FLOAT_ARRAY;
        }

        return FloatList.of(a).intersection(FloatList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#intersection(IntList)
     */
    public static double[] intersection(final double[] a, final double[] b) {
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
            return N.EMPTY_DOUBLE_ARRAY;
        }

        return DoubleList.of(a).intersection(DoubleList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#intersection(IntList)
     */
    public static <T> List<T> intersection(final T[] a, final Object[] b) {
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
            return new ArrayList<>();
        }

        final Multiset<?> bOccurrences = Multiset.of(b);
        final List<T> result = new ArrayList<>(N.min(9, a.length, b.length));

        for (T e : a) {
            if (bOccurrences.getAndRemove(e) > 0) {
                result.add(e);
            }
        }

        return result;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#intersection(IntList)
     */
    public static <T> List<T> intersection(final Collection<? extends T> a, final Collection<?> b) {
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
            return new ArrayList<>();
        }

        final Multiset<Object> bOccurrences = Multiset.from(b);

        final List<T> result = new ArrayList<>(N.min(9, a.size(), b.size()));

        for (T e : a) {
            if (bOccurrences.getAndRemove(e) > 0) {
                result.add(e);
            }
        }

        return result;
    }

    public static <T> List<T> intersection(final Collection<? extends Collection<? extends T>> c) {
        if (N.isNullOrEmpty(c)) {
            return new ArrayList<>();
        } else if (c.size() == 1) {
            return newArrayList(c.iterator().next());
        }

        for (Collection<? extends T> e : c) {
            if (N.isNullOrEmpty(e)) {
                return new ArrayList<>();
            }
        }

        final Iterator<? extends Collection<? extends T>> iter = c.iterator();
        List<T> result = N.intersection(iter.next(), iter.next());

        while (iter.hasNext()) {
            result = N.intersection(result, iter.next());

            if (result.size() == 0) {
                break;
            }
        }

        return result;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#difference(IntList)
     */
    public static boolean[] difference(final boolean[] a, final boolean[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_BOOLEAN_ARRAY;
        } else if (N.isNullOrEmpty(b)) {
            return a.clone();
        }

        return BooleanList.of(a).difference(BooleanList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#difference(IntList)
     */
    public static char[] difference(final char[] a, final char[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_CHAR_ARRAY;
        } else if (N.isNullOrEmpty(b)) {
            return a.clone();
        }

        return CharList.of(a).difference(CharList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#difference(IntList)
     */
    public static byte[] difference(final byte[] a, final byte[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_BYTE_ARRAY;
        } else if (N.isNullOrEmpty(b)) {
            return a.clone();
        }

        return ByteList.of(a).difference(ByteList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#difference(IntList)
     */
    public static short[] difference(final short[] a, final short[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_SHORT_ARRAY;
        } else if (N.isNullOrEmpty(b)) {
            return a.clone();
        }

        return ShortList.of(a).difference(ShortList.of(b)).trimToSize().array();
    }

    /**
     * Returns a new array with all the elements in <code>b</code> removed by occurrences.
     *
     * <pre>
     * int[] a = {0, 1, 2, 2, 3};
     * int[] b = {2, 5, 1};
     * int[] c = removeAll(a, b); // The elements c in a will b: [0, 3].
     *
     * int[] a = {0, 1, 2, 2, 3};
     * int[] b = {2, 5, 1};
     * int[] c = difference(a, b); // The elements c in a will b: [0, 2, 3].
     * </pre>
     *
     * @param a
     * @param b
     * @return
     * @see IntList#difference(IntList)
     */
    public static int[] difference(final int[] a, final int[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_INT_ARRAY;
        } else if (N.isNullOrEmpty(b)) {
            return a.clone();
        }

        return IntList.of(a).difference(IntList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#difference(IntList)
     */
    public static long[] difference(final long[] a, final long[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_LONG_ARRAY;
        } else if (N.isNullOrEmpty(b)) {
            return a.clone();
        }

        return LongList.of(a).difference(LongList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#difference(IntList)
     */
    public static float[] difference(final float[] a, final float[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_FLOAT_ARRAY;
        } else if (N.isNullOrEmpty(b)) {
            return a.clone();
        }

        return FloatList.of(a).difference(FloatList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#difference(IntList)
     */
    public static double[] difference(final double[] a, final double[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_DOUBLE_ARRAY;
        } else if (N.isNullOrEmpty(b)) {
            return a.clone();
        }

        return DoubleList.of(a).difference(DoubleList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#difference(IntList)
     */
    public static <T> List<T> difference(final T[] a, final Object[] b) {
        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        } else if (N.isNullOrEmpty(b)) {
            return N.asList(a);
        }

        final Multiset<?> bOccurrences = Multiset.of(b);
        final List<T> result = new ArrayList<>(N.min(a.length, N.max(9, a.length - b.length)));

        for (T e : a) {
            if (bOccurrences.getAndRemove(e) < 1) {
                result.add(e);
            }
        }

        return result;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#difference(IntList)
     */
    public static <T> List<T> difference(final Collection<? extends T> a, final Collection<?> b) {
        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        } else if (N.isNullOrEmpty(b)) {
            return new ArrayList<>(a);
        }

        final Multiset<Object> bOccurrences = Multiset.from(b);

        final List<T> result = new ArrayList<>(N.min(a.size(), N.max(9, a.size() - b.size())));

        for (T e : a) {
            if (bOccurrences.getAndRemove(e) < 1) {
                result.add(e);
            }
        }

        return result;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#symmetricDifference(IntList)
     */
    public static boolean[] symmetricDifference(final boolean[] a, final boolean[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_BOOLEAN_ARRAY : b.clone();
        } else if (N.isNullOrEmpty(b)) {
            return a.clone();
        }

        return BooleanList.of(a).symmetricDifference(BooleanList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#symmetricDifference(IntList)
     */
    public static char[] symmetricDifference(final char[] a, final char[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_CHAR_ARRAY : b.clone();
        } else if (N.isNullOrEmpty(b)) {
            return a.clone();
        }

        return CharList.of(a).symmetricDifference(CharList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#symmetricDifference(IntList)
     */
    public static byte[] symmetricDifference(final byte[] a, final byte[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_BYTE_ARRAY : b.clone();
        } else if (N.isNullOrEmpty(b)) {
            return a.clone();
        }

        return ByteList.of(a).symmetricDifference(ByteList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#symmetricDifference(IntList)
     */
    public static short[] symmetricDifference(final short[] a, final short[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_SHORT_ARRAY : b.clone();
        } else if (N.isNullOrEmpty(b)) {
            return a.clone();
        }

        return ShortList.of(a).symmetricDifference(ShortList.of(b)).trimToSize().array();
    }

    /**
     * <pre>
     * int[] a = {0, 1, 2, 2, 3};
     * int[] b = {2, 5, 1};
     * int[] c = symmetricDifference(a, b); // The elements c in a will b: [0, 2, 3, 5].
     * </pre>
     *
     * @param a
     * @param b
     * @return
     * @see IntList#symmetricDifference(IntList)
     * @see N#difference(int[], int[])
     */
    public static int[] symmetricDifference(final int[] a, final int[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_INT_ARRAY : b.clone();
        } else if (N.isNullOrEmpty(b)) {
            return a.clone();
        }

        return IntList.of(a).symmetricDifference(IntList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#symmetricDifference(IntList)
     */
    public static long[] symmetricDifference(final long[] a, final long[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_LONG_ARRAY : b.clone();
        } else if (N.isNullOrEmpty(b)) {
            return a.clone();
        }

        return LongList.of(a).symmetricDifference(LongList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#symmetricDifference(IntList)
     */
    public static float[] symmetricDifference(final float[] a, final float[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_FLOAT_ARRAY : b.clone();
        } else if (N.isNullOrEmpty(b)) {
            return a.clone();
        }

        return FloatList.of(a).symmetricDifference(FloatList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#symmetricDifference(IntList)
     */
    public static double[] symmetricDifference(final double[] a, final double[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_DOUBLE_ARRAY : b.clone();
        } else if (N.isNullOrEmpty(b)) {
            return a.clone();
        }

        return DoubleList.of(a).symmetricDifference(DoubleList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#symmetricDifference(IntList)
     */
    public static <T> List<T> symmetricDifference(final T[] a, final T[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.asList(b);
        } else if (N.isNullOrEmpty(b)) {
            return N.asList(a);
        }

        final Multiset<T> bOccurrences = Multiset.of(b);

        final List<T> result = new ArrayList<>(N.max(9, Math.abs(a.length - b.length)));

        for (T e : a) {
            if (bOccurrences.getAndRemove(e) < 1) {
                result.add(e);
            }
        }

        for (T e : b) {
            if (bOccurrences.getAndRemove(e) > 0) {
                result.add(e);
            }

            if (bOccurrences.isEmpty()) {
                break;
            }
        }

        return result;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#symmetricDifference(IntList)
     */
    public static <T> List<T> symmetricDifference(final Collection<? extends T> a, final Collection<? extends T> b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? new ArrayList<T>() : new ArrayList<>(b);
        } else if (N.isNullOrEmpty(b)) {
            return N.isNullOrEmpty(a) ? new ArrayList<T>() : new ArrayList<>(a);
        }

        //        final List<T> result = difference(a, b);
        //
        //        result.addAll(difference(b, a));
        //
        //        return result;

        final Multiset<T> bOccurrences = Multiset.from(b);
        final List<T> result = new ArrayList<>(N.max(9, Math.abs(a.size() - b.size())));

        for (T e : a) {
            if (bOccurrences.getAndRemove(e) < 1) {
                result.add(e);
            }
        }

        for (T e : b) {
            if (bOccurrences.getAndRemove(e) > 0) {
                result.add(e);
            }

            if (bOccurrences.isEmpty()) {
                break;
            }
        }

        return result;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean[] concat(final boolean[] a, final boolean[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_BOOLEAN_ARRAY : b.clone();
        } else if (N.isNullOrEmpty(b)) {
            return N.isNullOrEmpty(a) ? N.EMPTY_BOOLEAN_ARRAY : a.clone();
        }

        final boolean[] c = new boolean[a.length + b.length];

        copy(a, 0, c, 0, a.length);
        copy(b, 0, c, a.length, b.length);

        return c;
    }

    /**
     *
     * @param aa
     * @return
     */
    @SafeVarargs
    public static boolean[] concat(final boolean[]... aa) {
        if (N.isNullOrEmpty(aa)) {
            return N.EMPTY_BOOLEAN_ARRAY;
        } else if (aa.length == 1) {
            return N.isNullOrEmpty(aa[0]) ? N.EMPTY_BOOLEAN_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (boolean[] a : aa) {
            if (N.isNullOrEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final boolean[] c = new boolean[len];
        int fromIndex = 0;

        for (boolean[] a : aa) {
            if (N.isNullOrEmpty(a)) {
                continue;
            }

            System.arraycopy(a, 0, c, fromIndex, a.length);

            fromIndex += a.length;
        }

        return c;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static char[] concat(final char[] a, final char[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_CHAR_ARRAY : b.clone();
        } else if (N.isNullOrEmpty(b)) {
            return N.isNullOrEmpty(a) ? N.EMPTY_CHAR_ARRAY : a.clone();
        }

        final char[] c = new char[a.length + b.length];

        copy(a, 0, c, 0, a.length);
        copy(b, 0, c, a.length, b.length);

        return c;
    }

    /**
     *
     * @param aa
     * @return
     */
    @SafeVarargs
    public static char[] concat(final char[]... aa) {
        if (N.isNullOrEmpty(aa)) {
            return N.EMPTY_CHAR_ARRAY;
        } else if (aa.length == 1) {
            return N.isNullOrEmpty(aa[0]) ? N.EMPTY_CHAR_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (char[] a : aa) {
            if (N.isNullOrEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final char[] c = new char[len];
        int fromIndex = 0;

        for (char[] a : aa) {
            if (N.isNullOrEmpty(a)) {
                continue;
            }

            System.arraycopy(a, 0, c, fromIndex, a.length);

            fromIndex += a.length;
        }

        return c;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static byte[] concat(final byte[] a, final byte[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_BYTE_ARRAY : b.clone();
        } else if (N.isNullOrEmpty(b)) {
            return N.isNullOrEmpty(a) ? N.EMPTY_BYTE_ARRAY : a.clone();
        }

        final byte[] c = new byte[a.length + b.length];

        copy(a, 0, c, 0, a.length);
        copy(b, 0, c, a.length, b.length);

        return c;
    }

    /**
     *
     * @param aa
     * @return
     */
    @SafeVarargs
    public static byte[] concat(final byte[]... aa) {
        if (N.isNullOrEmpty(aa)) {
            return N.EMPTY_BYTE_ARRAY;
        } else if (aa.length == 1) {
            return N.isNullOrEmpty(aa[0]) ? N.EMPTY_BYTE_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (byte[] a : aa) {
            if (N.isNullOrEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final byte[] c = new byte[len];
        int fromIndex = 0;

        for (byte[] a : aa) {
            if (N.isNullOrEmpty(a)) {
                continue;
            }

            System.arraycopy(a, 0, c, fromIndex, a.length);

            fromIndex += a.length;
        }

        return c;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static short[] concat(final short[] a, final short[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_SHORT_ARRAY : b.clone();
        } else if (N.isNullOrEmpty(b)) {
            return N.isNullOrEmpty(a) ? N.EMPTY_SHORT_ARRAY : a.clone();
        }

        final short[] c = new short[a.length + b.length];

        copy(a, 0, c, 0, a.length);
        copy(b, 0, c, a.length, b.length);

        return c;
    }

    /**
     *
     * @param aa
     * @return
     */
    @SafeVarargs
    public static short[] concat(final short[]... aa) {
        if (N.isNullOrEmpty(aa)) {
            return N.EMPTY_SHORT_ARRAY;
        } else if (aa.length == 1) {
            return N.isNullOrEmpty(aa[0]) ? N.EMPTY_SHORT_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (short[] a : aa) {
            if (N.isNullOrEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final short[] c = new short[len];
        int fromIndex = 0;

        for (short[] a : aa) {
            if (N.isNullOrEmpty(a)) {
                continue;
            }

            System.arraycopy(a, 0, c, fromIndex, a.length);

            fromIndex += a.length;
        }

        return c;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static int[] concat(final int[] a, final int[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_INT_ARRAY : b.clone();
        } else if (N.isNullOrEmpty(b)) {
            return N.isNullOrEmpty(a) ? N.EMPTY_INT_ARRAY : a.clone();
        }

        final int[] c = new int[a.length + b.length];

        copy(a, 0, c, 0, a.length);
        copy(b, 0, c, a.length, b.length);

        return c;
    }

    /**
     *
     * @param aa
     * @return
     */
    @SafeVarargs
    public static int[] concat(final int[]... aa) {
        if (N.isNullOrEmpty(aa)) {
            return N.EMPTY_INT_ARRAY;
        } else if (aa.length == 1) {
            return N.isNullOrEmpty(aa[0]) ? N.EMPTY_INT_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (int[] a : aa) {
            if (N.isNullOrEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final int[] c = new int[len];
        int fromIndex = 0;

        for (int[] a : aa) {
            if (N.isNullOrEmpty(a)) {
                continue;
            }

            System.arraycopy(a, 0, c, fromIndex, a.length);

            fromIndex += a.length;
        }

        return c;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static long[] concat(final long[] a, final long[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_LONG_ARRAY : b.clone();
        } else if (N.isNullOrEmpty(b)) {
            return N.isNullOrEmpty(a) ? N.EMPTY_LONG_ARRAY : a.clone();
        }

        final long[] c = new long[a.length + b.length];

        copy(a, 0, c, 0, a.length);
        copy(b, 0, c, a.length, b.length);

        return c;
    }

    /**
     *
     * @param aa
     * @return
     */
    @SafeVarargs
    public static long[] concat(final long[]... aa) {
        if (N.isNullOrEmpty(aa)) {
            return N.EMPTY_LONG_ARRAY;
        } else if (aa.length == 1) {
            return N.isNullOrEmpty(aa[0]) ? N.EMPTY_LONG_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (long[] a : aa) {
            if (N.isNullOrEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final long[] c = new long[len];
        int fromIndex = 0;

        for (long[] a : aa) {
            if (N.isNullOrEmpty(a)) {
                continue;
            }

            System.arraycopy(a, 0, c, fromIndex, a.length);

            fromIndex += a.length;
        }

        return c;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static float[] concat(final float[] a, final float[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_FLOAT_ARRAY : b.clone();
        } else if (N.isNullOrEmpty(b)) {
            return N.isNullOrEmpty(a) ? N.EMPTY_FLOAT_ARRAY : a.clone();
        }

        final float[] c = new float[a.length + b.length];

        copy(a, 0, c, 0, a.length);
        copy(b, 0, c, a.length, b.length);

        return c;
    }

    /**
     *
     * @param aa
     * @return
     */
    @SafeVarargs
    public static float[] concat(final float[]... aa) {
        if (N.isNullOrEmpty(aa)) {
            return N.EMPTY_FLOAT_ARRAY;
        } else if (aa.length == 1) {
            return N.isNullOrEmpty(aa[0]) ? N.EMPTY_FLOAT_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (float[] a : aa) {
            if (N.isNullOrEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final float[] c = new float[len];
        int fromIndex = 0;

        for (float[] a : aa) {
            if (N.isNullOrEmpty(a)) {
                continue;
            }

            System.arraycopy(a, 0, c, fromIndex, a.length);

            fromIndex += a.length;
        }

        return c;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static double[] concat(final double[] a, final double[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_DOUBLE_ARRAY : b.clone();
        } else if (N.isNullOrEmpty(b)) {
            return N.isNullOrEmpty(a) ? N.EMPTY_DOUBLE_ARRAY : a.clone();
        }

        final double[] c = new double[a.length + b.length];

        copy(a, 0, c, 0, a.length);
        copy(b, 0, c, a.length, b.length);

        return c;
    }

    /**
     *
     * @param aa
     * @return
     */
    @SafeVarargs
    public static double[] concat(final double[]... aa) {
        if (N.isNullOrEmpty(aa)) {
            return N.EMPTY_DOUBLE_ARRAY;
        } else if (aa.length == 1) {
            return N.isNullOrEmpty(aa[0]) ? N.EMPTY_DOUBLE_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (double[] a : aa) {
            if (N.isNullOrEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final double[] c = new double[len];
        int fromIndex = 0;

        for (double[] a : aa) {
            if (N.isNullOrEmpty(a)) {
                continue;
            }

            System.arraycopy(a, 0, c, fromIndex, a.length);

            fromIndex += a.length;
        }

        return c;
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] concat(final T[] a, final T[] b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? a : b.clone();
        } else if (N.isNullOrEmpty(b)) {
            return a.clone();
        }

        final T[] c = (T[]) newArray(a.getClass().getComponentType(), a.length + b.length);

        copy(a, 0, c, 0, a.length);
        copy(b, 0, c, a.length, b.length);

        return c;
    }

    /**
     *
     * @param aa
     * @return
     * @throws NullPointerException if the specified <code>aa</code> is <code>null</code>.
     */
    @SafeVarargs
    public static <T> T[] concat(final T[]... aa) {
        N.checkArgNotNull(aa, "aa");

        if (aa.length == 1) {
            return N.isNullOrEmpty(aa[0]) ? aa[0] : aa[0].clone();
        }

        int len = 0;

        for (T[] a : aa) {
            if (N.isNullOrEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final T[] c = N.newArray(aa.getClass().getComponentType().getComponentType(), len);
        int fromIndex = 0;

        for (T[] a : aa) {
            if (N.isNullOrEmpty(a)) {
                continue;
            }

            System.arraycopy(a, 0, c, fromIndex, a.length);

            fromIndex += a.length;
        }

        return c;
    }

    public static <T> List<T> concat(final Collection<? extends T> a, final Collection<? extends T> b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? new ArrayList<T>(0) : new ArrayList<>(b);
        } else if (N.isNullOrEmpty(b)) {
            return new ArrayList<>(a);
        }

        final List<T> result = new ArrayList<>(a.size() + b.size());

        result.addAll(a);
        result.addAll(b);

        return result;
    }

    @SafeVarargs
    public static <T> List<T> concat(final Collection<? extends T>... a) {
        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        return concat(Arrays.asList(a));
    }

    public static <T> List<T> concat(final Collection<? extends Collection<? extends T>> c) {
        return concat(c, Factory.<T> ofList());
    }

    public static <T, C extends Collection<T>> C concat(final Collection<? extends Collection<? extends T>> c, final IntFunction<? extends C> supplier) {
        if (N.isNullOrEmpty(c)) {
            return supplier.apply(0);
        }

        int count = 0;

        for (Collection<? extends T> e : c) {
            if (N.notNullOrEmpty(e)) {
                count += e.size();
            }
        }

        final C result = supplier.apply(count);

        for (Collection<? extends T> e : c) {
            if (N.notNullOrEmpty(e)) {
                result.addAll(e);
            }
        }

        return result;
    }

    public static <T> ObjIterator<T> concat(final Iterator<? extends T> a, final Iterator<? extends T> b) {
        return Iterators.concat(a, b);
    }

    @SafeVarargs
    public static <T> ObjIterator<T> concat(final Iterator<? extends T>... a) {
        return Iterators.concat(a);
    }

    public static <T> ObjIterator<T> concatt(final Collection<? extends Iterator<? extends T>> c) {
        return Iterators.concat(c);
    }

    public static int replaceAll(final boolean[] a, final boolean oldVal, final boolean newVal) {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int result = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == oldVal) {
                a[i] = newVal;

                result++;
            }
        }

        return result;
    }

    public static int replaceAll(final char[] a, final char oldVal, final char newVal) {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int result = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == oldVal) {
                a[i] = newVal;

                result++;
            }
        }

        return result;
    }

    public static int replaceAll(final byte[] a, final byte oldVal, final byte newVal) {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int result = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == oldVal) {
                a[i] = newVal;

                result++;
            }
        }

        return result;
    }

    public static int replaceAll(final short[] a, final short oldVal, final short newVal) {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int result = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == oldVal) {
                a[i] = newVal;

                result++;
            }
        }

        return result;
    }

    public static int replaceAll(final int[] a, final int oldVal, final int newVal) {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int result = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == oldVal) {
                a[i] = newVal;

                result++;
            }
        }

        return result;
    }

    public static int replaceAll(final long[] a, final long oldVal, final long newVal) {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int result = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == oldVal) {
                a[i] = newVal;

                result++;
            }
        }

        return result;
    }

    public static int replaceAll(final float[] a, final float oldVal, final float newVal) {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int result = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (Float.compare(a[i], oldVal) == 0) {
                a[i] = newVal;

                result++;
            }
        }

        return result;
    }

    public static int replaceAll(final double[] a, final double oldVal, final double newVal) {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int result = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (Double.compare(a[i], oldVal) == 0) {
                a[i] = newVal;

                result++;
            }
        }

        return result;
    }

    public static <T> int replaceAll(final T[] a, final Object oldVal, final T newVal) {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int result = 0;

        if (oldVal == null) {
            for (int i = 0, len = a.length; i < len; i++) {
                if (a[i] == null) {
                    a[i] = newVal;

                    result++;
                }
            }
        } else {
            for (int i = 0, len = a.length; i < len; i++) {
                if (equals(a[i], oldVal)) {
                    a[i] = newVal;

                    result++;
                }
            }
        }

        return result;
    }

    public static <T> int replaceAll(final List<T> list, final Object oldVal, final T newVal) {
        if (N.isNullOrEmpty(list)) {
            return 0;
        }

        int result = 0;

        final int size = list.size();

        if (size < REPLACEALL_THRESHOLD || list instanceof RandomAccess) {
            if (oldVal == null) {
                for (int i = 0; i < size; i++) {
                    if (list.get(i) == null) {
                        list.set(i, newVal);

                        result++;
                    }
                }
            } else {
                for (int i = 0; i < size; i++) {
                    if (oldVal.equals(list.get(i))) {
                        list.set(i, newVal);

                        result++;
                    }
                }
            }
        } else {
            final ListIterator<T> itr = list.listIterator();

            if (oldVal == null) {
                for (int i = 0; i < size; i++) {
                    if (itr.next() == null) {
                        itr.set(newVal);

                        result++;
                    }
                }
            } else {
                for (int i = 0; i < size; i++) {
                    if (oldVal.equals(itr.next())) {
                        itr.set(newVal);

                        result++;
                    }
                }
            }
        }

        return result;
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     *
     * @param a
     * @param element
     * @return A new array containing the existing elements plus the new element
     */
    public static boolean[] add(final boolean[] a, final boolean element) {
        if (N.isNullOrEmpty(a)) {
            return Array.of(element);
        }

        final boolean[] newArray = new boolean[a.length + 1];

        copy(a, 0, newArray, 0, a.length);
        newArray[a.length] = element;

        return newArray;
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     *
     * @param a
     * @param element
     * @return A new array containing the existing elements plus the new element
     */
    public static char[] add(final char[] a, final char element) {
        if (N.isNullOrEmpty(a)) {
            return Array.of(element);
        }

        final char[] newArray = new char[a.length + 1];

        copy(a, 0, newArray, 0, a.length);
        newArray[a.length] = element;

        return newArray;
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     *
     * @param a
     * @param element
     * @return A new array containing the existing elements plus the new element
     */
    public static byte[] add(final byte[] a, final byte element) {
        if (N.isNullOrEmpty(a)) {
            return Array.of(element);
        }

        final byte[] newArray = new byte[a.length + 1];

        copy(a, 0, newArray, 0, a.length);
        newArray[a.length] = element;

        return newArray;
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     *
     * @param a
     * @param element
     * @return A new array containing the existing elements plus the new element
     */
    public static short[] add(final short[] a, final short element) {
        if (N.isNullOrEmpty(a)) {
            return Array.of(element);
        }

        final short[] newArray = new short[a.length + 1];

        copy(a, 0, newArray, 0, a.length);
        newArray[a.length] = element;

        return newArray;
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     *
     * @param a
     * @param element
     * @return A new array containing the existing elements plus the new element
     */
    public static int[] add(final int[] a, final int element) {
        if (N.isNullOrEmpty(a)) {
            return Array.of(element);
        }

        final int[] newArray = new int[a.length + 1];

        copy(a, 0, newArray, 0, a.length);
        newArray[a.length] = element;

        return newArray;
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     *
     * @param a
     * @param element
     * @return A new array containing the existing elements plus the new element
     */
    public static long[] add(final long[] a, final long element) {
        if (N.isNullOrEmpty(a)) {
            return Array.of(element);
        }

        final long[] newArray = new long[a.length + 1];

        copy(a, 0, newArray, 0, a.length);
        newArray[a.length] = element;

        return newArray;
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     *
     * @param a
     * @param element
     * @return A new array containing the existing elements plus the new element
     */
    public static float[] add(final float[] a, final float element) {
        if (N.isNullOrEmpty(a)) {
            return Array.of(element);
        }

        final float[] newArray = new float[a.length + 1];

        copy(a, 0, newArray, 0, a.length);
        newArray[a.length] = element;

        return newArray;
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     *
     * @param a
     * @param element
     * @return A new array containing the existing elements plus the new element
     */
    public static double[] add(final double[] a, final double element) {
        if (N.isNullOrEmpty(a)) {
            return Array.of(element);
        }

        final double[] newArray = new double[a.length + 1];

        copy(a, 0, newArray, 0, a.length);
        newArray[a.length] = element;

        return newArray;
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     *
     * @param a
     * @param element
     * @return A new array containing the existing elements plus the new element
     */
    public static String[] add(final String[] a, final String element) {
        if (N.isNullOrEmpty(a)) {
            return N.asArray(element);
        }

        final String[] newArray = new String[a.length + 1];

        copy(a, 0, newArray, 0, a.length);
        newArray[a.length] = element;

        return newArray;
    }

    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     *
     * @param a
     * @param element
     * @return A new array containing the existing elements plus the new element
     * @throws NullPointerException if the specified <code>a</code> is <code>null</code>.
     */
    public static <T> T[] add(final T[] a, final T element) {
        N.checkArgNotNull(a, "a");

        if (N.isNullOrEmpty(a)) {
            return N.asArray(element);
        }

        final T[] newArray = (T[]) Array.newInstance(a.getClass().getComponentType(), a.length + 1);

        copy(a, 0, newArray, 0, a.length);
        newArray[a.length] = element;

        return newArray;
    }

    /**
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     *
     * @param a
     *            the first array whose elements are added to the new array.
     * @param b
     *            the second array whose elements are added to the new array.
     * @return A new array containing the elements from a and b
     */
    @SafeVarargs
    public static boolean[] addAll(final boolean[] a, final boolean... b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_BOOLEAN_ARRAY : b.clone();
        }

        final boolean[] newArray = new boolean[a.length + b.length];

        copy(a, 0, newArray, 0, a.length);
        copy(b, 0, newArray, a.length, b.length);

        return newArray;
    }

    /**
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     *
     * @param a
     *            the first array whose elements are added to the new array.
     * @param b
     *            the second array whose elements are added to the new array.
     * @return A new array containing the elements from a and b
     */
    @SafeVarargs
    public static char[] addAll(final char[] a, final char... b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_CHAR_ARRAY : b.clone();
        }

        final char[] newArray = new char[a.length + b.length];

        copy(a, 0, newArray, 0, a.length);
        copy(b, 0, newArray, a.length, b.length);

        return newArray;
    }

    /**
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     *
     * @param a
     *            the first array whose elements are added to the new array.
     * @param b
     *            the second array whose elements are added to the new array.
     * @return A new array containing the elements from a and b
     */
    @SafeVarargs
    public static byte[] addAll(final byte[] a, final byte... b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_BYTE_ARRAY : b.clone();
        }

        final byte[] newArray = new byte[a.length + b.length];

        copy(a, 0, newArray, 0, a.length);
        copy(b, 0, newArray, a.length, b.length);

        return newArray;
    }

    /**
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     *
     * @param a
     *            the first array whose elements are added to the new array.
     * @param b
     *            the second array whose elements are added to the new array.
     * @return A new array containing the elements from a and b
     */
    @SafeVarargs
    public static short[] addAll(final short[] a, final short... b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_SHORT_ARRAY : b.clone();
        }

        final short[] newArray = new short[a.length + b.length];

        copy(a, 0, newArray, 0, a.length);
        copy(b, 0, newArray, a.length, b.length);

        return newArray;
    }

    /**
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     *
     * @param a
     *            the first array whose elements are added to the new array.
     * @param b
     *            the second array whose elements are added to the new array.
     * @return A new array containing the elements from a and b
     */
    @SafeVarargs
    public static int[] addAll(final int[] a, final int... b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_INT_ARRAY : b.clone();
        }

        final int[] newArray = new int[a.length + b.length];

        copy(a, 0, newArray, 0, a.length);
        copy(b, 0, newArray, a.length, b.length);

        return newArray;
    }

    /**
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     *
     * @param a
     *            the first array whose elements are added to the new array.
     * @param b
     *            the second array whose elements are added to the new array.
     * @return A new array containing the elements from a and b
     */
    @SafeVarargs
    public static long[] addAll(final long[] a, final long... b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_LONG_ARRAY : b.clone();
        }

        final long[] newArray = new long[a.length + b.length];

        copy(a, 0, newArray, 0, a.length);
        copy(b, 0, newArray, a.length, b.length);

        return newArray;
    }

    /**
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     *
     * @param a
     *            the first array whose elements are added to the new array.
     * @param b
     *            the second array whose elements are added to the new array.
     * @return A new array containing the elements from a and b
     */
    @SafeVarargs
    public static float[] addAll(final float[] a, final float... b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_FLOAT_ARRAY : b.clone();
        }

        final float[] newArray = new float[a.length + b.length];

        copy(a, 0, newArray, 0, a.length);
        copy(b, 0, newArray, a.length, b.length);

        return newArray;
    }

    /**
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     *
     * @param a
     *            the first array whose elements are added to the new array.
     * @param b
     *            the second array whose elements are added to the new array.
     * @return A new array containing the elements from a and b
     */
    @SafeVarargs
    public static double[] addAll(final double[] a, final double... b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_DOUBLE_ARRAY : b.clone();
        }

        final double[] newArray = new double[a.length + b.length];

        copy(a, 0, newArray, 0, a.length);
        copy(b, 0, newArray, a.length, b.length);

        return newArray;
    }

    /**
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     *
     * @param a
     *            the first array whose elements are added to the new array.
     * @param b
     *            the second array whose elements are added to the new array.
     * @return A new array containing the elements from a and b
     */
    @SafeVarargs
    public static String[] addAll(final String[] a, final String... b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.EMPTY_STRING_ARRAY : b.clone();
        }

        final String[] newArray = new String[a.length + b.length];

        copy(a, 0, newArray, 0, a.length);
        copy(b, 0, newArray, a.length, b.length);

        return newArray;
    }

    /**
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     *
     * @param a
     *            the first array whose elements are added to the new array.
     * @param b
     *            the second array whose elements are added to the new array.
     * @return A new array containing the elements from a and b
     * @throws NullPointerException if the specified <code>a</code> is <code>null</code>.
     */
    @SafeVarargs
    public static <T> T[] addAll(final T[] a, final T... b) {
        N.checkArgNotNull(a, "a");

        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? b : b.clone();
        }

        final T[] newArray = (T[]) Array.newInstance(a.getClass().getComponentType(), a.length + b.length);

        copy(a, 0, newArray, 0, a.length);
        copy(b, 0, newArray, a.length, b.length);

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * plus the given element on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param index
     *            the position of the new object
     * @param element
     *            the object to add
     * @return A new array containing the existing elements and the new element
     */
    public static boolean[] insert(final boolean[] a, final int index, final boolean element) {
        if (N.isNullOrEmpty(a) && index == 0) {
            return Array.of(element);
        }

        final boolean[] newArray = new boolean[a.length + 1];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        newArray[index] = element;

        if (index < a.length) {
            copy(a, index, newArray, index + 1, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * plus the given element on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param index
     *            the position of the new object
     * @param element
     *            the object to add
     * @return A new array containing the existing elements and the new element
     */
    public static char[] insert(final char[] a, final int index, final char element) {
        if (N.isNullOrEmpty(a) && index == 0) {
            return Array.of(element);
        }

        final char[] newArray = new char[a.length + 1];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        newArray[index] = element;

        if (index < a.length) {
            copy(a, index, newArray, index + 1, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * plus the given element on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param index
     *            the position of the new object
     * @param element
     *            the object to add
     * @return A new array containing the existing elements and the new element
     */
    public static byte[] insert(final byte[] a, final int index, final byte element) {
        if (N.isNullOrEmpty(a) && index == 0) {
            return Array.of(element);
        }

        final byte[] newArray = new byte[a.length + 1];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        newArray[index] = element;

        if (index < a.length) {
            copy(a, index, newArray, index + 1, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * plus the given element on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param index
     *            the position of the new object
     * @param element
     *            the object to add
     * @return A new array containing the existing elements and the new element
     */
    public static short[] insert(final short[] a, final int index, final short element) {
        if (N.isNullOrEmpty(a) && index == 0) {
            return Array.of(element);
        }

        final short[] newArray = new short[a.length + 1];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        newArray[index] = element;

        if (index < a.length) {
            copy(a, index, newArray, index + 1, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * plus the given element on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param index
     *            the position of the new object
     * @param element
     *            the object to add
     * @return A new array containing the existing elements and the new element
     */
    public static int[] insert(final int[] a, final int index, final int element) {
        if (N.isNullOrEmpty(a) && index == 0) {
            return Array.of(element);
        }

        final int[] newArray = new int[a.length + 1];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        newArray[index] = element;

        if (index < a.length) {
            copy(a, index, newArray, index + 1, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * plus the given element on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param index
     *            the position of the new object
     * @param element
     *            the object to add
     * @return A new array containing the existing elements and the new element
     */
    public static long[] insert(final long[] a, final int index, final long element) {
        if (N.isNullOrEmpty(a) && index == 0) {
            return Array.of(element);
        }

        final long[] newArray = new long[a.length + 1];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        newArray[index] = element;

        if (index < a.length) {
            copy(a, index, newArray, index + 1, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * plus the given element on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param index
     *            the position of the new object
     * @param element
     *            the object to add
     * @return A new array containing the existing elements and the new element
     */
    public static float[] insert(final float[] a, final int index, final float element) {
        if (N.isNullOrEmpty(a) && index == 0) {
            return Array.of(element);
        }

        final float[] newArray = new float[a.length + 1];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        newArray[index] = element;

        if (index < a.length) {
            copy(a, index, newArray, index + 1, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * plus the given element on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param index
     *            the position of the new object
     * @param element
     *            the object to add
     * @return A new array containing the existing elements and the new element
     */
    public static double[] insert(final double[] a, final int index, final double element) {
        if (N.isNullOrEmpty(a) && index == 0) {
            return Array.of(element);
        }

        final double[] newArray = new double[a.length + 1];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        newArray[index] = element;

        if (index < a.length) {
            copy(a, index, newArray, index + 1, a.length - index);
        }

        return newArray;
    }

    public static String[] insert(final String[] a, final int index, final String element) {
        if (N.isNullOrEmpty(a) && index == 0) {
            return N.asArray(element);
        }

        final String[] newArray = new String[a.length + 1];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        newArray[index] = element;

        if (index < a.length) {
            copy(a, index, newArray, index + 1, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * plus the given element on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param index
     *            the position of the new object
     * @param element
     *            the object to add
     * @return A new array containing the existing elements and the new element
     * @throws NullPointerException if the specified <code>a</code> is <code>null</code>.
     */
    public static <T> T[] insert(final T[] a, final int index, final T element) {
        N.checkArgNotNull(a, "a");

        final T[] newArray = N.newArray(a.getClass().getComponentType(), a.length + 1);

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        newArray[index] = element;

        if (index < a.length) {
            copy(a, index, newArray, index + 1, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elements at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     *
     * @param a
     *            the first array whose elements are added to the new array.
     * @param index
     *            the position of the new elements start from
     * @param b
     *            the second array whose elements are added to the new array.
     * @return A new array containing the elements from a and b
     */
    @SafeVarargs
    public static boolean[] insertAll(final boolean[] a, final int index, final boolean... b) {
        if (N.isNullOrEmpty(a) && index == 0) {
            return b.clone();
        }

        final boolean[] newArray = new boolean[a.length + b.length];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        copy(b, 0, newArray, index, b.length);

        if (index < a.length) {
            copy(a, index, newArray, index + b.length, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elements at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     *
     * @param a
     *            the first array whose elements are added to the new array.
     * @param index
     *            the position of the new elements start from
     * @param b
     *            the second array whose elements are added to the new array.
     * @return A new array containing the elements from a and b
     */
    @SafeVarargs
    public static char[] insertAll(final char[] a, final int index, final char... b) {
        if (N.isNullOrEmpty(a) && index == 0) {
            return b.clone();
        }

        final char[] newArray = new char[a.length + b.length];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        copy(b, 0, newArray, index, b.length);

        if (index < a.length) {
            copy(a, index, newArray, index + b.length, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elements at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     *
     * @param a
     *            the first array whose elements are added to the new array.
     * @param index
     *            the position of the new elements start from
     * @param b
     *            the second array whose elements are added to the new array.
     * @return A new array containing the elements from a and b
     */
    @SafeVarargs
    public static byte[] insertAll(final byte[] a, final int index, final byte... b) {
        if (N.isNullOrEmpty(a) && index == 0) {
            return b.clone();
        }

        final byte[] newArray = new byte[a.length + b.length];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        copy(b, 0, newArray, index, b.length);

        if (index < a.length) {
            copy(a, index, newArray, index + b.length, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elements at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     *
     * @param a
     *            the first array whose elements are added to the new array.
     * @param index
     *            the position of the new elements start from
     * @param b
     *            the second array whose elements are added to the new array.
     * @return A new array containing the elements from a and b
     */
    @SafeVarargs
    public static short[] insertAll(final short[] a, final int index, final short... b) {
        if (N.isNullOrEmpty(a) && index == 0) {
            return b.clone();
        }

        final short[] newArray = new short[a.length + b.length];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        copy(b, 0, newArray, index, b.length);

        if (index < a.length) {
            copy(a, index, newArray, index + b.length, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elements at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     *
     * @param a
     *            the first array whose elements are added to the new array.
     * @param index
     *            the position of the new elements start from
     * @param b
     *            the second array whose elements are added to the new array.
     * @return A new array containing the elements from a and b
     */
    @SafeVarargs
    public static int[] insertAll(final int[] a, final int index, final int... b) {
        if (N.isNullOrEmpty(a) && index == 0) {
            return b.clone();
        }

        final int[] newArray = new int[a.length + b.length];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        copy(b, 0, newArray, index, b.length);

        if (index < a.length) {
            copy(a, index, newArray, index + b.length, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elements at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     *
     * @param a
     *            the first array whose elements are added to the new array.
     * @param index
     *            the position of the new elements start from
     * @param b
     *            the second array whose elements are added to the new array.
     * @return A new array containing the elements from a and b
     */
    @SafeVarargs
    public static long[] insertAll(final long[] a, final int index, final long... b) {
        if (N.isNullOrEmpty(a) && index == 0) {
            return b.clone();
        }

        final long[] newArray = new long[a.length + b.length];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        copy(b, 0, newArray, index, b.length);

        if (index < a.length) {
            copy(a, index, newArray, index + b.length, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elements at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     *
     * @param a
     *            the first array whose elements are added to the new array.
     * @param index
     *            the position of the new elements start from
     * @param b
     *            the second array whose elements are added to the new array.
     * @return A new array containing the elements from a and b
     */
    @SafeVarargs
    public static float[] insertAll(final float[] a, final int index, final float... b) {
        if (N.isNullOrEmpty(a) && index == 0) {
            return b.clone();
        }

        final float[] newArray = new float[a.length + b.length];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        copy(b, 0, newArray, index, b.length);

        if (index < a.length) {
            copy(a, index, newArray, index + b.length, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elements at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     *
     * @param a
     *            the first array whose elements are added to the new array.
     * @param index
     *            the position of the new elements start from
     * @param b
     *            the second array whose elements are added to the new array.
     * @return A new array containing the elements from a and b
     */
    @SafeVarargs
    public static double[] insertAll(final double[] a, final int index, final double... b) {
        if (N.isNullOrEmpty(a) && index == 0) {
            return b.clone();
        }

        final double[] newArray = new double[a.length + b.length];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        copy(b, 0, newArray, index, b.length);

        if (index < a.length) {
            copy(a, index, newArray, index + b.length, a.length - index);
        }

        return newArray;
    }

    @SafeVarargs
    public static String[] insertAll(final String[] a, final int index, final String... b) {
        if (N.isNullOrEmpty(a) && index == 0) {
            return b.clone();
        }

        final String[] newArray = new String[a.length + b.length];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        copy(b, 0, newArray, index, b.length);

        if (index < a.length) {
            copy(a, index, newArray, index + b.length, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elements at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     *
     * @param a
     *            the first array whose elements are added to the new array.
     * @param index
     *            the position of the new elements start from
     * @param b
     *            the second array whose elements are added to the new array.
     * @return A new array containing the elements from a and b
     * @throws NullPointerException if the specified <code>a</code> is <code>null</code>.
     */
    @SafeVarargs
    public static <T> T[] insertAll(final T[] a, final int index, final T... b) {
        N.checkArgNotNull(a, "a");

        final T[] newArray = (T[]) Array.newInstance(a.getClass().getComponentType(), a.length + b.length);

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        copy(b, 0, newArray, index, b.length);

        if (index < a.length) {
            copy(a, index, newArray, index + b.length, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from their
     * indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     *
     *
     * @param <T>
     *            the component type of the array
     * @param a
     * @param index
     *            the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     */
    public static boolean[] delete(final boolean[] a, final int index) {
        final boolean[] result = new boolean[a.length - 1];

        if (index > 0) {
            copy(a, 0, result, 0, index);
        }

        if (index + 1 < a.length) {
            copy(a, index + 1, result, index, a.length - index - 1);
        }

        return result;
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from their
     * indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     *
     *
     * @param <T>
     *            the component type of the array
     * @param a
     * @param index
     *            the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     */
    public static char[] delete(final char[] a, final int index) {
        final char[] result = new char[a.length - 1];

        if (index > 0) {
            copy(a, 0, result, 0, index);
        }

        if (index + 1 < a.length) {
            copy(a, index + 1, result, index, a.length - index - 1);
        }

        return result;
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from their
     * indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     *
     *
     * @param <T>
     *            the component type of the array
     * @param a
     * @param index
     *            the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     */
    public static byte[] delete(final byte[] a, final int index) {
        final byte[] result = new byte[a.length - 1];

        if (index > 0) {
            copy(a, 0, result, 0, index);
        }

        if (index + 1 < a.length) {
            copy(a, index + 1, result, index, a.length - index - 1);
        }

        return result;
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from their
     * indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     *
     *
     * @param <T>
     *            the component type of the array
     * @param a
     * @param index
     *            the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     */
    public static short[] delete(final short[] a, final int index) {
        final short[] result = new short[a.length - 1];

        if (index > 0) {
            copy(a, 0, result, 0, index);
        }

        if (index + 1 < a.length) {
            copy(a, index + 1, result, index, a.length - index - 1);
        }

        return result;
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from their
     * indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     *
     *
     * @param <T>
     *            the component type of the array
     * @param a
     * @param index
     *            the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     */
    public static int[] delete(final int[] a, final int index) {
        final int[] result = new int[a.length - 1];

        if (index > 0) {
            copy(a, 0, result, 0, index);
        }

        if (index + 1 < a.length) {
            copy(a, index + 1, result, index, a.length - index - 1);
        }

        return result;
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from their
     * indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     *
     *
     * @param <T>
     *            the component type of the array
     * @param a
     * @param index
     *            the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     */
    public static long[] delete(final long[] a, final int index) {
        final long[] result = new long[a.length - 1];

        if (index > 0) {
            copy(a, 0, result, 0, index);
        }

        if (index + 1 < a.length) {
            copy(a, index + 1, result, index, a.length - index - 1);
        }

        return result;
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from their
     * indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     *
     *
     * @param <T>
     *            the component type of the array
     * @param a
     * @param index
     *            the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     */
    public static float[] delete(final float[] a, final int index) {
        final float[] result = new float[a.length - 1];

        if (index > 0) {
            copy(a, 0, result, 0, index);
        }

        if (index + 1 < a.length) {
            copy(a, index + 1, result, index, a.length - index - 1);
        }

        return result;
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from their
     * indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     *
     *
     * @param <T>
     *            the component type of the array
     * @param a
     * @param index
     *            the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     */
    public static double[] delete(final double[] a, final int index) {
        final double[] result = new double[a.length - 1];

        if (index > 0) {
            copy(a, 0, result, 0, index);
        }

        if (index + 1 < a.length) {
            copy(a, index + 1, result, index, a.length - index - 1);
        }

        return result;
    }

    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (subtracts one from their
     * indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     *
     *
     * @param <T>
     *            the component type of the array
     * @param a
     * @param index
     *            the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     */
    public static <T> T[] delete(final T[] a, final int index) {
        final T[] result = N.newArray(a.getClass().getComponentType(), a.length - 1);

        if (index > 0) {
            copy(a, 0, result, 0, index);
        }

        if (index + 1 < a.length) {
            copy(a, index + 1, result, index, a.length - index - 1);
        }

        return result;
    }

    /**
     * <p>
     * Removes the elements at the specified positions from the specified array.
     * All remaining elements are shifted to the left.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except those at the specified positions. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     *
     * <p>
     * If the input array is {@code null}, an IndexOutOfBoundsException will be
     * thrown, because in that case no valid index can be specified.
     * </p>
     *
     * <pre>
     * N.deleteAll([true, false, true], 0, 2) = [false]
     * N.removeAll([true, false, true], 1, 2) = [true]
     * </pre>
     *
     * @param a
     *            the array to remove the element from, may not be {@code null}
     * @param indices
     *            the positions of the elements to be removed
     * @return A new array containing the existing elements except those at the
     *         specified positions.
     */
    @SafeVarargs
    public static boolean[] deleteAll(final boolean[] a, int... indices) {
        if (N.isNullOrEmpty(indices)) {
            return a.clone();
        } else if (indices.length == 1) {
            return delete(a, indices[0]);
        }

        indices = indices.clone();
        N.sort(indices);
        final int lastIndex = indices[indices.length - 1];

        if (indices[0] < 0 || lastIndex >= a.length) {
            throw new IndexOutOfBoundsException("The specified indices are from: " + indices[0] + " to: " + lastIndex);
        }

        int diff = 1;
        for (int i = 1, len = indices.length; i < len; i++) {
            if (indices[i] == indices[i - 1]) {
                continue;
            }

            diff++;
        }

        final boolean[] result = new boolean[a.length - diff];
        int dest = 0;
        int len = 0;
        for (int i = 0, preIndex = -1; i < indices.length; preIndex = indices[i], i++) {
            if (indices[i] - preIndex > 1) {
                len = indices[i] - preIndex - 1;
                copy(a, preIndex + 1, result, dest, len);
                dest += len;
            }
        }

        if (lastIndex < a.length - 1) {
            len = a.length - lastIndex - 1;
            copy(a, lastIndex + 1, result, dest, len);
            dest += len;
        }

        return result;
    }

    /**
     * <p>
     * Removes the elements at the specified positions from the specified array.
     * All remaining elements are shifted to the left.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except those at the specified positions. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     *
     * <pre>
     * N.deleteAll([1], 0)             = []
     * N.deleteAll([2, 6], 0)          = [6]
     * N.deleteAll([2, 6], 0, 1)       = []
     * N.deleteAll([2, 6, 3], 1, 2)    = [2]
     * N.deleteAll([2, 6, 3], 0, 2)    = [6]
     * N.deleteAll([2, 6, 3], 0, 1, 2) = []
     * </pre>
     *
     * @param a
     * @param indices
     *            the positions of the elements to be removed
     * @return A new array containing the existing elements except those at the
     *         specified positions.
     */
    @SafeVarargs
    public static char[] deleteAll(final char[] a, int... indices) {
        if (N.isNullOrEmpty(indices)) {
            return a.clone();
        } else if (indices.length == 1) {
            return delete(a, indices[0]);
        }

        indices = indices.clone();
        N.sort(indices);
        final int lastIndex = indices[indices.length - 1];

        if (indices[0] < 0 || lastIndex >= a.length) {
            throw new IndexOutOfBoundsException("The specified indices are from: " + indices[0] + " to: " + lastIndex);
        }

        int diff = 1;
        for (int i = 1, len = indices.length; i < len; i++) {
            if (indices[i] == indices[i - 1]) {
                continue;
            }

            diff++;
        }

        final char[] result = new char[a.length - diff];
        int dest = 0;
        int len = 0;
        for (int i = 0, preIndex = -1; i < indices.length; preIndex = indices[i], i++) {
            if (indices[i] - preIndex > 1) {
                len = indices[i] - preIndex - 1;
                copy(a, preIndex + 1, result, dest, len);
                dest += len;
            }
        }

        if (lastIndex < a.length - 1) {
            len = a.length - lastIndex - 1;
            copy(a, lastIndex + 1, result, dest, len);
            dest += len;
        }

        return result;
    }

    /**
     * <p>
     * Removes the elements at the specified positions from the specified array.
     * All remaining elements are shifted to the left.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except those at the specified positions. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     *
     * <pre>
     * N.deleteAll([1], 0)             = []
     * N.deleteAll([2, 6], 0)          = [6]
     * N.deleteAll([2, 6], 0, 1)       = []
     * N.deleteAll([2, 6, 3], 1, 2)    = [2]
     * N.deleteAll([2, 6, 3], 0, 2)    = [6]
     * N.deleteAll([2, 6, 3], 0, 1, 2) = []
     * </pre>
     *
     * @param a
     * @param indices
     *            the positions of the elements to be removed
     * @return A new array containing the existing elements except those at the
     *         specified positions.
     *
     */
    @SafeVarargs
    public static byte[] deleteAll(final byte[] a, int... indices) {
        if (N.isNullOrEmpty(indices)) {
            return a.clone();
        } else if (indices.length == 1) {
            return delete(a, indices[0]);
        }

        indices = indices.clone();
        N.sort(indices);
        final int lastIndex = indices[indices.length - 1];

        if (indices[0] < 0 || lastIndex >= a.length) {
            throw new IndexOutOfBoundsException("The specified indices are from: " + indices[0] + " to: " + lastIndex);
        }

        int diff = 1;
        for (int i = 1, len = indices.length; i < len; i++) {
            if (indices[i] == indices[i - 1]) {
                continue;
            }

            diff++;
        }

        final byte[] result = new byte[a.length - diff];
        int dest = 0;
        int len = 0;
        for (int i = 0, preIndex = -1; i < indices.length; preIndex = indices[i], i++) {
            if (indices[i] - preIndex > 1) {
                len = indices[i] - preIndex - 1;
                copy(a, preIndex + 1, result, dest, len);
                dest += len;
            }
        }

        if (lastIndex < a.length - 1) {
            len = a.length - lastIndex - 1;
            copy(a, lastIndex + 1, result, dest, len);
            dest += len;
        }

        return result;
    }

    /**
     * <p>
     * Removes the elements at the specified positions from the specified array.
     * All remaining elements are shifted to the left.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except those at the specified positions. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     *
     * <pre>
     * N.deleteAll([1], 0)             = []
     * N.deleteAll([2, 6], 0)          = [6]
     * N.deleteAll([2, 6], 0, 1)       = []
     * N.deleteAll([2, 6, 3], 1, 2)    = [2]
     * N.deleteAll([2, 6, 3], 0, 2)    = [6]
     * N.deleteAll([2, 6, 3], 0, 1, 2) = []
     * </pre>
     *
     * @param a
     * @param indices
     *            the positions of the elements to be removed
     * @return A new array containing the existing elements except those at the
     *         specified positions.
     *
     */
    @SafeVarargs
    public static short[] deleteAll(final short[] a, int... indices) {
        if (N.isNullOrEmpty(indices)) {
            return a.clone();
        } else if (indices.length == 1) {
            return delete(a, indices[0]);
        }

        indices = indices.clone();
        N.sort(indices);
        final int lastIndex = indices[indices.length - 1];

        if (indices[0] < 0 || lastIndex >= a.length) {
            throw new IndexOutOfBoundsException("The specified indices are from: " + indices[0] + " to: " + lastIndex);
        }

        int diff = 1;
        for (int i = 1, len = indices.length; i < len; i++) {
            if (indices[i] == indices[i - 1]) {
                continue;
            }

            diff++;
        }

        final short[] result = new short[a.length - diff];
        int dest = 0;
        int len = 0;
        for (int i = 0, preIndex = -1; i < indices.length; preIndex = indices[i], i++) {
            if (indices[i] - preIndex > 1) {
                len = indices[i] - preIndex - 1;
                copy(a, preIndex + 1, result, dest, len);
                dest += len;
            }
        }

        if (lastIndex < a.length - 1) {
            len = a.length - lastIndex - 1;
            copy(a, lastIndex + 1, result, dest, len);
            dest += len;
        }

        return result;
    }

    /**
     * <p>
     * Removes the elements at the specified positions from the specified array.
     * All remaining elements are shifted to the left.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except those at the specified positions. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     *
     * <pre>
     * N.deleteAll([1], 0)             = []
     * N.deleteAll([2, 6], 0)          = [6]
     * N.deleteAll([2, 6], 0, 1)       = []
     * N.deleteAll([2, 6, 3], 1, 2)    = [2]
     * N.deleteAll([2, 6, 3], 0, 2)    = [6]
     * N.deleteAll([2, 6, 3], 0, 1, 2) = []
     * </pre>
     *
     * @param a
     * @param indices
     *            the positions of the elements to be removed
     * @return A new array containing the existing elements except those at the
     *         specified positions.
     * @throws IndexOutOfBoundsException
     *             if any index is out of range (index &lt; 0 || index &gt;=
     *             array.length), or if the array is {@code null}.
     *
     */
    @SafeVarargs
    public static int[] deleteAll(final int[] a, int... indices) {
        if (N.isNullOrEmpty(indices)) {
            return a.clone();
        } else if (indices.length == 1) {
            return delete(a, indices[0]);
        }

        indices = indices.clone();
        N.sort(indices);
        final int lastIndex = indices[indices.length - 1];

        if (indices[0] < 0 || lastIndex >= a.length) {
            throw new IndexOutOfBoundsException("The specified indices are from: " + indices[0] + " to: " + lastIndex);
        }

        int diff = 1;
        for (int i = 1, len = indices.length; i < len; i++) {
            if (indices[i] == indices[i - 1]) {
                continue;
            }

            diff++;
        }

        final int[] result = new int[a.length - diff];
        int dest = 0;
        int len = 0;
        for (int i = 0, preIndex = -1; i < indices.length; preIndex = indices[i], i++) {
            if (indices[i] - preIndex > 1) {
                len = indices[i] - preIndex - 1;
                copy(a, preIndex + 1, result, dest, len);
                dest += len;
            }
        }

        if (lastIndex < a.length - 1) {
            len = a.length - lastIndex - 1;
            copy(a, lastIndex + 1, result, dest, len);
            dest += len;
        }

        return result;
    }

    /**
     * <p>
     * Removes the elements at the specified positions from the specified array.
     * All remaining elements are shifted to the left.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except those at the specified positions. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     *
     * <pre>
     * N.deleteAll([1], 0)             = []
     * N.deleteAll([2, 6], 0)          = [6]
     * N.deleteAll([2, 6], 0, 1)       = []
     * N.deleteAll([2, 6, 3], 1, 2)    = [2]
     * N.deleteAll([2, 6, 3], 0, 2)    = [6]
     * N.deleteAll([2, 6, 3], 0, 1, 2) = []
     * </pre>
     *
     * @param a
     *            the array to remove the element from, may not be {@code null}
     * @param indices
     *            the positions of the elements to be removed
     * @return A new array containing the existing elements except those at the
     *         specified positions.
     */
    @SafeVarargs
    public static long[] deleteAll(final long[] a, int... indices) {
        if (N.isNullOrEmpty(indices)) {
            return a.clone();
        } else if (indices.length == 1) {
            return delete(a, indices[0]);
        }

        indices = indices.clone();
        N.sort(indices);
        final int lastIndex = indices[indices.length - 1];

        if (indices[0] < 0 || lastIndex >= a.length) {
            throw new IndexOutOfBoundsException("The specified indices are from: " + indices[0] + " to: " + lastIndex);
        }

        int diff = 1;
        for (int i = 1, len = indices.length; i < len; i++) {
            if (indices[i] == indices[i - 1]) {
                continue;
            }

            diff++;
        }

        final long[] result = new long[a.length - diff];
        int dest = 0;
        int len = 0;
        for (int i = 0, preIndex = -1; i < indices.length; preIndex = indices[i], i++) {
            if (indices[i] - preIndex > 1) {
                len = indices[i] - preIndex - 1;
                copy(a, preIndex + 1, result, dest, len);
                dest += len;
            }
        }

        if (lastIndex < a.length - 1) {
            len = a.length - lastIndex - 1;
            copy(a, lastIndex + 1, result, dest, len);
            dest += len;
        }

        return result;
    }

    /**
     * <p>
     * Removes the elements at the specified positions from the specified array.
     * All remaining elements are shifted to the left.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except those at the specified positions. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     *
     * <pre>
     * N.deleteAll([1], 0)             = []
     * N.deleteAll([2, 6], 0)          = [6]
     * N.deleteAll([2, 6], 0, 1)       = []
     * N.deleteAll([2, 6, 3], 1, 2)    = [2]
     * N.deleteAll([2, 6, 3], 0, 2)    = [6]
     * N.deleteAll([2, 6, 3], 0, 1, 2) = []
     * </pre>
     *
     * @param a
     * @param indices
     *            the positions of the elements to be removed
     * @return A new array containing the existing elements except those at the
     *         specified positions.
     *
     */
    @SafeVarargs
    public static float[] deleteAll(final float[] a, int... indices) {
        if (N.isNullOrEmpty(indices)) {
            return a.clone();
        } else if (indices.length == 1) {
            return delete(a, indices[0]);
        }

        indices = indices.clone();
        N.sort(indices);
        final int lastIndex = indices[indices.length - 1];

        if (indices[0] < 0 || lastIndex >= a.length) {
            throw new IndexOutOfBoundsException("The specified indices are from: " + indices[0] + " to: " + lastIndex);
        }

        int diff = 1;
        for (int i = 1, len = indices.length; i < len; i++) {
            if (indices[i] == indices[i - 1]) {
                continue;
            }

            diff++;
        }

        final float[] result = new float[a.length - diff];
        int dest = 0;
        int len = 0;
        for (int i = 0, preIndex = -1; i < indices.length; preIndex = indices[i], i++) {
            if (indices[i] - preIndex > 1) {
                len = indices[i] - preIndex - 1;
                copy(a, preIndex + 1, result, dest, len);
                dest += len;
            }
        }

        if (lastIndex < a.length - 1) {
            len = a.length - lastIndex - 1;
            copy(a, lastIndex + 1, result, dest, len);
            dest += len;
        }

        return result;
    }

    /**
     * <p>
     * Removes the elements at the specified positions from the specified array.
     * All remaining elements are shifted to the left.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except those at the specified positions. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     *
     * <pre>
     * N.deleteAll([1], 0)             = []
     * N.deleteAll([2, 6], 0)          = [6]
     * N.deleteAll([2, 6], 0, 1)       = []
     * N.deleteAll([2, 6, 3], 1, 2)    = [2]
     * N.deleteAll([2, 6, 3], 0, 2)    = [6]
     * N.deleteAll([2, 6, 3], 0, 1, 2) = []
     * </pre>
     *
     * @param a
     * @param indices
     *            the positions of the elements to be removed
     * @return A new array containing the existing elements except those at the
     *         specified positions.
     *
     */
    @SafeVarargs
    public static double[] deleteAll(final double[] a, int... indices) {
        if (N.isNullOrEmpty(indices)) {
            return a.clone();
        } else if (indices.length == 1) {
            return delete(a, indices[0]);
        }

        indices = indices.clone();
        N.sort(indices);
        final int lastIndex = indices[indices.length - 1];

        if (indices[0] < 0 || lastIndex >= a.length) {
            throw new IndexOutOfBoundsException("The specified indices are from: " + indices[0] + " to: " + lastIndex);
        }

        int diff = 1;
        for (int i = 1, len = indices.length; i < len; i++) {
            if (indices[i] == indices[i - 1]) {
                continue;
            }

            diff++;
        }

        final double[] result = new double[a.length - diff];
        int dest = 0;
        int len = 0;
        for (int i = 0, preIndex = -1; i < indices.length; preIndex = indices[i], i++) {
            if (indices[i] - preIndex > 1) {
                len = indices[i] - preIndex - 1;
                copy(a, preIndex + 1, result, dest, len);
                dest += len;
            }
        }

        if (lastIndex < a.length - 1) {
            len = a.length - lastIndex - 1;
            copy(a, lastIndex + 1, result, dest, len);
            dest += len;
        }

        return result;
    }

    /**
     * <p>
     * Removes the elements at the specified positions from the specified array.
     * All remaining elements are shifted to the left.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except those at the specified positions. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     *
     *
     * <pre>
     * N.deleteAll(["a", "b", "c"], 0, 2) = ["b"]
     * N.deleteAll(["a", "b", "c"], 1, 2) = ["a"]
     * </pre>
     *
     * @param <T>
     *            the component type of the array
     * @param a
     * @param indices
     *            the positions of the elements to be removed
     * @return A new array containing the existing elements except those at the
     *         specified positions.
     * @throws NullPointerException if the specified <code>a</code> is <code>null</code>.
     */
    @SafeVarargs
    public static <T> T[] deleteAll(final T[] a, int... indices) {
        N.checkArgNotNull(a, "a");

        if (N.isNullOrEmpty(indices)) {
            return a.clone();
        } else if (indices.length == 1) {
            return delete(a, indices[0]);
        }

        indices = indices.clone();
        N.sort(indices);
        return deleteAllBySortedIndices(a, indices);
    }

    private static <T> T[] deleteAllBySortedIndices(final T[] a, int... indices) {
        final int lastIndex = indices[indices.length - 1];

        if (indices[0] < 0 || lastIndex >= a.length) {
            throw new IndexOutOfBoundsException("The specified indices are from: " + indices[0] + " to: " + lastIndex);
        }

        int diff = 1;
        for (int i = 1, len = indices.length; i < len; i++) {
            if (indices[i] == indices[i - 1]) {
                continue;
            }

            diff++;
        }

        final T[] result = N.newArray(a.getClass().getComponentType(), a.length - diff);
        int dest = 0;
        int len = 0;
        for (int i = 0, preIndex = -1; i < indices.length; preIndex = indices[i], i++) {
            if (indices[i] - preIndex > 1) {
                len = indices[i] - preIndex - 1;
                copy(a, preIndex + 1, result, dest, len);
                dest += len;
            }
        }

        if (lastIndex < a.length - 1) {
            len = a.length - lastIndex - 1;
            copy(a, lastIndex + 1, result, dest, len);
            dest += len;
        }

        return result;
    }

    /**
     *
     * Removes the elements at the specified positions from the specified List.
     *
     * @param list
     * @param indices
     * @return
     */
    @SuppressWarnings("rawtypes")
    @SafeVarargs
    public static boolean deleteAll(final List<?> list, int... indices) {
        N.checkArgNotNull(list);

        if (N.isNullOrEmpty(indices)) {
            return false;
        } else if (indices.length == 1) {
            list.remove(indices[0]);
            return true;
        }

        indices = indices.clone();
        sort(indices);

        if (indices[0] < 0 || indices[indices.length - 1] >= list.size()) {
            throw new IndexOutOfBoundsException("The specified indices are from: " + indices[0] + " to: " + indices[indices.length - 1]);
        }

        if (list instanceof LinkedList) {
            final Iterator<?> iterator = list.iterator();

            int idx = -1;
            for (int i = 0, len = indices.length; i < len; i++) {
                if (i > 0 && indices[i] == indices[i - 1]) {
                    continue;
                }

                while (idx < indices[i]) {
                    idx++;
                    iterator.next();
                }

                iterator.remove();
            }
        } else {
            final Object[] a = list.toArray();
            final Object[] res = deleteAllBySortedIndices(a, indices);
            list.clear();
            list.addAll((List) Arrays.asList(res));
        }

        return true;
    }

    /**
     * Deletes the values from {@code fromIndex} to {@code toIndex}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return a new array
     */
    public static boolean[] deleteRange(final boolean[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, a.length);

        if (fromIndex == toIndex) {
            return a.clone();
        }

        final boolean[] b = new boolean[a.length - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            N.copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < a.length) {
            N.copy(a, toIndex, b, fromIndex, a.length - toIndex);
        }

        return b;
    }

    /**
     * Deletes the values from {@code fromIndex} to {@code toIndex}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return a new array
     */
    public static char[] deleteRange(final char[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, a.length);

        if (fromIndex == toIndex) {
            return a.clone();
        }

        final char[] b = new char[a.length - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            N.copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < a.length) {
            N.copy(a, toIndex, b, fromIndex, a.length - toIndex);
        }

        return b;
    }

    /**
     * Deletes the values from {@code fromIndex} to {@code toIndex}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return a new array
     */
    public static byte[] deleteRange(final byte[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, a.length);

        if (fromIndex == toIndex) {
            return a.clone();
        }

        final byte[] b = new byte[a.length - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            N.copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < a.length) {
            N.copy(a, toIndex, b, fromIndex, a.length - toIndex);
        }

        return b;
    }

    /**
     * Deletes the values from {@code fromIndex} to {@code toIndex}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return a new array
     */
    public static short[] deleteRange(final short[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, a.length);

        if (fromIndex == toIndex) {
            return a.clone();
        }

        final short[] b = new short[a.length - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            N.copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < a.length) {
            N.copy(a, toIndex, b, fromIndex, a.length - toIndex);
        }

        return b;
    }

    /**
     * Deletes the values from {@code fromIndex} to {@code toIndex}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return a new array
     */
    public static int[] deleteRange(final int[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, a.length);

        if (fromIndex == toIndex) {
            return a.clone();
        }

        final int[] b = new int[a.length - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            N.copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < a.length) {
            N.copy(a, toIndex, b, fromIndex, a.length - toIndex);
        }

        return b;
    }

    /**
     * Deletes the values from {@code fromIndex} to {@code toIndex}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return a new array
     */
    public static long[] deleteRange(final long[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, a.length);

        if (fromIndex == toIndex) {
            return a.clone();
        }

        final long[] b = new long[a.length - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            N.copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < a.length) {
            N.copy(a, toIndex, b, fromIndex, a.length - toIndex);
        }

        return b;
    }

    /**
     * Deletes the values from {@code fromIndex} to {@code toIndex}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return a new array
     */
    public static float[] deleteRange(final float[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, a.length);

        if (fromIndex == toIndex) {
            return a.clone();
        }

        final float[] b = new float[a.length - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            N.copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < a.length) {
            N.copy(a, toIndex, b, fromIndex, a.length - toIndex);
        }

        return b;
    }

    /**
     * Deletes the values from {@code fromIndex} to {@code toIndex}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return a new array
     */
    public static double[] deleteRange(final double[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, a.length);

        if (fromIndex == toIndex) {
            return a.clone();
        }

        final double[] b = new double[a.length - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            N.copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < a.length) {
            N.copy(a, toIndex, b, fromIndex, a.length - toIndex);
        }

        return b;
    }

    /**
     * Deletes the values from {@code fromIndex} to {@code toIndex}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return a new array
     */
    public static <T> T[] deleteRange(final T[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, a.length);

        if (fromIndex == toIndex) {
            return a.clone();
        }

        final T[] b = Array.newInstance(a.getClass().getComponentType(), a.length - (toIndex - fromIndex));

        if (fromIndex > 0) {
            N.copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < a.length) {
            N.copy(a, toIndex, b, fromIndex, a.length - toIndex);
        }

        return b;
    }

    /**
     * Returns {@code true} if the {@code List} is updated when {@code fromIndex < toIndex}, otherwise {@code false} is returned when {@code fromIndex == toIndex}.
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> boolean deleteRange(final List<T> c, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, c.size());

        if (fromIndex == toIndex) {
            return false;
        }

        if (c instanceof LinkedList || toIndex - fromIndex <= 3) {
            c.subList(fromIndex, toIndex).clear();
        } else {
            if (isListElementDataFieldGettable && isListElementDataFieldSettable && listElementDataField != null && c instanceof ArrayList) {
                T[] array = null;

                try {
                    array = (T[]) listElementDataField.get(c);
                    N.copy(array, toIndex, array, fromIndex, c.size() - toIndex);
                    listSizeField.set(c, c.size() - (toIndex - fromIndex));
                    // update modCount
                    c.add(null);
                    c.remove(c.size() - 1);
                    return true;
                } catch (Throwable e) {
                    // ignore;
                    isListElementDataFieldSettable = false;
                }
            }

            final List<T> tmp = new ArrayList<>(c.size() - (toIndex - fromIndex));

            if (fromIndex > 0) {
                tmp.addAll(c.subList(0, fromIndex));
            }

            if (toIndex < c.size()) {
                tmp.addAll(c.subList(toIndex, c.size()));
            }

            c.clear();
            c.addAll(tmp);
        }

        return true;
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param element
     *            the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static boolean[] remove(final boolean[] a, final boolean element) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_BOOLEAN_ARRAY;
        }

        int index = indexOf(a, 0, element);

        return index == INDEX_NOT_FOUND ? a.clone() : delete(a, index);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param element
     *            the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static char[] remove(final char[] a, final char element) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_CHAR_ARRAY;
        }

        int index = indexOf(a, 0, element);

        return index == INDEX_NOT_FOUND ? a.clone() : delete(a, index);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param element
     *            the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static byte[] remove(final byte[] a, final byte element) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_BYTE_ARRAY;
        }

        int index = indexOf(a, 0, element);

        return index == INDEX_NOT_FOUND ? a.clone() : delete(a, index);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param element
     *            the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static short[] remove(final short[] a, final short element) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_SHORT_ARRAY;
        }

        int index = indexOf(a, 0, element);

        return index == INDEX_NOT_FOUND ? a.clone() : delete(a, index);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param element
     *            the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static int[] remove(final int[] a, final int element) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_INT_ARRAY;
        }

        int index = indexOf(a, 0, element);

        return index == INDEX_NOT_FOUND ? a.clone() : delete(a, index);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param element
     *            the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static long[] remove(final long[] a, final long element) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_LONG_ARRAY;
        }

        int index = indexOf(a, 0, element);

        return index == INDEX_NOT_FOUND ? a.clone() : delete(a, index);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param element
     *            the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static float[] remove(final float[] a, final float element) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_FLOAT_ARRAY;
        }

        int index = indexOf(a, 0, element);

        return index == INDEX_NOT_FOUND ? a.clone() : delete(a, index);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param element
     *            the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static double[] remove(final double[] a, final double element) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_DOUBLE_ARRAY;
        }

        int index = indexOf(a, 0, element);

        return index == INDEX_NOT_FOUND ? a.clone() : delete(a, index);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param element
     *            the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static <T> T[] remove(final T[] a, final Object element) {
        if (N.isNullOrEmpty(a)) {
            return a;
        }

        int index = indexOf(a, 0, element);

        return index == INDEX_NOT_FOUND ? a.clone() : delete(a, index);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * collection. If the collection doesn't contains such an element, no
     * elements are removed from the collection.
     * </p>
     *
     * @param c
     * @param element the element to be removed
     * @return <tt>true</tt> if this collection changed as a result of the call
     */
    static boolean remove(final Collection<?> c, final Object element) {
        if (N.isNullOrEmpty(c)) {
            return false;
        }

        return c.remove(element);
    }

    /**
     * Removes all the occurrences of the specified element from the specified
     * array. All subsequent elements are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     *
     * @param a
     * @param element
     * @return A new array containing the existing elements except the
     *         occurrences of the specified element.
     */
    public static boolean[] removeAllOccurrences(final boolean[] a, final boolean element) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_BOOLEAN_ARRAY;
        }

        final boolean[] copy = a.clone();
        int idx = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == element) {
                continue;
            }

            copy[idx++] = a[i];
        }

        return idx == copy.length ? copy : N.copyOfRange(copy, 0, idx);
    }

    /**
     * Removes all the occurrences of the specified element from the specified
     * array. All subsequent elements are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     *
     * @param a
     * @param element
     * @return A new array containing the existing elements except the
     *         occurrences of the specified element.
     */
    public static char[] removeAllOccurrences(final char[] a, final char element) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_CHAR_ARRAY;
        }

        final char[] copy = a.clone();
        int idx = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == element) {
                continue;
            }

            copy[idx++] = a[i];
        }

        return idx == copy.length ? copy : N.copyOfRange(copy, 0, idx);
    }

    /**
     * Removes all the occurrences of the specified element from the specified
     * array. All subsequent elements are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     *
     * @param a
     * @param element
     * @return A new array containing the existing elements except the
     *         occurrences of the specified element.
     */
    public static byte[] removeAllOccurrences(final byte[] a, final byte element) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_BYTE_ARRAY;
        }

        final byte[] copy = a.clone();
        int idx = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == element) {
                continue;
            }

            copy[idx++] = a[i];
        }

        return idx == copy.length ? copy : N.copyOfRange(copy, 0, idx);
    }

    /**
     * Removes all the occurrences of the specified element from the specified
     * array. All subsequent elements are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     *
     * @param a
     * @param element
     * @return A new array containing the existing elements except the
     *         occurrences of the specified element.
     */
    public static short[] removeAllOccurrences(final short[] a, final short element) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_SHORT_ARRAY;
        }

        final short[] copy = a.clone();
        int idx = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == element) {
                continue;
            }

            copy[idx++] = a[i];
        }

        return idx == copy.length ? copy : N.copyOfRange(copy, 0, idx);
    }

    /**
     * Removes all the occurrences of the specified element from the specified
     * array. All subsequent elements are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     *
     * @param a
     * @param element
     * @return A new array containing the existing elements except the
     *         occurrences of the specified element.
     */
    public static int[] removeAllOccurrences(final int[] a, final int element) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_INT_ARRAY;
        }

        final int[] copy = a.clone();
        int idx = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == element) {
                continue;
            }

            copy[idx++] = a[i];
        }

        return idx == copy.length ? copy : N.copyOfRange(copy, 0, idx);
    }

    /**
     * Removes all the occurrences of the specified element from the specified
     * array. All subsequent elements are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     *
     * @param a
     * @param element
     * @return A new array containing the existing elements except the
     *         occurrences of the specified element.
     */
    public static long[] removeAllOccurrences(final long[] a, final long element) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_LONG_ARRAY;
        }

        final long[] copy = a.clone();
        int idx = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == element) {
                continue;
            }

            copy[idx++] = a[i];
        }

        return idx == copy.length ? copy : N.copyOfRange(copy, 0, idx);
    }

    /**
     * Removes all the occurrences of the specified element from the specified
     * array. All subsequent elements are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     *
     * @param a
     * @param element
     * @return A new array containing the existing elements except the
     *         occurrences of the specified element.
     */
    public static float[] removeAllOccurrences(final float[] a, final float element) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_FLOAT_ARRAY;
        }

        final float[] copy = a.clone();
        int idx = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (equals(a[i], element)) {
                continue;
            }

            copy[idx++] = a[i];
        }

        return idx == copy.length ? copy : N.copyOfRange(copy, 0, idx);
    }

    /**
     * Removes all the occurrences of the specified element from the specified
     * array. All subsequent elements are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     *
     * @param a
     * @param element
     * @return A new array containing the existing elements except the
     *         occurrences of the specified element.
     */
    public static double[] removeAllOccurrences(final double[] a, final double element) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_DOUBLE_ARRAY;
        }

        final double[] copy = a.clone();
        int idx = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (equals(a[i], element)) {
                continue;
            }

            copy[idx++] = a[i];
        }

        return idx == copy.length ? copy : N.copyOfRange(copy, 0, idx);
    }

    /**
     * Removes all the occurrences of the specified element from the specified
     * array. All subsequent elements are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     *
     * @param a
     * @param element
     * @return A new array containing the existing elements except the
     *         occurrences of the specified element.
     */
    public static <T> T[] removeAllOccurrences(final T[] a, final Object element) {
        if (N.isNullOrEmpty(a)) {
            return a;
        }

        final T[] copy = a.clone();
        int idx = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (equals(a[i], element)) {
                continue;
            }

            copy[idx++] = a[i];
        }

        return idx == copy.length ? copy : N.copyOfRange(copy, 0, idx);
    }

    public static boolean removeAllOccurrences(Collection<?> c, final Object element) {
        if (N.isNullOrEmpty(c)) {
            return false;
        }

        return Iterables.removeAll(c, N.asSet(element));
    }

    /**
     * Returns a new array with removes all the occurrences of specified elements from <code>a</code>
     *
     * @param a
     * @param elements
     * @return
     * @see Collection#removeAll(Collection)
     */
    @SafeVarargs
    public static boolean[] removeAll(final boolean[] a, final boolean... elements) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_BOOLEAN_ARRAY;
        } else if (N.isNullOrEmpty(elements)) {
            return a.clone();
        } else if (elements.length == 1) {
            return removeAllOccurrences(a, elements[0]);
        }

        final BooleanList list = BooleanList.of(a.clone());
        list.removeAll(BooleanList.of(elements));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elements from <code>a</code>
     *
     * @param a
     * @param elements
     * @return
     * @see Collection#removeAll(Collection)
     */
    @SafeVarargs
    public static char[] removeAll(final char[] a, final char... elements) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_CHAR_ARRAY;
        } else if (N.isNullOrEmpty(elements)) {
            return a.clone();
        } else if (elements.length == 1) {
            return removeAllOccurrences(a, elements[0]);
        }

        final CharList list = CharList.of(a.clone());
        list.removeAll(CharList.of(elements));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elements from <code>a</code>
     *
     * @param a
     * @param elements
     * @return
     * @see Collection#removeAll(Collection)
     */
    @SafeVarargs
    public static byte[] removeAll(final byte[] a, final byte... elements) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_BYTE_ARRAY;
        } else if (N.isNullOrEmpty(elements)) {
            return a.clone();
        } else if (elements.length == 1) {
            return removeAllOccurrences(a, elements[0]);
        }

        final ByteList list = ByteList.of(a.clone());
        list.removeAll(ByteList.of(elements));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elements from <code>a</code>
     *
     * @param a
     * @param elements
     * @return
     * @see Collection#removeAll(Collection)
     */
    @SafeVarargs
    public static short[] removeAll(final short[] a, final short... elements) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_SHORT_ARRAY;
        } else if (N.isNullOrEmpty(elements)) {
            return a.clone();
        } else if (elements.length == 1) {
            return removeAllOccurrences(a, elements[0]);
        }

        final ShortList list = ShortList.of(a.clone());
        list.removeAll(ShortList.of(elements));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elements from <code>a</code>
     *
     * @param a
     * @param elements
     * @return
     * @see Collection#removeAll(Collection)
     */
    @SafeVarargs
    public static int[] removeAll(final int[] a, final int... elements) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_INT_ARRAY;
        } else if (N.isNullOrEmpty(elements)) {
            return a.clone();
        } else if (elements.length == 1) {
            return removeAllOccurrences(a, elements[0]);
        }

        final IntList list = IntList.of(a.clone());
        list.removeAll(IntList.of(elements));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elements from <code>a</code>
     *
     * @param a
     * @param elements
     * @return
     * @see Collection#removeAll(Collection)
     */
    @SafeVarargs
    public static long[] removeAll(final long[] a, final long... elements) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_LONG_ARRAY;
        } else if (N.isNullOrEmpty(elements)) {
            return a.clone();
        } else if (elements.length == 1) {
            return removeAllOccurrences(a, elements[0]);
        }

        final LongList list = LongList.of(a.clone());
        list.removeAll(LongList.of(elements));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elements from <code>a</code>
     *
     * @param a
     * @param elements
     * @return
     * @see Collection#removeAll(Collection)
     */
    @SafeVarargs
    public static float[] removeAll(final float[] a, final float... elements) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_FLOAT_ARRAY;
        } else if (N.isNullOrEmpty(elements)) {
            return a.clone();
        } else if (elements.length == 1) {
            return removeAllOccurrences(a, elements[0]);
        }

        final FloatList list = FloatList.of(a.clone());
        list.removeAll(FloatList.of(elements));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elements from <code>a</code>
     *
     * @param a
     * @param elements
     * @return
     * @see Collection#removeAll(Collection)
     */
    @SafeVarargs
    public static double[] removeAll(final double[] a, final double... elements) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_DOUBLE_ARRAY;
        } else if (N.isNullOrEmpty(elements)) {
            return a.clone();
        } else if (elements.length == 1) {
            return removeAllOccurrences(a, elements[0]);
        }

        final DoubleList list = DoubleList.of(a.clone());
        list.removeAll(DoubleList.of(elements));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elements from <code>a</code>
     *
     * @param a
     * @param elements
     * @return
     * @see Collection#removeAll(Collection)
     */
    @SafeVarargs
    public static <T> T[] removeAll(final T[] a, final Object... elements) {
        if (N.isNullOrEmpty(a)) {
            return a;
        } else if (N.isNullOrEmpty(elements)) {
            return a.clone();
        } else if (elements.length == 1) {
            return removeAllOccurrences(a, elements[0]);
        }

        final Set<Object> set = N.asSet(elements);
        final List<T> result = new ArrayList<>();

        for (T e : a) {
            if (!set.contains(e)) {
                result.add(e);
            }
        }

        return result.toArray((T[]) N.newArray(a.getClass().getComponentType(), result.size()));
    }

    @SafeVarargs
    public static boolean removeAll(final Collection<?> c, final Object... elements) {
        if (N.isNullOrEmpty(c) || N.isNullOrEmpty(elements)) {
            return false;
        } else {
            return Iterables.removeAll(c, N.asSet(elements));
        }
    }

    public static boolean[] removeDuplicates(final boolean[] a) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_BOOLEAN_ARRAY;
        }

        return removeDuplicates(a, false);
    }

    public static boolean[] removeDuplicates(final boolean[] a, final boolean isSorted) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_BOOLEAN_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    public static boolean[] removeDuplicates(final boolean[] a, final int from, final int to, final boolean isSorted) {
        checkFromToIndex(from, to, len(a));

        if (N.isNullOrEmpty(a) && from == 0 && to == 0) {
            return N.EMPTY_BOOLEAN_ARRAY;
        } else if (to - from <= 1) {
            return N.copyOfRange(a, from, to);
        }

        final Boolean[] b = new Boolean[2];

        for (int i = from; i < to; i++) {
            if (b[0] == null) {
                b[0] = a[i];
            } else if (b[0].booleanValue() != a[i]) {
                b[1] = a[i];
                break;
            }
        }

        return b[1] == null ? new boolean[] { b[0].booleanValue() } : new boolean[] { b[0].booleanValue(), b[1].booleanValue() };
    }

    public static char[] removeDuplicates(final char[] a) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_CHAR_ARRAY;
        }

        return removeDuplicates(a, false);
    }

    public static char[] removeDuplicates(final char[] a, final boolean isSorted) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_CHAR_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    public static char[] removeDuplicates(final char[] a, final int from, final int to, final boolean isSorted) {
        checkFromToIndex(from, to, len(a));

        if (N.isNullOrEmpty(a) && from == 0 && to == 0) {
            return N.EMPTY_CHAR_ARRAY;
        } else if (to - from <= 1) {
            return N.copyOfRange(a, from, to);
        }

        if (isSorted) {
            final char[] b = (from == 0 && to == a.length) ? a.clone() : N.copyOfRange(a, from, to);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (b[i] == b[i - 1]) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : N.copyOfRange(b, 0, idx);
        } else {
            final Set<Character> set = new LinkedHashSet<>(N.initHashCapacity(a.length));

            for (int i = from; i < to; i++) {
                set.add(a[i]);
            }

            if (set.size() == to - from) {
                return (from == 0 && to == a.length) ? a.clone() : N.copyOfRange(a, from, to);
            } else {
                final char[] result = new char[set.size()];
                int i = 0;

                for (char e : set) {
                    result[i++] = e;
                }

                return result;
            }
        }
    }

    public static byte[] removeDuplicates(final byte[] a) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_BYTE_ARRAY;
        }

        return removeDuplicates(a, false);
    }

    public static byte[] removeDuplicates(final byte[] a, final boolean isSorted) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_BYTE_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    public static byte[] removeDuplicates(final byte[] a, final int from, final int to, final boolean isSorted) {
        checkFromToIndex(from, to, len(a));

        if (N.isNullOrEmpty(a) && from == 0 && to == 0) {
            return N.EMPTY_BYTE_ARRAY;
        } else if (to - from <= 1) {
            return N.copyOfRange(a, from, to);
        }

        if (isSorted) {
            final byte[] b = (from == 0 && to == a.length) ? a.clone() : N.copyOfRange(a, from, to);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (b[i] == b[i - 1]) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : N.copyOfRange(b, 0, idx);
        } else {
            final Set<Byte> set = new LinkedHashSet<>(N.initHashCapacity(a.length));

            for (int i = from; i < to; i++) {
                set.add(a[i]);
            }

            if (set.size() == to - from) {
                return (from == 0 && to == a.length) ? a.clone() : N.copyOfRange(a, from, to);
            } else {
                final byte[] result = new byte[set.size()];
                int i = 0;

                for (byte e : set) {
                    result[i++] = e;
                }

                return result;
            }
        }
    }

    public static short[] removeDuplicates(final short[] a) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_SHORT_ARRAY;
        }

        return removeDuplicates(a, false);
    }

    public static short[] removeDuplicates(final short[] a, final boolean isSorted) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_SHORT_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    public static short[] removeDuplicates(final short[] a, final int from, final int to, final boolean isSorted) {
        checkFromToIndex(from, to, len(a));

        if (N.isNullOrEmpty(a) && from == 0 && to == 0) {
            return N.EMPTY_SHORT_ARRAY;
        } else if (to - from <= 1) {
            return N.copyOfRange(a, from, to);
        }

        if (isSorted) {
            final short[] b = (from == 0 && to == a.length) ? a.clone() : N.copyOfRange(a, from, to);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (b[i] == b[i - 1]) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : N.copyOfRange(b, 0, idx);
        } else {
            final Set<Short> set = new LinkedHashSet<>(N.initHashCapacity(a.length));

            for (int i = from; i < to; i++) {
                set.add(a[i]);
            }

            if (set.size() == to - from) {
                return (from == 0 && to == a.length) ? a.clone() : N.copyOfRange(a, from, to);
            } else {
                final short[] result = new short[set.size()];
                int i = 0;

                for (short e : set) {
                    result[i++] = e;
                }

                return result;
            }
        }
    }

    public static int[] removeDuplicates(final int[] a) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_INT_ARRAY;
        }

        return removeDuplicates(a, false);
    }

    public static int[] removeDuplicates(final int[] a, final boolean isSorted) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_INT_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    public static int[] removeDuplicates(final int[] a, final int from, final int to, final boolean isSorted) {
        checkFromToIndex(from, to, len(a));

        if (N.isNullOrEmpty(a) && from == 0 && to == 0) {
            return N.EMPTY_INT_ARRAY;
        } else if (to - from <= 1) {
            return N.copyOfRange(a, from, to);
        }

        if (isSorted) {
            final int[] b = (from == 0 && to == a.length) ? a.clone() : N.copyOfRange(a, from, to);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (b[i] == b[i - 1]) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : N.copyOfRange(b, 0, idx);
        } else {

            final Set<Integer> set = new LinkedHashSet<>(N.initHashCapacity(a.length));

            for (int i = from; i < to; i++) {
                set.add(a[i]);
            }

            if (set.size() == to - from) {
                return (from == 0 && to == a.length) ? a.clone() : N.copyOfRange(a, from, to);
            } else {
                final int[] result = new int[set.size()];
                int i = 0;

                for (int e : set) {
                    result[i++] = e;
                }

                return result;
            }
        }
    }

    public static long[] removeDuplicates(final long[] a) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_LONG_ARRAY;
        }

        return removeDuplicates(a, false);
    }

    public static long[] removeDuplicates(final long[] a, final boolean isSorted) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_LONG_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    public static long[] removeDuplicates(final long[] a, final int from, final int to, final boolean isSorted) {
        checkFromToIndex(from, to, len(a));

        if (N.isNullOrEmpty(a) && from == 0 && to == 0) {
            return N.EMPTY_LONG_ARRAY;
        } else if (to - from <= 1) {
            return N.copyOfRange(a, from, to);
        }

        if (isSorted) {
            final long[] b = (from == 0 && to == a.length) ? a.clone() : N.copyOfRange(a, from, to);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (b[i] == b[i - 1]) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : N.copyOfRange(b, 0, idx);
        } else {
            final Set<Long> set = new LinkedHashSet<>(N.initHashCapacity(a.length));

            for (int i = from; i < to; i++) {
                set.add(a[i]);
            }

            if (set.size() == to - from) {
                return (from == 0 && to == a.length) ? a.clone() : N.copyOfRange(a, from, to);
            } else {
                final long[] result = new long[set.size()];
                int i = 0;

                for (long e : set) {
                    result[i++] = e;
                }

                return result;
            }
        }
    }

    public static float[] removeDuplicates(final float[] a) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_FLOAT_ARRAY;
        }

        return removeDuplicates(a, false);
    }

    public static float[] removeDuplicates(final float[] a, final boolean isSorted) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_FLOAT_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    public static float[] removeDuplicates(final float[] a, final int from, final int to, final boolean isSorted) {
        checkFromToIndex(from, to, len(a));

        if (N.isNullOrEmpty(a) && from == 0 && to == 0) {
            return N.EMPTY_FLOAT_ARRAY;
        } else if (to - from <= 1) {
            return N.copyOfRange(a, from, to);
        }

        if (isSorted) {
            final float[] b = (from == 0 && to == a.length) ? a.clone() : N.copyOfRange(a, from, to);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (N.equals(b[i], b[i - 1])) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : N.copyOfRange(b, 0, idx);
        } else {

            final Set<Float> set = new LinkedHashSet<>(N.initHashCapacity(a.length));

            for (int i = from; i < to; i++) {
                set.add(a[i]);
            }

            if (set.size() == to - from) {
                return (from == 0 && to == a.length) ? a.clone() : N.copyOfRange(a, from, to);
            } else {
                final float[] result = new float[set.size()];
                int i = 0;

                for (float e : set) {
                    result[i++] = e;
                }

                return result;
            }
        }
    }

    public static double[] removeDuplicates(final double[] a) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_DOUBLE_ARRAY;
        }

        return removeDuplicates(a, false);
    }

    public static double[] removeDuplicates(final double[] a, final boolean isSorted) {
        if (N.isNullOrEmpty(a)) {
            return N.EMPTY_DOUBLE_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    public static double[] removeDuplicates(final double[] a, final int from, final int to, final boolean isSorted) {
        checkFromToIndex(from, to, len(a));

        if (N.isNullOrEmpty(a) && from == 0 && to == 0) {
            return N.EMPTY_DOUBLE_ARRAY;
        } else if (to - from <= 1) {
            return N.copyOfRange(a, from, to);
        }

        if (isSorted) {
            final double[] b = (from == 0 && to == a.length) ? a.clone() : N.copyOfRange(a, from, to);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (N.equals(b[i], b[i - 1])) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : N.copyOfRange(b, 0, idx);
        } else {
            final Set<Double> set = new LinkedHashSet<>(N.initHashCapacity(a.length));

            for (int i = from; i < to; i++) {
                set.add(a[i]);
            }

            if (set.size() == to - from) {
                return (from == 0 && to == a.length) ? a.clone() : N.copyOfRange(a, from, to);
            } else {
                final double[] result = new double[set.size()];
                int i = 0;

                for (double e : set) {
                    result[i++] = e;
                }

                return result;
            }
        }
    }

    /**
     * <p>
     * Removes all duplicates elements
     * </p>
     *
     * <pre>
     * N.removeElements(["a", "b", "a"]) = ["a", "b"]
     * </pre>
     *
     * @param <T>
     *            the component type of the array
     * @param a
     * @return A new array containing the existing elements except the duplicates
     *
     * @throws NullPointerException if the specified array <code>a</code> is null.
     */
    public static <T> T[] removeDuplicates(final T[] a) {
        if (N.isNullOrEmpty(a)) {
            return a;
        }

        return removeDuplicates(a, false);
    }

    public static <T> T[] removeDuplicates(final T[] a, final boolean isSorted) {
        if (N.isNullOrEmpty(a)) {
            return a;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    public static <T> T[] removeDuplicates(final T[] a, final int from, final int to, final boolean isSorted) {
        checkFromToIndex(from, to, len(a));

        if (N.isNullOrEmpty(a) && from == 0 && to == 0) {
            return a;
        } else if (to - from <= 1) {
            return N.copyOfRange(a, from, to);
        }

        if (isSorted) {
            final T[] b = (from == 0 && to == a.length) ? a.clone() : N.copyOfRange(a, from, to);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (N.equals(b[i], b[i - 1])) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : N.copyOfRange(b, 0, idx);
        } else {
            final List<T> list = distinct(a, from, to);
            return list.toArray((T[]) N.newArray(a.getClass().getComponentType(), list.size()));
        }
    }

    /**
     *
     * @param c
     * @return <code>true</code> if there is one or more duplicated elements are removed. otherwise <code>false</code> is returned.
     */
    public static boolean removeDuplicates(final Collection<?> c) {
        if (N.isNullOrEmpty(c) || c.size() == 1) {
            return false;
        }

        return removeDuplicates(c, false);
    }

    /**
     *
     * @param c
     * @param isSorted
     * @return <code>true</code> if there is one or more duplicated elements are removed. otherwise <code>false</code> is returned.
     */
    @SuppressWarnings("rawtypes")
    public static boolean removeDuplicates(final Collection<?> c, final boolean isSorted) {
        if (N.isNullOrEmpty(c) || c.size() == 1) {
            return false;
        }

        if (isSorted) {
            boolean hasDuplicates = false;
            final Iterator<?> it = c.iterator();
            Object pre = it.next();
            Object next = null;
            while (it.hasNext()) {
                next = it.next();
                if (N.equals(next, pre)) {
                    it.remove();
                    hasDuplicates = true;
                } else {
                    pre = next;
                }
            }

            return hasDuplicates;
        } else {
            List<?> list = distinct(c);

            final boolean hasDuplicates = list.size() != c.size();

            if (hasDuplicates) {
                c.clear();
                c.addAll((List) list);
            }

            return hasDuplicates;
        }
    }

    // Primitive/Object array converters
    // ----------------------------------------------------------------------

    public static boolean hasDuplicates(final char[] a) {
        return hasDuplicates(a, false);
    }

    public static boolean hasDuplicates(final char[] a, final boolean isSorted) {
        if (N.isNullOrEmpty(a)) {
            return false;
        }

        return hasDuplicates(a, 0, a.length, isSorted);
    }

    static boolean hasDuplicates(final char[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        N.checkFromToIndex(fromIndex, toIndex, a.length);

        if (N.isNullOrEmpty(a) || toIndex - fromIndex < 2) {
            return false;
        } else if (toIndex - fromIndex == 2) {
            return a[fromIndex] == a[fromIndex + 1];
        } else if (toIndex - fromIndex == 3) {
            return a[fromIndex] == a[fromIndex + 1] || a[fromIndex] == a[fromIndex + 2] || a[fromIndex + 1] == a[fromIndex + 2];
        }

        if (isSorted) {
            for (int i = fromIndex + 1; i < toIndex; i++) {
                if (a[i] == a[i - 1]) {
                    return true;
                }
            }

            return false;
        } else {
            final Set<Character> set = new HashSet<>(N.initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                if (set.add(a[i]) == false) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean hasDuplicates(final byte[] a) {
        return hasDuplicates(a, false);
    }

    public static boolean hasDuplicates(final byte[] a, final boolean isSorted) {
        if (N.isNullOrEmpty(a)) {
            return false;
        }

        return hasDuplicates(a, 0, a.length, isSorted);
    }

    static boolean hasDuplicates(final byte[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        N.checkFromToIndex(fromIndex, toIndex, a.length);

        if (N.isNullOrEmpty(a) || toIndex - fromIndex < 2) {
            return false;
        } else if (toIndex - fromIndex == 2) {
            return a[fromIndex] == a[fromIndex + 1];
        } else if (toIndex - fromIndex == 3) {
            return a[fromIndex] == a[fromIndex + 1] || a[fromIndex] == a[fromIndex + 2] || a[fromIndex + 1] == a[fromIndex + 2];
        }

        if (isSorted) {
            for (int i = fromIndex + 1; i < toIndex; i++) {
                if (a[i] == a[i - 1]) {
                    return true;
                }
            }

            return false;
        } else {
            final Set<Byte> set = new HashSet<>(N.initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                if (set.add(a[i]) == false) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean hasDuplicates(final short[] a) {
        return hasDuplicates(a, false);
    }

    public static boolean hasDuplicates(final short[] a, final boolean isSorted) {
        if (N.isNullOrEmpty(a)) {
            return false;
        }

        return hasDuplicates(a, 0, a.length, isSorted);
    }

    static boolean hasDuplicates(final short[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        N.checkFromToIndex(fromIndex, toIndex, a.length);

        if (N.isNullOrEmpty(a) || toIndex - fromIndex < 2) {
            return false;
        } else if (toIndex - fromIndex == 2) {
            return a[fromIndex] == a[fromIndex + 1];
        } else if (toIndex - fromIndex == 3) {
            return a[fromIndex] == a[fromIndex + 1] || a[fromIndex] == a[fromIndex + 2] || a[fromIndex + 1] == a[fromIndex + 2];
        }

        if (isSorted) {
            for (int i = fromIndex + 1; i < toIndex; i++) {
                if (a[i] == a[i - 1]) {
                    return true;
                }
            }

            return false;
        } else {
            final Set<Short> set = new HashSet<>(N.initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                if (set.add(a[i]) == false) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean hasDuplicates(final int[] a) {
        return hasDuplicates(a, false);
    }

    public static boolean hasDuplicates(final int[] a, final boolean isSorted) {
        if (N.isNullOrEmpty(a)) {
            return false;
        }

        return hasDuplicates(a, 0, a.length, isSorted);
    }

    static boolean hasDuplicates(final int[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        N.checkFromToIndex(fromIndex, toIndex, a.length);

        if (N.isNullOrEmpty(a) || toIndex - fromIndex < 2) {
            return false;
        } else if (toIndex - fromIndex == 2) {
            return a[fromIndex] == a[fromIndex + 1];
        } else if (toIndex - fromIndex == 3) {
            return a[fromIndex] == a[fromIndex + 1] || a[fromIndex] == a[fromIndex + 2] || a[fromIndex + 1] == a[fromIndex + 2];
        }

        if (isSorted) {
            for (int i = fromIndex + 1; i < toIndex; i++) {
                if (a[i] == a[i - 1]) {
                    return true;
                }
            }

            return false;
        } else {
            final Set<Integer> set = new HashSet<>(N.initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                if (set.add(a[i]) == false) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean hasDuplicates(final long[] a) {
        return hasDuplicates(a, false);
    }

    public static boolean hasDuplicates(final long[] a, final boolean isSorted) {
        if (N.isNullOrEmpty(a)) {
            return false;
        }

        return hasDuplicates(a, 0, a.length, isSorted);
    }

    static boolean hasDuplicates(final long[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        N.checkFromToIndex(fromIndex, toIndex, a.length);

        if (N.isNullOrEmpty(a) || toIndex - fromIndex < 2) {
            return false;
        } else if (toIndex - fromIndex == 2) {
            return a[fromIndex] == a[fromIndex + 1];
        } else if (toIndex - fromIndex == 3) {
            return a[fromIndex] == a[fromIndex + 1] || a[fromIndex] == a[fromIndex + 2] || a[fromIndex + 1] == a[fromIndex + 2];
        }

        if (isSorted) {
            for (int i = fromIndex + 1; i < toIndex; i++) {
                if (a[i] == a[i - 1]) {
                    return true;
                }
            }

            return false;
        } else {
            final Set<Long> set = new HashSet<>(N.initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                if (set.add(a[i]) == false) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean hasDuplicates(final float[] a) {
        return hasDuplicates(a, false);
    }

    public static boolean hasDuplicates(final float[] a, final boolean isSorted) {
        if (N.isNullOrEmpty(a)) {
            return false;
        }

        return hasDuplicates(a, 0, a.length, isSorted);
    }

    static boolean hasDuplicates(final float[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        N.checkFromToIndex(fromIndex, toIndex, a.length);

        if (N.isNullOrEmpty(a) || toIndex - fromIndex < 2) {
            return false;
        } else if (toIndex - fromIndex == 2) {
            return N.equals(a[fromIndex], a[fromIndex + 1]);
        } else if (toIndex - fromIndex == 3) {
            return N.equals(a[fromIndex], a[fromIndex + 1]) || N.equals(a[fromIndex], a[fromIndex + 2]) || N.equals(a[fromIndex + 1], a[fromIndex + 2]);
        }

        if (isSorted) {
            for (int i = fromIndex + 1; i < toIndex; i++) {
                if (N.equals(a[i], a[i - 1])) {
                    return true;
                }
            }

            return false;
        } else {
            final Set<Float> set = new HashSet<>(N.initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                if (set.add(a[i]) == false) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean hasDuplicates(final double[] a) {
        return hasDuplicates(a, false);
    }

    public static boolean hasDuplicates(final double[] a, final boolean isSorted) {
        if (N.isNullOrEmpty(a)) {
            return false;
        }

        return hasDuplicates(a, 0, a.length, isSorted);
    }

    static boolean hasDuplicates(final double[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        N.checkFromToIndex(fromIndex, toIndex, a.length);

        if (N.isNullOrEmpty(a) || toIndex - fromIndex < 2) {
            return false;
        } else if (toIndex - fromIndex == 2) {
            return N.equals(a[fromIndex], a[fromIndex + 1]);
        } else if (toIndex - fromIndex == 3) {
            return N.equals(a[fromIndex], a[fromIndex + 1]) || N.equals(a[fromIndex], a[fromIndex + 2]) || N.equals(a[fromIndex + 1], a[fromIndex + 2]);
        }

        if (isSorted) {
            for (int i = fromIndex + 1; i < toIndex; i++) {
                if (N.equals(a[i], a[i - 1])) {
                    return true;
                }
            }

            return false;
        } else {
            final Set<Double> set = new HashSet<>(N.initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                if (set.add(a[i]) == false) {
                    return true;
                }
            }

            return false;
        }
    }

    public static <T> boolean hasDuplicates(final T[] a) {
        return hasDuplicates(a, false);
    }

    public static <T> boolean hasDuplicates(final T[] a, final boolean isSorted) {
        if (N.isNullOrEmpty(a)) {
            return false;
        }

        return hasDuplicates(a, 0, a.length, isSorted);
    }

    static <T> boolean hasDuplicates(final T[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        N.checkFromToIndex(fromIndex, toIndex, a.length);

        if (N.isNullOrEmpty(a) || toIndex - fromIndex < 2) {
            return false;
        } else if (toIndex - fromIndex == 2) {
            return N.equals(a[fromIndex], a[fromIndex + 1]);
        } else if (toIndex - fromIndex == 3) {
            return N.equals(a[fromIndex], a[fromIndex + 1]) || N.equals(a[fromIndex], a[fromIndex + 2]) || N.equals(a[fromIndex + 1], a[fromIndex + 2]);
        }

        if (isSorted) {
            for (int i = fromIndex + 1; i < toIndex; i++) {
                if (N.equals(a[i], a[i - 1])) {
                    return true;
                }
            }

            return false;
        } else {
            final Set<Object> set = new HashSet<>(N.initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                if (set.add(hashKey(a[i])) == false) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean hasDuplicates(final Collection<?> c) {
        return hasDuplicates(c, false);
    }

    public static boolean hasDuplicates(final Collection<?> c, final boolean isSorted) {
        if (N.isNullOrEmpty(c) || c.size() == 1) {
            return false;
        }

        if (isSorted) {
            final Iterator<?> it = c.iterator();
            Object pre = it.next();
            Object next = null;
            while (it.hasNext()) {
                next = it.next();

                if (N.equals(next, pre)) {
                    return true;
                }

                pre = next;
            }

            return false;
        } else {
            final Set<Object> set = new HashSet<>(N.initHashCapacity(c.size()));

            for (Object e : c) {
                if (set.add(hashKey(e)) == false) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     *
     * @param a
     * @return a long number
     */
    @SafeVarargs
    public static int sum(final char... a) {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        return sum(a, 0, a.length);
    }

    public static int sum(final char[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (N.isNullOrEmpty(a)) {
            if (to > 0) {
                throw new IndexOutOfBoundsException();
            }

            return 0;
        }

        long sum = 0;

        for (int i = from; i < to; i++) {
            sum += a[i];
        }

        return N.toIntExact(sum);
    }

    /**
     *
     * @param a
     * @return a long number
     */
    @SafeVarargs
    public static int sum(final byte... a) {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        return sum(a, 0, a.length);
    }

    public static int sum(final byte[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (N.isNullOrEmpty(a)) {
            if (to > 0) {
                throw new IndexOutOfBoundsException();
            }

            return 0;
        }

        long sum = 0;

        for (int i = from; i < to; i++) {
            sum += a[i];
        }

        return N.toIntExact(sum);
    }

    /**
     *
     * @param a
     * @return a long number
     */
    @SafeVarargs
    public static int sum(final short... a) {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        return sum(a, 0, a.length);
    }

    public static int sum(final short[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (N.isNullOrEmpty(a)) {
            if (to > 0) {
                throw new IndexOutOfBoundsException();
            }

            return 0;
        }

        long sum = 0;

        for (int i = from; i < to; i++) {
            sum += a[i];
        }

        return N.toIntExact(sum);
    }

    /**
     *
     * @param a
     * @return a long number
     */
    @SafeVarargs
    public static int sum(final int... a) {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        return sum(a, 0, a.length);
    }

    public static int sum(final int[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (N.isNullOrEmpty(a)) {
            if (to > 0) {
                throw new IndexOutOfBoundsException();
            }

            return 0;
        }

        long sum = 0;

        for (int i = from; i < to; i++) {
            sum += a[i];
        }

        return N.toIntExact(sum);
    }

    /**
     *
     * @param a
     * @return a long number
     */
    @SafeVarargs
    public static long sum(final long... a) {
        if (N.isNullOrEmpty(a)) {
            return 0L;
        }

        return sum(a, 0, a.length);
    }

    public static long sum(final long[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (N.isNullOrEmpty(a)) {
            if (to > 0) {
                throw new IndexOutOfBoundsException();
            }

            return 0L;
        }

        long sum = 0;

        for (int i = from; i < to; i++) {
            sum += a[i];
        }

        return sum;
    }

    /**
     *
     * @param a
     * @return a double number
     */
    @SafeVarargs
    public static float sum(final float... a) {
        if (N.isNullOrEmpty(a)) {
            return 0f;
        }

        return sum(a, 0, a.length);
    }

    public static float sum(final float[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (N.isNullOrEmpty(a)) {
            if (to > 0) {
                throw new IndexOutOfBoundsException();
            }

            return 0f;
        }

        final KahanSummation summation = new KahanSummation();

        for (int i = from; i < to; i++) {
            summation.add(a[i]);
        }

        return (float) summation.sum();
    }

    /**
     *
     * @param a
     * @return a double number
     */
    @SafeVarargs
    public static double sum(final double... a) {
        if (N.isNullOrEmpty(a)) {
            return 0d;
        }

        return sum(a, 0, a.length);
    }

    public static double sum(final double[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (N.isNullOrEmpty(a)) {
            if (to > 0) {
                throw new IndexOutOfBoundsException();
            }

            return 0d;
        }

        final KahanSummation summation = new KahanSummation();

        for (int i = from; i < to; i++) {
            summation.add(a[i]);
        }

        return summation.sum();
    }

    /**
     *
     * @param a
     * @return a double number
     */
    @SafeVarargs
    public static double average(final char... a) {
        if (N.isNullOrEmpty(a)) {
            return 0d;
        }

        return average(a, 0, a.length);
    }

    public static double average(final char[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (N.isNullOrEmpty(a)) {
            if (to > 0) {
                throw new IndexOutOfBoundsException();
            }

            return 0d;
        } else if (from == to) {
            return 0d;
        }

        long sum = 0;

        for (int i = from; i < to; i++) {
            sum += a[i];
        }

        return ((double) sum) / (to - from);
    }

    /**
    *
    * @param a
    * @return a double number
    */
    @SafeVarargs
    public static double average(final byte... a) {
        if (N.isNullOrEmpty(a)) {
            return 0d;
        }

        return average(a, 0, a.length);
    }

    public static double average(final byte[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (N.isNullOrEmpty(a)) {
            if (to > 0) {
                throw new IndexOutOfBoundsException();
            }

            return 0d;
        } else if (from == to) {
            return 0d;
        }

        long sum = 0;

        for (int i = from; i < to; i++) {
            sum += a[i];
        }

        return ((double) sum) / (to - from);
    }

    /**
     *
     * @param a
     * @return a double number
     */
    @SafeVarargs
    public static double average(final short... a) {
        if (N.isNullOrEmpty(a)) {
            return 0d;
        }

        return average(a, 0, a.length);
    }

    public static double average(final short[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (N.isNullOrEmpty(a)) {
            if (to > 0) {
                throw new IndexOutOfBoundsException();
            }

            return 0d;
        } else if (from == to) {
            return 0d;
        }

        long sum = 0;

        for (int i = from; i < to; i++) {
            sum += a[i];
        }

        return ((double) sum) / (to - from);
    }

    /**
     *
     * @param a
     * @return a double number
     */
    @SafeVarargs
    public static double average(final int... a) {
        if (N.isNullOrEmpty(a)) {
            return 0d;
        }

        return average(a, 0, a.length);
    }

    public static double average(final int[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (N.isNullOrEmpty(a)) {
            if (to > 0) {
                throw new IndexOutOfBoundsException();
            }

            return 0d;
        } else if (from == to) {
            return 0d;
        }

        long sum = 0;

        for (int i = from; i < to; i++) {
            sum += a[i];
        }

        return ((double) sum) / (to - from);
    }

    /**
     *
     * @param a
     * @return a double number
     */
    @SafeVarargs
    public static double average(final long... a) {
        if (N.isNullOrEmpty(a)) {
            return 0d;
        }

        return average(a, 0, a.length);
    }

    public static double average(final long[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (N.isNullOrEmpty(a)) {
            if (to > 0) {
                throw new IndexOutOfBoundsException();
            }

            return 0d;
        } else if (from == to) {
            return 0d;
        }

        long sum = 0;

        for (int i = from; i < to; i++) {
            sum += a[i];
        }

        return ((double) sum) / (to - from);
    }

    /**
     *
     * @param a
     * @return a double number
     */
    @SafeVarargs
    public static double average(final float... a) {
        if (N.isNullOrEmpty(a)) {
            return 0d;
        }

        return average(a, 0, a.length);
    }

    public static double average(final float[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (N.isNullOrEmpty(a)) {
            if (to > 0) {
                throw new IndexOutOfBoundsException();
            }

            return 0d;
        } else if (from == to) {
            return 0d;
        }

        final KahanSummation summation = new KahanSummation();

        for (int i = from; i < to; i++) {
            summation.add(a[i]);
        }

        return summation.average().orZero();
    }

    /**
     *
     * @param a
     * @return a double number
     */
    @SafeVarargs
    public static double average(final double... a) {
        if (N.isNullOrEmpty(a)) {
            return 0d;
        }

        return average(a, 0, a.length);
    }

    public static double average(final double[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (N.isNullOrEmpty(a)) {
            if (to > 0) {
                throw new IndexOutOfBoundsException();
            }

            return 0d;
        } else if (from == to) {
            return 0d;
        }

        final KahanSummation summation = new KahanSummation();

        for (int i = from; i < to; i++) {
            summation.add(a[i]);
        }

        return summation.average().orZero();
    }

    /**
     * <p>
     * Gets the minimum of two <code>char</code> values.
     * </p>
     *
     * @param a
     * @param b
     * @return the smallest of the values
     */
    public static char min(final char a, final char b) {
        return (a <= b) ? a : b;
    }

    /**
     * <p>
     * Gets the minimum of two <code>byte</code> values.
     * </p>
     *
     * @param a
     * @param b
     * @return the smallest of the values
     */
    public static byte min(final byte a, final byte b) {
        return (a <= b) ? a : b;
    }

    /**
     * <p>
     * Gets the minimum of two <code>short</code> values.
     * </p>
     *
     * @param a
     * @param b
     * @return the smallest of the values
     */
    public static short min(final short a, final short b) {
        return (a <= b) ? a : b;
    }

    /**
     * <p>
     * Gets the minimum of two <code>int</code> values.
     * </p>
     *
     * @param a
     * @param b
     * @return the smallest of the values
     */
    public static int min(final int a, final int b) {
        return (a <= b) ? a : b;
    }

    /**
     * <p>
     * Gets the minimum of two <code>long</code> values.
     * </p>
     *
     * @param a
     * @param b
     * @return the smallest of the values
     */
    public static long min(final long a, final long b) {
        return (a <= b) ? a : b;
    }

    /**
     * <p>
     * Gets the minimum of two <code>float</code> values.
     * </p>
     *
     * @param a
     * @param b
     * @return the smallest of the values
     */
    public static float min(final float a, final float b) {
        return Math.min(a, b);
    }

    /**
     * <p>
     * Gets the minimum of two <code>double</code> values.
     * </p>
     *
     * @param a
     * @param b
     * @return the smallest of the values
     */
    public static double min(final double a, final double b) {
        return Math.min(a, b);
    }

    public static <T extends Comparable<? super T>> T min(final T a, final T b) {
        return (T) min(a, b, NULL_MAX_COMPARATOR);
    }

    public static <T> T min(final T a, final T b, final Comparator<? super T> cmp) {
        return (cmp == null ? NULL_MAX_COMPARATOR : cmp).compare(a, b) <= 0 ? a : b;
    }

    /**
     * <p>
     * Gets the minimum of three <code>char</code> values.
     * </p>
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static char min(final char a, final char b, final char c) {
        final char m = (a <= b) ? a : b;

        return (m <= c) ? m : c;
    }

    /**
     * <p>
     * Gets the minimum of three <code>byte</code> values.
     * </p>
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static byte min(final byte a, final byte b, final byte c) {
        final byte m = (a <= b) ? a : b;

        return (m <= c) ? m : c;
    }

    /**
     * <p>
     * Gets the minimum of three <code>short</code> values.
     * </p>
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static short min(final short a, final short b, final short c) {
        final short m = (a <= b) ? a : b;

        return (m <= c) ? m : c;
    }

    /**
     * <p>
     * Gets the minimum of three <code>int</code> values.
     * </p>
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static int min(final int a, final int b, final int c) {
        final int m = (a <= b) ? a : b;

        return (m <= c) ? m : c;
    }

    /**
     * <p>
     * Gets the minimum of three <code>long</code> values.
     * </p>
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static long min(final long a, final long b, final long c) {
        final long m = (a <= b) ? a : b;

        return (m <= c) ? m : c;
    }

    /**
     * <p>
     * Gets the minimum of three <code>float</code> values.
     * </p>
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static float min(final float a, final float b, final float c) {
        return Math.min(Math.min(a, b), c);
    }

    /**
     * <p>
     * Gets the minimum of three <code>double</code> values.
     * </p>
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static double min(final double a, final double b, final double c) {
        return Math.min(Math.min(a, b), c);
    }

    public static <T extends Comparable<? super T>> T min(final T a, final T b, final T c) {
        return (T) min(a, b, c, NULL_MAX_COMPARATOR);
    }

    public static <T> T min(final T a, final T b, final T c, final Comparator<? super T> cmp) {
        return min(min(a, b, cmp), c, cmp);
    }

    /**
     * <p>
     * Returns the minimum value in an array.
     * </p>
     *
     * @param a
     *            an array, must not be null or empty
     * @return the minimum value in the array
     */
    @SafeVarargs
    public static char min(final char... a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        if (N.isNullOrEmpty(a)) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        return min(a, 0, a.length);
    }

    public static char min(final char[] a, final int from, final int to) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        // Finds and returns min
        char min = a[from];
        for (int i = from + 1; i < to; i++) {
            if (a[i] < min) {
                min = a[i];
            }
        }

        return min;
    }

    /**
     * <p>
     * Returns the minimum value in an array.
     * </p>
     *
     * @param a
     *            an array, must not be null or empty
     * @return the minimum value in the array
     */
    @SafeVarargs
    public static byte min(final byte... a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return min(a, 0, a.length);
    }

    public static byte min(final byte[] a, final int from, final int to) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        // Finds and returns min
        byte min = a[from];
        for (int i = from + 1; i < to; i++) {
            if (a[i] < min) {
                min = a[i];
            }
        }

        return min;
    }

    /**
     * <p>
     * Returns the minimum value in an array.
     * </p>
     *
     * @param a
     *            an array, must not be null or empty
     * @return the minimum value in the array
     */
    @SafeVarargs
    public static short min(final short... a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return min(a, 0, a.length);
    }

    public static short min(final short[] a, final int from, final int to) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        // Finds and returns min
        short min = a[from];
        for (int i = from + 1; i < to; i++) {
            if (a[i] < min) {
                min = a[i];
            }
        }

        return min;
    }

    /**
     * <p>
     * Returns the minimum value in an array.
     * </p>
     *
     * @param a
     *            an array, must not be null or empty
     * @return the minimum value in the array
     */
    @SafeVarargs
    public static int min(final int... a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return min(a, 0, a.length);
    }

    public static int min(final int[] a, final int from, final int to) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        // Finds and returns min
        int min = a[from];
        for (int i = from + 1; i < to; i++) {
            if (a[i] < min) {
                min = a[i];
            }
        }

        return min;
    }

    // Min in array
    // --------------------------------------------------------------------
    /**
     * <p>
     * Returns the minimum value in an array.
     * </p>
     *
     * @param a
     *            an array, must not be null or empty
     * @return the minimum value in the array
     */
    @SafeVarargs
    public static long min(final long... a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return min(a, 0, a.length);
    }

    public static long min(final long[] a, final int from, final int to) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        // Finds and returns min
        long min = a[from];
        for (int i = from + 1; i < to; i++) {
            if (a[i] < min) {
                min = a[i];
            }
        }

        return min;
    }

    /**
     * <p>
     * Returns the minimum value in an array.
     * </p>
     *
     * @param a
     *            an array, must not be null or empty
     * @return the minimum value in the array
     * @see IEEE754rUtil#min(float[]) IEEE754rUtils for a version of this method
     *      that handles NaN differently
     */
    @SafeVarargs
    public static float min(final float... a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return min(a, 0, a.length);
    }

    public static float min(final float[] a, final int from, final int to) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        // Finds and returns min
        float min = a[from];
        for (int i = from + 1; i < to; i++) {
            min = Math.min(min, a[i]);

            if (Float.isNaN(min)) {
                return min;
            }
        }

        return min;
    }

    /**
     * <p>
     * Returns the minimum value in an array.
     * </p>
     *
     * @param a
     *            an array, must not be null or empty
     * @return the minimum value in the array
     * @see IEEE754rUtil#min(double[]) IEEE754rUtils for a version of this
     *      method that handles NaN differently
     */
    @SafeVarargs
    public static double min(final double... a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return min(a, 0, a.length);
    }

    public static double min(final double[] a, final int from, final int to) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        // Finds and returns min
        double min = a[from];
        for (int i = from + 1; i < to; i++) {
            min = Math.min(min, a[i]);

            if (Double.isNaN(min)) {
                return min;
            }
        }

        return min;
    }

    /**
     * Returns the minimum element in the array.
     *
     * @param a
     *            an array, must not be null or empty
     * @return the minimum value in the array
     */
    public static <T extends Comparable<? super T>> T min(final T[] a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return min(a, 0, a.length);
    }

    public static <T extends Comparable<? super T>> T min(final T[] a, final int from, final int to) {
        return (T) min(a, from, to, NULL_MAX_COMPARATOR);
    }

    /**
     * Returns the minimum element in the array.
     *
     * @param a
     *            an array, must not be null or empty
     * @param cmp
     * @return the minimum value in the array
     */
    public static <T> T min(final T[] a, final Comparator<? super T> cmp) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return min(a, 0, a.length, cmp);
    }

    public static <T> T min(final T[] a, final int from, final int to, Comparator<? super T> cmp) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        cmp = cmp == null ? NULL_MAX_COMPARATOR : cmp;

        T candidate = a[from];
        for (int i = from + 1; i < to; i++) {
            if (cmp.compare(a[i], candidate) < 0) {
                candidate = a[i];
            }

            if (candidate == null && cmp == NULL_MIN_COMPARATOR) {
                return null;
            }
        }

        return candidate;
    }

    public static <T extends Comparable<? super T>> T min(final Collection<? extends T> c) {
        N.checkArgNotNullOrEmpty(c, "The spcified collection 'c' can not be null or empty");

        return min(c, 0, c.size());
    }

    public static <T extends Comparable<? super T>> T min(final Collection<? extends T> c, final int from, final int to) {
        N.checkArgNotNullOrEmpty(c, "The spcified collection 'c' can not be null or empty");

        return (T) min(c, from, to, NULL_MAX_COMPARATOR);
    }

    public static <T> T min(final Collection<? extends T> c, Comparator<? super T> cmp) {
        N.checkArgNotNullOrEmpty(c, "The spcified collection 'c' can not be null or empty");

        return min(c, 0, c.size(), cmp);
    }

    /**
     * Returns the minimum element in the collection.
     *
     * @param c
     * @param from
     * @param to
     * @param cmp
     * @return the minimum value in the Collection
     */
    public static <T> T min(final Collection<? extends T> c, final int from, final int to, Comparator<? super T> cmp) {
        checkFromToIndex(from, to, size(c));

        if (N.isNullOrEmpty(c) || to - from < 1 || from >= c.size()) {
            throw new IllegalArgumentException("The size of collection can not be null or empty");
        }

        cmp = cmp == null ? NULL_MAX_COMPARATOR : cmp;

        T candidate = null;
        T e = null;

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;
            candidate = list.get(from);

            for (int i = from + 1; i < to; i++) {
                e = list.get(i);

                if (cmp.compare(e, candidate) < 0) {
                    candidate = e;
                }

                if (candidate == null && cmp == NULL_MIN_COMPARATOR) {
                    return null;
                }
            }
        } else {
            final Iterator<? extends T> it = c.iterator();

            for (int i = 0; i < to; i++) {
                if (i < from) {
                    it.next();
                    continue;
                } else if (i == from) {
                    candidate = it.next();
                } else {
                    e = it.next();

                    if (cmp.compare(e, candidate) < 0) {
                        candidate = e;
                    }

                    if (candidate == null && cmp == NULL_MIN_COMPARATOR) {
                        return null;
                    }
                }
            }
        }

        return candidate;
    }

    public static <T extends Comparable<T>> List<T> minAll(final T[] a) {
        return minAll(a, NULL_MAX_COMPARATOR);
    }

    public static <T> List<T> minAll(final T[] a, Comparator<? super T> cmp) {
        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        cmp = cmp == null ? NULL_MAX_COMPARATOR : cmp;

        final List<T> result = new ArrayList<>();
        T candicate = a[0];
        int cp = 0;

        result.add(candicate);

        for (int i = 1, len = a.length; i < len; i++) {
            cp = cmp.compare(a[i], candicate);

            if (cp == 0) {
                result.add(a[i]);
            } else if (cp < 0) {
                result.clear();
                result.add(a[i]);
                candicate = a[i];
            }
        }

        return result;
    }

    public static <T extends Comparable<T>> List<T> minAll(final Collection<T> c) {
        return minAll(c, NULL_MAX_COMPARATOR);
    }

    public static <T> List<T> minAll(final Collection<T> c, Comparator<? super T> cmp) {
        if (N.isNullOrEmpty(c)) {
            return new ArrayList<>();
        }

        cmp = cmp == null ? NULL_MAX_COMPARATOR : cmp;

        final Iterator<T> iter = c.iterator();
        final List<T> result = new ArrayList<>();
        T candicate = iter.next();
        T next = null;
        int cp = 0;

        result.add(candicate);

        while (iter.hasNext()) {
            next = iter.next();
            cp = cmp.compare(next, candicate);

            if (cp == 0) {
                result.add(next);
            } else if (cp < 0) {
                result.clear();
                result.add(next);
                candicate = next;
            }
        }

        return result;
    }

    /**
     * <p>
     * Gets the maximum of two <code>char</code> values.
     * </p>
     *
     * @param a
     * @param b
     * @return the largest of the values
     */
    public static char max(final char a, final char b) {
        return (a >= b) ? a : b;
    }

    /**
     * <p>
     * Gets the maximum of two <code>byte</code> values.
     * </p>
     *
     * @param a
     * @param b
     * @return the largest of the values
     */
    public static byte max(final byte a, final byte b) {
        return (a >= b) ? a : b;
    }

    /**
     * <p>
     * Gets the maximum of two <code>short</code> values.
     * </p>
     *
     * @param a
     * @param b
     * @return the largest of the values
     */
    public static short max(final short a, final short b) {
        return (a >= b) ? a : b;
    }

    /**
     * <p>
     * Gets the maximum of two <code>int</code> values.
     * </p>
     *
     * @param a
     * @param b
     * @return the largest of the values
     */
    public static int max(final int a, final int b) {
        return (a >= b) ? a : b;
    }

    /**
     * <p>
     * Gets the maximum of two <code>long</code> values.
     * </p>
     *
     * @param a
     * @param b
     * @return the largest of the values
     */
    public static long max(final long a, final long b) {
        return (a >= b) ? a : b;
    }

    /**
     * <p>
     * Gets the maximum of two <code>float</code> values.
     * </p>
     *
     * @param a
     * @param b
     * @return the largest of the values
     */
    public static float max(final float a, final float b) {
        return Math.max(a, b);
    }

    /**
     * <p>
     * Gets the maximum of two <code>double</code> values.
     * </p>
     *
     * @param a
     * @param b
     * @return the largest of the values
     */
    public static double max(final double a, final double b) {
        return Math.max(a, b);
    }

    public static <T extends Comparable<? super T>> T max(final T a, final T b) {
        return (T) max(a, b, NULL_MIN_COMPARATOR);
    }

    public static <T> T max(final T a, final T b, final Comparator<? super T> cmp) {
        return (cmp == null ? NULL_MIN_COMPARATOR : cmp).compare(a, b) >= 0 ? a : b;
    }

    /**
     * Gets the maximum of three <code>char</code> values.
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static char max(final char a, final char b, final char c) {
        final char m = (a >= b) ? a : b;

        return (m >= c) ? m : c;
    }

    /**
     * Gets the maximum of three <code>byte</code> values.
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static byte max(final byte a, final byte b, final byte c) {
        final byte m = (a >= b) ? a : b;

        return (m >= c) ? m : c;
    }

    /**
     * Gets the maximum of three <code>short</code> values.
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static short max(final short a, final short b, final short c) {
        final short m = (a >= b) ? a : b;

        return (m >= c) ? m : c;
    }

    /**
     * Gets the maximum of three <code>int</code> values.
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static int max(final int a, final int b, final int c) {
        final int m = (a >= b) ? a : b;

        return (m >= c) ? m : c;
    }

    /**
     * Gets the maximum of three <code>long</code> values.
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static long max(final long a, final long b, final long c) {
        final long m = (a >= b) ? a : b;

        return (m >= c) ? m : c;
    }

    /**
     * Gets the maximum of three <code>float</code> values.
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static float max(final float a, final float b, final float c) {
        return Math.max(Math.max(a, b), c);
    }

    /**
     * Gets the maximum of three <code>double</code> values.
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static double max(final double a, final double b, final double c) {
        return Math.max(Math.max(a, b), c);
    }

    public static <T extends Comparable<? super T>> T max(final T a, final T b, final T c) {
        return (T) max(a, b, c, NULL_MIN_COMPARATOR);
    }

    public static <T> T max(final T a, final T b, final T c, final Comparator<? super T> cmp) {
        return max(max(a, b, cmp), c, cmp);
    }

    /**
     * <p>
     * Returns the maximum value in an array.
     * </p>
     *
     * @param a
     *            an array, must not be null or empty
     * @return the maximum value in the array
     */
    @SafeVarargs
    public static char max(final char... a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return max(a, 0, a.length);
    }

    public static char max(final char[] a, final int from, final int to) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        // Finds and returns max
        char max = a[from];
        for (int i = from + 1; i < to; i++) {
            if (a[i] > max) {
                max = a[i];
            }
        }

        return max;
    }

    /**
     * <p>
     * Returns the maximum value in an array.
     * </p>
     *
     * @param a
     *            an array, must not be null or empty
     * @return the maximum value in the array
     */
    @SafeVarargs
    public static byte max(final byte... a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return max(a, 0, a.length);
    }

    public static byte max(final byte[] a, final int from, final int to) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        // Finds and returns max
        byte max = a[from];
        for (int i = from + 1; i < to; i++) {
            if (a[i] > max) {
                max = a[i];
            }
        }

        return max;
    }

    /**
     * <p>
     * Returns the maximum value in an array.
     * </p>
     *
     * @param a
     *            an array, must not be null or empty
     * @return the maximum value in the array
     */
    @SafeVarargs
    public static short max(final short... a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return max(a, 0, a.length);
    }

    public static short max(final short[] a, final int from, final int to) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        // Finds and returns max
        short max = a[from];
        for (int i = from + 1; i < to; i++) {
            if (a[i] > max) {
                max = a[i];
            }
        }

        return max;
    }

    /**
     * <p>
     * Returns the maximum value in an array.
     * </p>
     *
     * @param a
     *            an array, must not be null or empty
     * @return the maximum value in the array
     */
    @SafeVarargs
    public static int max(final int... a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return max(a, 0, a.length);
    }

    public static int max(final int[] a, final int from, final int to) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        // Finds and returns max
        int max = a[from];
        for (int i = from + 1; i < to; i++) {
            if (a[i] > max) {
                max = a[i];
            }
        }

        return max;
    }

    /**
     * <p>
     * Returns the maximum value in an array.
     * </p>
     *
     * @param a
     *            an array, must not be null or empty
     * @return the maximum value in the array
     */
    @SafeVarargs
    public static long max(final long... a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return max(a, 0, a.length);
    }

    public static long max(final long[] a, final int from, final int to) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        // Finds and returns max
        long max = a[from];
        for (int i = from + 1; i < to; i++) {
            if (a[i] > max) {
                max = a[i];
            }
        }

        return max;
    }

    /**
     * <p>
     * Returns the maximum value in an array.
     * </p>
     *
     * @param a
     *            an array, must not be null or empty
     * @return the maximum value in the array
     * @see IEEE754rUtil#max(float[]) IEEE754rUtils for a version of this method
     *      that handles NaN differently
     */
    @SafeVarargs
    public static float max(final float... a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return max(a, 0, a.length);
    }

    public static float max(final float[] a, final int from, final int to) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        // Finds and returns max
        float max = a[from];
        for (int i = from + 1; i < to; i++) {
            max = Math.max(max, a[i]);

            if (Float.isNaN(max)) {
                return max;
            }
        }

        return max;
    }

    /**
     * <p>
     * Returns the maximum value in an array.
     * </p>
     *
     * @param a
     *            an array, must not be null or empty
     * @return the maximum value in the array
     * @see IEEE754rUtil#max(double[]) IEEE754rUtils for a version of this
     *      method that handles NaN differently
     */
    @SafeVarargs
    public static double max(final double... a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return max(a, 0, a.length);
    }

    public static double max(final double[] a, final int from, final int to) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        // Finds and returns max
        double max = a[from];
        for (int i = from + 1; i < to; i++) {
            max = Math.max(max, a[i]);

            if (Double.isNaN(max)) {
                return max;
            }
        }

        return max;
    }

    /**
     * Returns the maximum element in the array.
     *
     * @param a
     *            an array, must not be null or empty
     * @return the maximum value in the array
     * @throws IllegalArgumentException
     *             if <code>a</code> is <code>null</code> or empty.
     */
    public static <T extends Comparable<? super T>> T max(final T[] a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return max(a, 0, a.length);
    }

    public static <T extends Comparable<? super T>> T max(final T[] a, final int from, final int to) {
        return (T) max(a, from, to, NULL_MIN_COMPARATOR);
    }

    /**
     * Returns the maximum element in the array.
     *
     * @param a
     *            an array, must not be null or empty
     * @param cmp
     * @return the maximum value in the array
     */
    public static <T> T max(final T[] a, final Comparator<? super T> cmp) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return max(a, 0, a.length, cmp);
    }

    public static <T> T max(final T[] a, final int from, final int to, Comparator<? super T> cmp) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        cmp = cmp == null ? NULL_MIN_COMPARATOR : cmp;

        T candidate = a[from];
        for (int i = from + 1; i < to; i++) {
            if (cmp.compare(a[i], candidate) > 0) {
                candidate = a[i];
            }

            if (candidate == null && cmp == NULL_MAX_COMPARATOR) {
                return null;
            }
        }

        return candidate;
    }

    public static <T extends Comparable<? super T>> T max(final Collection<? extends T> c) {
        N.checkArgNotNullOrEmpty(c, "The spcified collection 'c' can not be null or empty");

        return max(c, 0, c.size());
    }

    public static <T extends Comparable<? super T>> T max(final Collection<? extends T> c, final int from, final int to) {
        N.checkArgNotNullOrEmpty(c, "The spcified collection 'c' can not be null or empty");

        return (T) max(c, from, to, NULL_MIN_COMPARATOR);
    }

    public static <T> T max(final Collection<? extends T> c, Comparator<? super T> cmp) {
        N.checkArgNotNullOrEmpty(c, "The spcified collection 'c' can not be null or empty");

        return max(c, 0, c.size(), cmp);
    }

    /**
     * Returns the maximum element in the collection.
     *
     * @param c
     * @param from
     * @param to
     * @param cmp
     * @return the maximum value in the Collection
     */
    public static <T> T max(final Collection<? extends T> c, final int from, final int to, Comparator<? super T> cmp) {
        checkFromToIndex(from, to, size(c));

        if (N.isNullOrEmpty(c) || to - from < 1 || from >= c.size()) {
            throw new IllegalArgumentException("The size of collection can not be null or empty");
        }

        cmp = cmp == null ? NULL_MIN_COMPARATOR : cmp;

        T candidate = null;
        T e = null;

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;
            candidate = list.get(from);

            for (int i = from + 1; i < to; i++) {
                e = list.get(i);

                if (cmp.compare(e, candidate) > 0) {
                    candidate = e;
                }

                if (candidate == null && cmp == NULL_MAX_COMPARATOR) {
                    return null;
                }
            }
        } else {
            final Iterator<? extends T> it = c.iterator();

            for (int i = 0; i < to; i++) {
                if (i < from) {
                    it.next();
                    continue;
                } else if (i == from) {
                    candidate = it.next();
                } else {
                    e = it.next();

                    if (cmp.compare(e, candidate) > 0) {
                        candidate = e;
                    }
                }

                if (candidate == null && cmp == NULL_MAX_COMPARATOR) {
                    return null;
                }
            }
        }

        return candidate;
    }

    public static <T extends Comparable<T>> List<T> maxAll(final T[] a) {
        return maxAll(a, NULL_MIN_COMPARATOR);
    }

    public static <T> List<T> maxAll(final T[] a, Comparator<? super T> cmp) {
        if (N.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        cmp = cmp == null ? NULL_MIN_COMPARATOR : cmp;

        final List<T> result = new ArrayList<>();
        T candicate = a[0];
        int cp = 0;

        result.add(candicate);

        for (int i = 1, len = a.length; i < len; i++) {
            cp = cmp.compare(a[i], candicate);

            if (cp == 0) {
                result.add(a[i]);
            } else if (cp > 0) {
                result.clear();
                result.add(a[i]);
                candicate = a[i];
            }
        }

        return result;
    }

    public static <T extends Comparable<T>> List<T> maxAll(final Collection<T> c) {
        return maxAll(c, NULL_MIN_COMPARATOR);
    }

    public static <T> List<T> maxAll(final Collection<T> c, Comparator<? super T> cmp) {
        if (N.isNullOrEmpty(c)) {
            return new ArrayList<>();
        }

        cmp = cmp == null ? NULL_MIN_COMPARATOR : cmp;

        final Iterator<T> iter = c.iterator();
        final List<T> result = new ArrayList<>();
        T candicate = iter.next();
        T next = null;
        int cp = 0;

        result.add(candicate);

        while (iter.hasNext()) {
            next = iter.next();
            cp = cmp.compare(next, candicate);

            if (cp == 0) {
                result.add(next);
            } else if (cp > 0) {
                result.clear();
                result.add(next);
                candicate = next;
            }
        }

        return result;
    }

    /**
     * Gets the median of three values.
     *
     * @param a
     * @param b
     * @param c
     * @return the median of the values
     * @see #median(int...)
     */
    public static char median(final char a, final char b, final char c) {
        if ((a >= b && a <= c) || (a >= c && a <= b)) {
            return a;
        } else if ((b >= a && b <= c) || (b >= c && b <= a)) {
            return b;
        } else {
            return c;
        }
    }

    /**
     * Gets the median of three values.
     *
     * @param a
     * @param b
     * @param c
     * @return the median of the values
     * @see #median(int...)
     */
    public static byte median(final byte a, final byte b, final byte c) {
        if ((a >= b && a <= c) || (a >= c && a <= b)) {
            return a;
        } else if ((b >= a && b <= c) || (b >= c && b <= a)) {
            return b;
        } else {
            return c;
        }
    }

    /**
     * Gets the median of three values.
     *
     * @param a
     * @param b
     * @param c
     * @return the median of the values
     * @see #median(int...)
     */
    public static short median(final short a, final short b, final short c) {
        if ((a >= b && a <= c) || (a >= c && a <= b)) {
            return a;
        } else if ((b >= a && b <= c) || (b >= c && b <= a)) {
            return b;
        } else {
            return c;
        }
    }

    /**
     * Gets the median of three values.
     *
     * @param a
     * @param b
     * @param c
     * @return the median of the values
     * @see #median(int...)
     */
    public static int median(final int a, final int b, final int c) {
        if ((a >= b && a <= c) || (a >= c && a <= b)) {
            return a;
        } else if ((b >= a && b <= c) || (b >= c && b <= a)) {
            return b;
        } else {
            return c;
        }
    }

    /**
     * Gets the median of three values.
     *
     * @param a
     * @param b
     * @param c
     * @return the median of the values
     * @see #median(int...)
     */
    public static long median(final long a, final long b, final long c) {
        if ((a >= b && a <= c) || (a >= c && a <= b)) {
            return a;
        } else if ((b >= a && b <= c) || (b >= c && b <= a)) {
            return b;
        } else {
            return c;
        }
    }

    /**
     * Gets the median of three values.
     *
     * @param a
     * @param b
     * @param c
     * @return the median of the values
     * @see #median(int...)
     */
    public static float median(final float a, final float b, final float c) {
        int ab = Float.compare(a, b);
        int ac = Float.compare(a, c);
        int bc = 0;

        if ((ab >= 0 && ac <= 0) || (ac >= 0 && ab <= 0)) {
            return a;
        } else if ((((bc = Float.compare(b, c)) <= 0) && ab <= 0) || (bc >= 0 && ab >= 0)) {
            return b;
        } else {
            return c;
        }
    }

    /**
     * Gets the median of three values.
     *
     * @param a
     * @param b
     * @param c
     * @return the median of the values
     * @see #median(int...)
     */
    public static double median(final double a, final double b, final double c) {
        int ab = Double.compare(a, b);
        int ac = Double.compare(a, c);
        int bc = 0;

        if ((ab >= 0 && ac <= 0) || (ac >= 0 && ab <= 0)) {
            return a;
        } else if ((((bc = Double.compare(b, c)) <= 0) && ab <= 0) || (bc >= 0 && ab >= 0)) {
            return b;
        } else {
            return c;
        }
    }

    /**
     * Gets the median of three values.
     *
     * @param a
     * @param b
     * @param c
     * @return the median of the values
     * @see #median(int...)
     */
    public static <T extends Comparable<? super T>> T median(final T a, final T b, final T c) {
        return (T) median(a, b, c, NATURAL_ORDER);
    }

    /**
     * Gets the median of three values.
     *
     * @param a
     * @param b
     * @param c
     * @return the median of the values
     * @see #median(int...)
     */
    public static <T> T median(final T a, final T b, final T c, Comparator<? super T> cmp) {
        cmp = cmp == null ? NATURAL_ORDER : cmp;

        int ab = cmp.compare(a, b);
        int ac = cmp.compare(a, c);
        int bc = 0;

        if ((ab >= 0 && ac <= 0) || (ac >= 0 && ab <= 0)) {
            return a;
        } else if ((((bc = cmp.compare(b, c)) <= 0) && ab <= 0) || (bc >= 0 && ab >= 0)) {
            return b;
        } else {
            return c;
        }
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param a an array, must not be null or empty
     * @return the median value in the array
     * @see #median(int...)
     */
    @SafeVarargs
    public static char median(final char... a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return median(a, 0, a.length);
    }

    public static char median(final char[] a, final int from, final int to) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        checkFromToIndex(from, to, a.length);

        final int len = to - from;

        if (len == 1) {
            return a[from];
        } else if (len == 2) {
            return min(a[from], a[from + 1]);
        } else if (len == 3) {
            return median(a[from], a[from + 1], a[from + 2]);
        } else {
            return kthLargest(a, from, to, len / 2 + 1);
        }
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param a an array, must not be null or empty
     * @return the median value in the array
     * @see #median(int...)
     */
    @SafeVarargs
    public static byte median(final byte... a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return median(a, 0, a.length);
    }

    public static byte median(final byte[] a, final int from, final int to) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        checkFromToIndex(from, to, a.length);

        final int len = to - from;

        if (len == 1) {
            return a[from];
        } else if (len == 2) {
            return min(a[from], a[from + 1]);
        } else if (len == 3) {
            return median(a[from], a[from + 1], a[from + 2]);
        } else {
            return kthLargest(a, from, to, len / 2 + 1);
        }
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param a an array, must not be null or empty
     * @return the median value in the array
     * @see #median(int...)
     */
    @SafeVarargs
    public static short median(final short... a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return median(a, 0, a.length);
    }

    public static short median(final short[] a, final int from, final int to) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        checkFromToIndex(from, to, a.length);

        final int len = to - from;

        if (len == 1) {
            return a[from];
        } else if (len == 2) {
            return min(a[from], a[from + 1]);
        } else if (len == 3) {
            return median(a[from], a[from + 1], a[from + 2]);
        } else {
            return kthLargest(a, from, to, len / 2 + 1);
        }
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param a an array, must not be null or empty
     * @return the median value in the array
     * @see #median(int...)
     */
    @SafeVarargs
    public static int median(final int... a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return median(a, 0, a.length);
    }

    public static int median(final int[] a, final int from, final int to) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        checkFromToIndex(from, to, a.length);

        final int len = to - from;

        if (len == 1) {
            return a[from];
        } else if (len == 2) {
            return min(a[from], a[from + 1]);
        } else if (len == 3) {
            return median(a[from], a[from + 1], a[from + 2]);
        } else {
            return kthLargest(a, from, to, len / 2 + 1);
        }
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param a an array, must not be null or empty
     * @return the median value in the array
     * @see #median(int...)
     */
    @SafeVarargs
    public static long median(final long... a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return median(a, 0, a.length);
    }

    public static long median(final long[] a, final int from, final int to) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        checkFromToIndex(from, to, a.length);

        final int len = to - from;

        if (len == 1) {
            return a[from];
        } else if (len == 2) {
            return min(a[from], a[from + 1]);
        } else if (len == 3) {
            return median(a[from], a[from + 1], a[from + 2]);
        } else {
            return kthLargest(a, from, to, len / 2 + 1);
        }
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param a an array, must not be null or empty
     * @return the median value in the array
     * @see #median(int...)
     */
    @SafeVarargs
    public static float median(final float... a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return median(a, 0, a.length);
    }

    public static float median(final float[] a, final int from, final int to) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        checkFromToIndex(from, to, a.length);

        final int len = to - from;

        if (len == 1) {
            return a[from];
        } else if (len == 2) {
            return min(a[from], a[from + 1]);
        } else if (len == 3) {
            return median(a[from], a[from + 1], a[from + 2]);
        } else {
            return kthLargest(a, from, to, len / 2 + 1);
        }
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param a an array, must not be null or empty
     * @return the median value in the array
     * @see #median(int...)
     */
    @SafeVarargs
    public static double median(final double... a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return median(a, 0, a.length);
    }

    public static double median(final double[] a, final int from, final int to) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        checkFromToIndex(from, to, a.length);

        final int len = to - from;

        if (len == 1) {
            return a[from];
        } else if (len == 2) {
            return min(a[from], a[from + 1]);
        } else if (len == 3) {
            return median(a[from], a[from + 1], a[from + 2]);
        } else {
            return kthLargest(a, from, to, len / 2 + 1);
        }
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param a an array, must not be null or empty
     * @return the median value in the array
     * @see #median(int...)
     */
    public static <T extends Comparable<? super T>> T median(final T[] a) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return median(a, 0, a.length);
    }

    public static <T extends Comparable<? super T>> T median(final T[] a, final int from, final int to) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        return (T) median(a, from, to, NATURAL_ORDER);
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param a an array, must not be null or empty
     * @return the median value in the array
     * @see #median(int...)
     */
    public static <T> T median(final T[] a, Comparator<? super T> cmp) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return median(a, 0, a.length, cmp);
    }

    public static <T> T median(final T[] a, final int from, final int to, Comparator<? super T> cmp) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        checkFromToIndex(from, to, a.length);

        cmp = cmp == null ? NATURAL_ORDER : cmp;

        final int len = to - from;

        return kthLargest(a, from, to, len / 2 + 1, cmp);
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param a an array, must not be null or empty
     * @return the median value in the array
     * @see #median(int...)
     */
    public static <T extends Comparable<? super T>> T median(final Collection<? extends T> c) {
        N.checkArgNotNullOrEmpty(c, "The spcified collection 'c' can not be null or empty");

        return median(c, 0, c.size());
    }

    public static <T extends Comparable<? super T>> T median(final Collection<? extends T> c, final int from, final int to) {
        return (T) median(c, from, to, NATURAL_ORDER);
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param a an array, must not be null or empty
     * @return the median value in the array
     * @see #median(int...)
     */
    public static <T> T median(final Collection<? extends T> c, Comparator<? super T> cmp) {
        N.checkArgNotNullOrEmpty(c, "The spcified collection 'c' can not be null or empty");

        return median(c, 0, c.size(), cmp);
    }

    public static <T> T median(final Collection<? extends T> c, final int from, final int to, Comparator<? super T> cmp) {
        if (N.isNullOrEmpty(c) || to - from < 1) {
            throw new IllegalArgumentException("The length of collection can not be null or empty");
        }

        checkFromToIndex(from, to, c.size());

        cmp = cmp == null ? NATURAL_ORDER : cmp;

        final int len = to - from;

        return kthLargest(c, from, to, len / 2 + 1, cmp);
    }

    /**
     *
     * @param a
     * @param k
     * @return the kth largest element.
     * @throws IllegalArgumentException if the length of the specified array is less than <code>k</code>.
     */
    public static char kthLargest(final char[] a, final int k) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return Array.kthLargest(a, k);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @param k
     * @return the kth largest element from <code>a[from]</code> to <code>a[to]</code>
     * @throws IllegalArgumentException if <code>to - from</code> is less than <code>k</code>.
     */
    public static char kthLargest(final char[] a, final int from, final int to, final int k) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        return Array.kthLargest(a, from, to, k);
    }

    /**
     *
     * @param a
     * @param k
     * @return the kth largest element.
     * @throws IllegalArgumentException if the length of the specified array is less than <code>k</code>.
     */
    public static byte kthLargest(final byte[] a, final int k) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return Array.kthLargest(a, k);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @param k
     * @return the kth largest element from <code>a[from]</code> to <code>a[to]</code>
     * @throws IllegalArgumentException if <code>to - from</code> is less than <code>k</code>.
     */
    public static byte kthLargest(final byte[] a, final int from, final int to, final int k) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        return Array.kthLargest(a, from, to, k);
    }

    /**
     *
     * @param a
     * @param k
     * @return the kth largest element.
     * @throws IllegalArgumentException if the length of the specified array is less than <code>k</code>.
     */
    public static short kthLargest(final short[] a, final int k) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return Array.kthLargest(a, k);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @param k
     * @return the kth largest element from <code>a[from]</code> to <code>a[to]</code>
     * @throws IllegalArgumentException if <code>to - from</code> is less than <code>k</code>.
     */
    public static short kthLargest(final short[] a, final int from, final int to, final int k) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        return Array.kthLargest(a, from, to, k);
    }

    /**
     *
     * @param a
     * @param k
     * @return the kth largest element.
     * @throws IllegalArgumentException if the length of the specified array is less than <code>k</code>.
     */
    public static int kthLargest(final int[] a, final int k) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return Array.kthLargest(a, k);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @param k
     * @return the kth largest element from <code>a[from]</code> to <code>a[to]</code>
     * @throws IllegalArgumentException if <code>to - from</code> is less than <code>k</code>.
     */
    public static int kthLargest(final int[] a, final int from, final int to, final int k) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        return Array.kthLargest(a, from, to, k);
    }

    /**
     *
     * @param a
     * @param k
     * @return the kth largest element.
     * @throws IllegalArgumentException if the length of the specified array is less than <code>k</code>.
     */
    public static long kthLargest(final long[] a, final int k) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return Array.kthLargest(a, k);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @param k
     * @return the kth largest element from <code>a[from]</code> to <code>a[to]</code>
     * @throws IllegalArgumentException if <code>to - from</code> is less than <code>k</code>.
     */
    public static long kthLargest(final long[] a, final int from, final int to, final int k) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        return Array.kthLargest(a, from, to, k);
    }

    /**
     *
     * @param a
     * @param k
     * @return the kth largest element.
     * @throws IllegalArgumentException if the length of the specified array is less than <code>k</code>.
     */
    public static float kthLargest(final float[] a, final int k) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return Array.kthLargest(a, k);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @param k
     * @return the kth largest element from <code>a[from]</code> to <code>a[to]</code>
     * @throws IllegalArgumentException if <code>to - from</code> is less than <code>k</code>.
     */
    public static float kthLargest(final float[] a, final int from, final int to, final int k) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        return Array.kthLargest(a, from, to, k);
    }

    /**
     *
     * @param a
     * @param k
     * @return the kth largest element.
     * @throws IllegalArgumentException if the length of the specified array is less than <code>k</code>.
     */
    public static double kthLargest(final double[] a, final int k) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return Array.kthLargest(a, k);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @param k
     * @return the kth largest element from <code>a[from]</code> to <code>a[to]</code>
     * @throws IllegalArgumentException if <code>to - from</code> is less than <code>k</code>.
     */
    public static double kthLargest(final double[] a, final int from, final int to, final int k) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        return Array.kthLargest(a, from, to, k);
    }

    /**
     *
     * @param a
     * @param k
     * @return the kth largest element.
     * @throws IllegalArgumentException if the length of the specified array is less than <code>k</code>.
     */
    public static <T extends Comparable<T>> T kthLargest(final T[] a, final int k) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return Array.kthLargest(a, k);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @param k
     * @return the kth largest element from <code>a[from]</code> to <code>a[to]</code>
     * @throws IllegalArgumentException if <code>to - from</code> is less than <code>k</code>.
     */
    public static <T extends Comparable<T>> T kthLargest(final T[] a, final int from, final int to, final int k) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        return Array.kthLargest(a, from, to, k);
    }

    /**
     *
     * @param a
     * @param k
     * @param cmp
     * @return the kth largest element.
     * @throws IllegalArgumentException if the length of the specified array is less than <code>k</code>.
     */
    public static <T> T kthLargest(final T[] a, final int k, final Comparator<? super T> cmp) {
        N.checkArgNotNullOrEmpty(a, "The spcified array 'a' can not be null or empty");

        return Array.kthLargest(a, k, cmp);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @param k
     * @param cmp
     * @return the kth largest element from <code>a[from]</code> to <code>a[to]</code>
     * @throws IllegalArgumentException if <code>to - from</code> is less than <code>k</code>.
     */
    public static <T> T kthLargest(final T[] a, final int from, final int to, final int k, final Comparator<? super T> cmp) {
        if (N.isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The length of array can not be null or empty");
        }

        return Array.kthLargest(a, from, to, k, cmp);
    }

    /**
     *
     * @param c
     * @param k
     * @return the kth largest element.
     * @throws IllegalArgumentException if the length of the specified array is less than <code>k</code>.
     */
    public static <T extends Comparable<T>> T kthLargest(final Collection<? extends T> c, final int k) {
        N.checkArgNotNullOrEmpty(c, "The spcified collection 'c' can not be null or empty");

        return Array.kthLargest(c, k);
    }

    /**
     *
     * @param c
     * @param from
     * @param to
     * @param k
     * @return the kth largest element from <code>a[from]</code> to <code>a[to]</code>
     * @throws IllegalArgumentException if <code>to - from</code> is less than <code>k</code>.
     */
    public static <T extends Comparable<T>> T kthLargest(final Collection<? extends T> c, final int from, final int to, final int k) {
        if (N.isNullOrEmpty(c) || to - from < 1) {
            throw new IllegalArgumentException("The length of collection can not be null or empty");
        }

        return Array.kthLargest(c, from, to, k);
    }

    /**
     *
     * @param c
     * @param k
     * @param cmp
     * @return the kth largest element.
     * @throws IllegalArgumentException if the length of the specified array is less than <code>k</code>.
     */
    public static <T> T kthLargest(final Collection<? extends T> c, final int k, final Comparator<? super T> cmp) {
        N.checkArgNotNullOrEmpty(c, "The spcified collection 'c' can not be null or empty");

        return Array.kthLargest(c, k, cmp);
    }

    /**
     *
     * @param c
     * @param from
     * @param to
     * @param k
     * @param cmp
     * @return the kth largest element from <code>a[from]</code> to <code>a[to]</code>
     * @throws IllegalArgumentException if <code>to - from</code> is less than <code>k</code>.
     */
    public static <T> T kthLargest(final Collection<? extends T> c, final int from, final int to, final int k, final Comparator<? super T> cmp) {
        if (N.isNullOrEmpty(c) || to - from < 1) {
            throw new IllegalArgumentException("The length of collection can not be null or empty");
        }

        return Array.kthLargest(c, from, to, k, cmp);
    }

    /**
     * Returns the elements at: <code>Percentage</code> * length of the specified array.
     *
     * @param sortedArray
     * @return
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is null or empty.
     */
    public static Map<Percentage, Character> percentiles(final char[] sortedArray) {
        N.checkArgNotNullOrEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty");

        final int len = sortedArray.length;
        final Map<Percentage, Character> m = new LinkedHashMap<>(N.initHashCapacity(Percentage.values().length));

        for (Percentage p : Percentage.values()) {
            m.put(p, sortedArray[(int) (len * p.doubleValue())]);
        }

        return ImmutableMap.of(m);
    }

    /**
     * Returns the elements at: <code>Percentage</code> * length of the specified array.
     *
     * @param sortedArray
     * @return
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is null or empty.
     */
    public static Map<Percentage, Byte> percentiles(final byte[] sortedArray) {
        N.checkArgNotNullOrEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty");

        final int len = sortedArray.length;
        final Map<Percentage, Byte> m = new LinkedHashMap<>(N.initHashCapacity(Percentage.values().length));

        for (Percentage p : Percentage.values()) {
            m.put(p, sortedArray[(int) (len * p.doubleValue())]);
        }

        return ImmutableMap.of(m);
    }

    /**
     * Returns the elements at: <code>Percentage</code> * length of the specified array.
     *
     * @param sortedArray
     * @return
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is null or empty.
     */
    public static Map<Percentage, Short> percentiles(final short[] sortedArray) {
        N.checkArgNotNullOrEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty");

        final int len = sortedArray.length;
        final Map<Percentage, Short> m = new LinkedHashMap<>(N.initHashCapacity(Percentage.values().length));

        for (Percentage p : Percentage.values()) {
            m.put(p, sortedArray[(int) (len * p.doubleValue())]);
        }

        return ImmutableMap.of(m);
    }

    /**
     * Returns the elements at: <code>Percentage</code> * length of the specified array.
     *
     * @param sortedArray
     * @return
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is null or empty.
     */
    public static Map<Percentage, Integer> percentiles(final int[] sortedArray) {
        N.checkArgNotNullOrEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty");

        final int len = sortedArray.length;
        final Map<Percentage, Integer> m = new LinkedHashMap<>(N.initHashCapacity(Percentage.values().length));

        for (Percentage p : Percentage.values()) {
            m.put(p, sortedArray[(int) (len * p.doubleValue())]);
        }

        return ImmutableMap.of(m);
    }

    /**
     * Returns the elements at: <code>Percentage</code> * length of the specified array.
     *
     * @param sortedArray
     * @return
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is null or empty.
     */
    public static Map<Percentage, Long> percentiles(final long[] sortedArray) {
        N.checkArgNotNullOrEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty");

        final int len = sortedArray.length;
        final Map<Percentage, Long> m = new LinkedHashMap<>(N.initHashCapacity(Percentage.values().length));

        for (Percentage p : Percentage.values()) {
            m.put(p, sortedArray[(int) (len * p.doubleValue())]);
        }

        return ImmutableMap.of(m);
    }

    /**
     * Returns the elements at: <code>Percentage</code> * length of the specified array.
     *
     * @param sortedArray
     * @return
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is null or empty.
     */
    public static Map<Percentage, Float> percentiles(final float[] sortedArray) {
        N.checkArgNotNullOrEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty");

        final int len = sortedArray.length;
        final Map<Percentage, Float> m = new LinkedHashMap<>(N.initHashCapacity(Percentage.values().length));

        for (Percentage p : Percentage.values()) {
            m.put(p, sortedArray[(int) (len * p.doubleValue())]);
        }

        return ImmutableMap.of(m);
    }

    /**
     * Returns the elements at: <code>Percentage</code> * length of the specified array.
     *
     * @param sortedArray
     * @return
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is null or empty.
     */
    public static Map<Percentage, Double> percentiles(final double[] sortedArray) {
        N.checkArgNotNullOrEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty");

        final int len = sortedArray.length;
        final Map<Percentage, Double> m = new LinkedHashMap<>(N.initHashCapacity(Percentage.values().length));

        for (Percentage p : Percentage.values()) {
            m.put(p, sortedArray[(int) (len * p.doubleValue())]);
        }

        return ImmutableMap.of(m);
    }

    /**
     * Returns the elements at: <code>Percentage</code> * length of the specified array.
     *
     * @param sortedArray
     * @return
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is null or empty.
     */
    public static <T> Map<Percentage, T> percentiles(final T[] sortedArray) {
        N.checkArgNotNullOrEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty");

        final int len = sortedArray.length;
        final Map<Percentage, T> m = new LinkedHashMap<>(N.initHashCapacity(Percentage.values().length));

        for (Percentage p : Percentage.values()) {
            m.put(p, sortedArray[(int) (len * p.doubleValue())]);
        }

        return ImmutableMap.of(m);
    }

    /**
     * Returns the elements at: <code>Percentage</code> * length of the specified array.
     *
     * @param sortedArray
     * @return
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is null or empty.
     */
    public static <T> Map<Percentage, T> percentiles(final List<T> sortedList) {
        N.checkArgNotNullOrEmpty(sortedList, "The spcified 'sortedList' can not be null or empty");

        final int size = sortedList.size();
        final Map<Percentage, T> m = new LinkedHashMap<>(N.initHashCapacity(Percentage.values().length));

        for (Percentage p : Percentage.values()) {
            m.put(p, sortedList.get((int) (size * p.doubleValue())));
        }

        return ImmutableMap.of(m);
    }

    public static String toJSON(final Object obj) {
        return Utils.jsonParser.serialize(obj, Utils.jsc);
    }

    public static String toJSON(final Object obj, final boolean prettyFormat) {
        return Utils.jsonParser.serialize(obj, prettyFormat ? Utils.jscPrettyFormat : Utils.jsc);
    }

    public static String toJSON(final Object obj, final JSONSerializationConfig config) {
        return Utils.jsonParser.serialize(obj, config);
    }

    public static void toJSON(final File file, final Object obj) {
        Utils.jsonParser.serialize(file, obj);
    }

    public static void toJSON(final File file, final Object obj, final JSONSerializationConfig config) {
        Utils.jsonParser.serialize(file, obj, config);
    }

    public static void toJSON(final OutputStream os, final Object obj) {
        Utils.jsonParser.serialize(os, obj);
    }

    public static void toJSON(final OutputStream os, final Object obj, final JSONSerializationConfig config) {
        Utils.jsonParser.serialize(os, obj, config);
    }

    public static void toJSON(final Writer writer, final Object obj) {
        Utils.jsonParser.serialize(writer, obj);
    }

    public static void toJSON(final Writer writer, final Object obj, final JSONSerializationConfig config) {
        Utils.jsonParser.serialize(writer, obj, config);
    }

    public static <T> T fromJSON(final Class<? extends T> targetClass, final String json) {
        return Utils.jsonParser.deserialize(targetClass, json);
    }

    public static <T> T fromJSON(final Class<? extends T> targetClass, final String json, final JSONDeserializationConfig config) {
        return Utils.jsonParser.deserialize(targetClass, json, config);
    }

    public static <T> T fromJSON(final Class<? extends T> targetClass, final File json) {
        return Utils.jsonParser.deserialize(targetClass, json);
    }

    public static <T> T fromJSON(final Class<? extends T> targetClass, final File json, final JSONDeserializationConfig config) {
        return Utils.jsonParser.deserialize(targetClass, json, config);
    }

    public static <T> T fromJSON(final Class<? extends T> targetClass, final InputStream json) {
        return Utils.jsonParser.deserialize(targetClass, json);
    }

    public static <T> T fromJSON(final Class<? extends T> targetClass, final InputStream json, final JSONDeserializationConfig config) {
        return Utils.jsonParser.deserialize(targetClass, json, config);
    }

    public static <T> T fromJSON(final Class<? extends T> targetClass, final Reader json) {
        return Utils.jsonParser.deserialize(targetClass, json);
    }

    public static <T> T fromJSON(final Class<? extends T> targetClass, final Reader json, final JSONDeserializationConfig config) {
        return Utils.jsonParser.deserialize(targetClass, json, config);
    }

    public static <T> T fromJSON(final Class<? extends T> targetClass, final String json, final int fromIndex, final int toIndex) {
        return Utils.jsonParser.deserialize(targetClass, json, fromIndex, toIndex);
    }

    public static <T> T fromJSON(final Class<? extends T> targetClass, final String json, final int fromIndex, final int toIndex,
            final JSONDeserializationConfig config) {
        return Utils.jsonParser.deserialize(targetClass, json, fromIndex, toIndex, config);
    }

    /**
     *
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param json
     * @return
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final String json) {
        return fromJSON(targetType, json, null);
    }

    /**
     *
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param json
     * @param config
     * @return
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final String json, final JSONDeserializationConfig config) {
        return Utils.jsonParser.deserialize(targetType.clazz(), json, setConfig(targetType, config, true));
    }

    /**
     *
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param json
     * @return
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final File json) {
        return fromJSON(targetType, json, null);
    }

    /**
     *
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param json
     * @param config
     * @return
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final File json, final JSONDeserializationConfig config) {
        return Utils.jsonParser.deserialize(targetType.clazz(), json, setConfig(targetType, config, true));
    }

    /**
     *
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param json
     * @return
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final InputStream json) {
        return fromJSON(targetType, json, null);
    }

    /**
     *
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param json
     * @param config
     * @return
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final InputStream json, final JSONDeserializationConfig config) {
        return Utils.jsonParser.deserialize(targetType.clazz(), json, setConfig(targetType, config, true));
    }

    /**
     *
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param json
     * @return
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final Reader json) {
        return fromJSON(targetType, json, null);
    }

    /**
     *
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param json
     * @param config
     * @return
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final Reader json, final JSONDeserializationConfig config) {
        return Utils.jsonParser.deserialize(targetType.clazz(), json, setConfig(targetType, config, true));
    }

    /**
     *
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param json
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final String json, final int fromIndex, final int toIndex) {
        return fromJSON(targetType, json, fromIndex, toIndex, null);
    }

    /**
     *
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param json
     * @param fromIndex
     * @param toIndex
     * @param config
     * @return
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final String json, final int fromIndex, final int toIndex,
            final JSONDeserializationConfig config) {
        return Utils.jsonParser.deserialize(targetType.clazz(), json, fromIndex, toIndex, setConfig(targetType, config, true));
    }

    public static String formatJSON(final String json) {
        return formatJSON(Object.class, json);
    }

    public static String formatJSON(final Class<?> type, final String json) {
        return toJSON(fromJSON(type, json), Utils.jscPrettyFormat);
    }

    public static String formatJSON(final Type<?> type, final String json) {
        return toJSON(fromJSON(type, json), Utils.jscPrettyFormat);
    }

    public static String toXML(final Object obj) {
        return Utils.xmlParser.serialize(obj);
    }

    public static String toXML(final Object obj, final boolean prettyFormat) {
        return Utils.xmlParser.serialize(obj, prettyFormat ? Utils.xscPrettyFormat : Utils.xsc);
    }

    public static String toXML(final Object obj, final XMLSerializationConfig config) {
        return Utils.xmlParser.serialize(obj, config);
    }

    public static void toXML(final File file, final Object obj) {
        Utils.xmlParser.serialize(file, obj);
    }

    public static void toXML(final File file, final Object obj, final XMLSerializationConfig config) {
        Utils.xmlParser.serialize(file, obj, config);
    }

    public static void toXML(final OutputStream os, final Object obj) {
        Utils.xmlParser.serialize(os, obj);
    }

    public static void toXML(final OutputStream os, final Object obj, final XMLSerializationConfig config) {
        Utils.xmlParser.serialize(os, obj, config);
    }

    public static void toXML(final Writer writer, final Object obj) {
        Utils.xmlParser.serialize(writer, obj);
    }

    public static void toXML(final Writer writer, final Object obj, final XMLSerializationConfig config) {
        Utils.xmlParser.serialize(writer, obj, config);
    }

    public static <T> T fromXML(final Class<? extends T> targetClass, final String xml) {
        return Utils.xmlParser.deserialize(targetClass, xml);
    }

    public static <T> T fromXML(final Class<? extends T> targetClass, final String xml, final XMLDeserializationConfig config) {
        return Utils.xmlParser.deserialize(targetClass, xml, config);
    }

    public static <T> T fromXML(final Class<? extends T> targetClass, final File xml) {
        return Utils.xmlParser.deserialize(targetClass, xml);
    }

    public static <T> T fromXML(final Class<? extends T> targetClass, final File xml, final XMLDeserializationConfig config) {
        return Utils.xmlParser.deserialize(targetClass, xml, config);
    }

    public static <T> T fromXML(final Class<? extends T> targetClass, final InputStream xml) {
        return Utils.xmlParser.deserialize(targetClass, xml);
    }

    public static <T> T fromXML(final Class<? extends T> targetClass, final InputStream xml, final XMLDeserializationConfig config) {
        return Utils.xmlParser.deserialize(targetClass, xml, config);
    }

    public static <T> T fromXML(final Class<? extends T> targetClass, final Reader xml) {
        return Utils.xmlParser.deserialize(targetClass, xml);
    }

    public static <T> T fromXML(final Class<? extends T> targetClass, final Reader xml, final XMLDeserializationConfig config) {
        return Utils.xmlParser.deserialize(targetClass, xml, config);
    }

    /**
     *
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param xml
     * @return
     */
    public static <T> T fromXML(final Type<? extends T> targetType, final String xml) {
        return fromJSON(targetType, xml, null);
    }

    /**
     *
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param xml
     * @param config
     * @return
     */
    public static <T> T fromXML(final Type<? extends T> targetType, final String xml, final XMLDeserializationConfig config) {
        return Utils.xmlParser.deserialize(targetType.clazz(), xml, setConfig(targetType, config, false));
    }

    /**
     *
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param xml
     * @return
     */
    public static <T> T fromXML(final Type<? extends T> targetType, final File xml) {
        return fromJSON(targetType, xml, null);
    }

    /**
     *
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param xml
     * @param config
     * @return
     */
    public static <T> T fromXML(final Type<? extends T> targetType, final File xml, final XMLDeserializationConfig config) {
        return Utils.xmlParser.deserialize(targetType.clazz(), xml, setConfig(targetType, config, false));
    }

    /**
     *
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param xml
     * @return
     */
    public static <T> T fromXML(final Type<? extends T> targetType, final InputStream xml) {
        return fromJSON(targetType, xml, null);
    }

    /**
     *
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param xml
     * @param config
     * @return
     */
    public static <T> T fromXML(final Type<? extends T> targetType, final InputStream xml, final XMLDeserializationConfig config) {
        return Utils.xmlParser.deserialize(targetType.clazz(), xml, setConfig(targetType, config, false));
    }

    /**
     *
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param xml
     * @return
     */
    public static <T> T fromXML(final Type<? extends T> targetType, final Reader xml) {
        return fromJSON(targetType, xml, null);
    }

    /**
     *
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param xml
     * @param config
     * @return
     */
    public static <T> T fromXML(final Type<? extends T> targetType, final Reader xml, final XMLDeserializationConfig config) {
        return Utils.xmlParser.deserialize(targetType.clazz(), xml, setConfig(targetType, config, false));
    }

    private static <C extends DeserializationConfig<C>> C setConfig(final Type<?> targetType, final C config, boolean isJSON) {
        C res = config;

        if (targetType.isCollection()) {
            if (config == null || config.getElementType() == null) {
                res = config == null ? (C) (isJSON ? JDC.create() : XDC.create()) : (C) config.copy();
                res.setElementType(targetType.getParameterTypes()[0]);
            }
        } else if (targetType.isMap()) {
            if (config == null || config.getMapKeyType() == null || config.getMapValueType() == null) {
                res = config == null ? (C) (isJSON ? JDC.create() : XDC.create()) : (C) config.copy();

                if (res.getMapKeyType() == null) {
                    res.setMapKeyType(targetType.getParameterTypes()[0]);
                }

                if (res.getMapValueType() == null) {
                    res.setMapValueType(targetType.getParameterTypes()[1]);
                }
            }
        }

        return res;
    }

    public static String xml2JSON(final String xml) {
        return xml2JSON(Map.class, xml);
    }

    public static String xml2JSON(final Class<?> cls, final String xml) {
        return Utils.jsonParser.serialize(Utils.xmlParser.deserialize(cls, xml), Utils.jsc);
    }

    public static String json2XML(final String json) {
        return json2XML(Map.class, json);
    }

    public static String json2XML(final Class<?> cls, final String json) {
        return Utils.xmlParser.serialize(Utils.jsonParser.deserialize(cls, json));
    }

    public static void execute(final Try.Runnable<? extends Exception> cmd, final int retryTimes, final long retryInterval,
            final Predicate<? super Exception> retryCondition) {
        try {
            Retry.of(retryTimes, retryInterval, retryCondition).run(cmd);
        } catch (Exception e) {
            throw N.toRuntimeException(e);
        }
    }

    public static <T> T execute(final Callable<T> cmd, final int retryTimes, final long retryInterval,
            final BiPredicate<? super T, ? super Exception> retryCondition) {
        try {
            final Retry<T> retry = Retry.of(retryTimes, retryInterval, retryCondition);
            return retry.call(cmd);
        } catch (Exception e) {
            throw N.toRuntimeException(e);
        }
    }

    public static ContinuableFuture<Void> asyncExecute(final Try.Runnable<? extends Exception> command) {
        return asyncExecutor.execute(command);
    }

    public static ContinuableFuture<Void> asyncExecute(final Try.Runnable<? extends Exception> command, final long delay) {
        return asyncExecutor.execute(command, delay);
    }

    @SafeVarargs
    public static List<ContinuableFuture<Void>> asyncExecute(final Try.Runnable<? extends Exception>... commands) {
        return asyncExecutor.execute(commands);
    }

    public static List<ContinuableFuture<Void>> asyncExecute(final List<? extends Try.Runnable<? extends Exception>> commands) {
        return asyncExecutor.execute(commands);
    }

    public static <T> ContinuableFuture<T> asyncExecute(final Callable<T> command) {
        return asyncExecutor.execute(command);
    }

    public static <T> ContinuableFuture<T> asyncExecute(final Callable<T> command, final long delay) {
        return asyncExecutor.execute(command, delay);
    }

    @SafeVarargs
    public static <T> List<ContinuableFuture<T>> asyncExecute(final Callable<T>... commands) {
        return asyncExecutor.execute(commands);
    }

    public static <T> List<ContinuableFuture<T>> asyncExecute(final Collection<? extends Callable<T>> commands) {
        return asyncExecutor.execute(commands);
    }

    public static ContinuableFuture<Void> asyncExecute(final Try.Runnable<? extends Exception> cmd, final int retryTimes, final long retryInterval,
            final Predicate<? super Exception> retryCondition) {
        return asyncExecutor.execute(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Retry.of(retryTimes, retryInterval, retryCondition).run(cmd);
                return null;
            }
        });
    }

    public static <T> ContinuableFuture<T> asyncExecute(final Callable<T> cmd, final int retryTimes, final long retryInterval,
            final BiPredicate<? super T, ? super Exception> retryCondition) {
        return asyncExecutor.execute(new Callable<T>() {
            @Override
            public T call() throws Exception {
                final Retry<T> retry = Retry.of(retryTimes, retryInterval, retryCondition);
                return retry.call(cmd);
            }
        });
    }

    private static final Map<Class<? extends Throwable>, Function<Throwable, RuntimeException>> toRuntimeExceptionFuncMap = new HashMap<>();
    static {
        toRuntimeExceptionFuncMap.put(RuntimeException.class, new Function<Throwable, RuntimeException>() {
            @Override
            public RuntimeException apply(Throwable e) {
                return (RuntimeException) e;
            }
        });

        toRuntimeExceptionFuncMap.put(IOException.class, new Function<Throwable, RuntimeException>() {
            @Override
            public RuntimeException apply(Throwable e) {
                return new UncheckedIOException((IOException) e);
            }
        });

        toRuntimeExceptionFuncMap.put(SQLException.class, new Function<Throwable, RuntimeException>() {
            @Override
            public RuntimeException apply(Throwable e) {
                return new UncheckedSQLException((SQLException) e);
            }
        });

        toRuntimeExceptionFuncMap.put(ExecutionException.class, new Function<Throwable, RuntimeException>() {
            @Override
            public RuntimeException apply(Throwable e) {
                return e.getCause() == null ? new UncheckedException(e) : toRuntimeException(e.getCause());
            }
        });

        toRuntimeExceptionFuncMap.put(InvocationTargetException.class, new Function<Throwable, RuntimeException>() {
            @Override
            public RuntimeException apply(Throwable e) {
                return e.getCause() == null ? new UncheckedException(e) : toRuntimeException(e.getCause());
            }
        });

        toRuntimeExceptionFuncMap.put(UndeclaredThrowableException.class, new Function<Throwable, RuntimeException>() {
            @Override
            public RuntimeException apply(Throwable e) {
                return e.getCause() == null ? new UncheckedException(e) : toRuntimeException(e.getCause());
            }
        });
    }

    public static RuntimeException toRuntimeException(Throwable e) {
        final Function<Throwable, RuntimeException> func = toRuntimeExceptionFuncMap.get(e.getClass());

        if (func == null) {
            return e instanceof RuntimeException ? (RuntimeException) e : new UncheckedException(e);
        } else {
            return func.apply(e);
        }
    }

    @Beta
    public static <E extends Throwable> boolean mayCausedBy(Throwable e, final Class<E> exceptionClass) {
        N.checkArgNotNull(e, "e");
        N.checkArgNotNull(exceptionClass, "exceptionClass");

        while (e != null) {
            if (exceptionClass.isAssignableFrom(e.getClass())) {
                return true;
            }

            e = e.getCause();
        }

        return false;
    }

    public static void sleep(final long timeoutInMillis) {
        if (timeoutInMillis <= 0) {
            return;
        }

        try {
            TimeUnit.MILLISECONDS.sleep(timeoutInMillis);
        } catch (InterruptedException e) {
            throw new UncheckedException(e);
        }
    }

    public static void sleep(final long timeout, final TimeUnit unit) {
        N.checkArgNotNull(unit, "unit");

        if (timeout <= 0) {
            return;
        }

        try {
            unit.sleep(timeout);
        } catch (InterruptedException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Note: Copied from Google Guava under Apache License v2.0
     * <br />
     * <br />
     *
     * If a thread is interrupted during such a call, the call continues to block until the result is available or the
     * timeout elapses, and only then re-interrupts the thread.
     *
     * @param timeoutInMillis
     */
    public static void sleepUninterruptibly(final long timeoutInMillis) {
        if (timeoutInMillis <= 0) {
            return;
        }

        boolean interrupted = false;

        try {
            long remainingNanos = TimeUnit.MILLISECONDS.toNanos(timeoutInMillis);
            final long sysNanos = System.nanoTime();
            final long end = remainingNanos >= Long.MAX_VALUE - sysNanos ? Long.MAX_VALUE : sysNanos + remainingNanos;

            while (true) {
                try {
                    // TimeUnit.sleep() treats negative timeouts just like zero.
                    TimeUnit.NANOSECONDS.sleep(remainingNanos);
                    return;
                } catch (InterruptedException e) {
                    interrupted = true;
                    remainingNanos = end - System.nanoTime();
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Note: Copied from Google Guava under Apache License v2.0
     * <br />
     * <br />
     *
     * If a thread is interrupted during such a call, the call continues to block until the result is available or the
     * timeout elapses, and only then re-interrupts the thread.
     *
     * @param timeoutInMillis
     */
    public static void sleepUninterruptibly(final long timeout, final TimeUnit unit) {
        N.checkArgNotNull(unit, "unit");

        if (timeout <= 0) {
            return;
        }

        boolean interrupted = false;

        try {
            long remainingNanos = unit.toNanos(timeout);
            final long sysNanos = System.nanoTime();
            final long end = remainingNanos >= Long.MAX_VALUE - sysNanos ? Long.MAX_VALUE : sysNanos + remainingNanos;

            while (true) {
                try {
                    // TimeUnit.sleep() treats negative timeouts just like zero.
                    TimeUnit.NANOSECONDS.sleep(remainingNanos);
                    return;
                } catch (InterruptedException e) {
                    interrupted = true;
                    remainingNanos = end - System.nanoTime();
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Note: Copied from Google Guava under Apache License v2.0
     * <br />
     * <br />
     *
     * If a thread is interrupted during such a call, the call continues to block until the result is available or the
     * timeout elapses, and only then re-interrupts the thread.
     *
     * @param cmd
     */
    public static void runUninterruptibly(final Try.Runnable<InterruptedException> cmd) {
        N.checkArgNotNull(cmd);

        boolean interrupted = false;

        try {
            while (true) {
                try {
                    cmd.run();
                    return;
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Note: Copied from Google Guava under Apache License v2.0
     * <br />
     * <br />
     *
     * If a thread is interrupted during such a call, the call continues to block until the result is available or the
     * timeout elapses, and only then re-interrupts the thread.
     *
     * @param timeoutInMillis
     * @param cmd
     */
    public static void runUninterruptibly(final long timeoutInMillis, final Try.LongConsumer<InterruptedException> cmd) {
        N.checkArgNotNull(cmd);

        boolean interrupted = false;

        try {
            long remainingMillis = timeoutInMillis;
            final long sysMillis = System.currentTimeMillis();
            final long end = remainingMillis >= Long.MAX_VALUE - sysMillis ? Long.MAX_VALUE : sysMillis + remainingMillis;

            while (true) {
                try {
                    cmd.accept(remainingMillis);
                    return;
                } catch (InterruptedException e) {
                    interrupted = true;
                    remainingMillis = end - System.currentTimeMillis();
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Note: Copied from Google Guava under Apache License v2.0
     * <br />
     * <br />
     *
     * If a thread is interrupted during such a call, the call continues to block until the result is available or the
     * timeout elapses, and only then re-interrupts the thread.
     *
     * @param timeout
     * @param unit
     * @param cmd
     */
    public static void runUninterruptibly(final long timeout, final TimeUnit unit, final Try.BiConsumer<Long, TimeUnit, InterruptedException> cmd) {
        N.checkArgNotNull(unit, "unit");
        N.checkArgNotNull(cmd);

        boolean interrupted = false;

        try {
            long remainingNanos = unit.toNanos(timeout);
            final long sysNanos = System.nanoTime();
            final long end = remainingNanos >= Long.MAX_VALUE - sysNanos ? Long.MAX_VALUE : sysNanos + remainingNanos;

            while (true) {
                try {
                    cmd.accept(remainingNanos, TimeUnit.NANOSECONDS);
                    return;
                } catch (InterruptedException e) {
                    interrupted = true;
                    remainingNanos = end - System.nanoTime();
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Note: Copied from Google Guava under Apache License v2.0
     * <br />
     * <br />
     *
     * If a thread is interrupted during such a call, the call continues to block until the result is available or the
     * timeout elapses, and only then re-interrupts the thread.
     *
     * @param cmd
     * @return
     */
    public static <T> T callUninterruptibly(Try.Callable<T, InterruptedException> cmd) {
        N.checkArgNotNull(cmd);

        boolean interrupted = false;
        try {
            while (true) {
                try {
                    return cmd.call();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Note: Copied from Google Guava under Apache License v2.0
     * <br />
     * <br />
     *
     * If a thread is interrupted during such a call, the call continues to block until the result is available or the
     * timeout elapses, and only then re-interrupts the thread.
     *
     * @param timeoutInMillis
     * @param cmd
     * @return
     */
    public static <T> T callUninterruptibly(final long timeoutInMillis, final Try.LongFunction<T, InterruptedException> cmd) {
        N.checkArgNotNull(cmd);

        boolean interrupted = false;

        try {
            long remainingMillis = timeoutInMillis;
            final long sysMillis = System.currentTimeMillis();
            final long end = remainingMillis >= Long.MAX_VALUE - sysMillis ? Long.MAX_VALUE : sysMillis + remainingMillis;

            while (true) {
                try {
                    return cmd.apply(remainingMillis);
                } catch (InterruptedException e) {
                    interrupted = true;
                    remainingMillis = end - System.currentTimeMillis();
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Note: Copied from Google Guava under Apache License v2.0
     * <br />
     * <br />
     *
     * If a thread is interrupted during such a call, the call continues to block until the result is available or the
     * timeout elapses, and only then re-interrupts the thread.
     *
     * @param timeout
     * @param unit
     * @param cmd
     * @return
     */
    public static <T> T callUninterruptibly(final long timeout, final TimeUnit unit, final Try.BiFunction<Long, TimeUnit, T, InterruptedException> cmd) {
        N.checkArgNotNull(unit, "unit");
        N.checkArgNotNull(cmd);

        boolean interrupted = false;

        try {
            long remainingNanos = unit.toNanos(timeout);
            final long sysNanos = System.nanoTime();
            final long end = remainingNanos >= Long.MAX_VALUE - sysNanos ? Long.MAX_VALUE : sysNanos + remainingNanos;

            while (true) {
                try {
                    return cmd.apply(remainingNanos, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    interrupted = true;
                    remainingNanos = end - System.nanoTime();
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     *
     * @param obj
     * @return the input <code>obj</code>
     */
    @SuppressWarnings("rawtypes")
    public static <T> T println(final T obj) {
        if (obj instanceof Collection) {
            System.out.println(Joiner.with(N.ELEMENT_SEPARATOR, "[", "]").reuseCachedBuffer(true).appendAll((Collection) obj));
        } else if (obj instanceof Map) {
            System.out.println(Joiner.with(N.ELEMENT_SEPARATOR, "=", "{", "}").reuseCachedBuffer(true).appendEntries((Map) obj));
        } else {
            System.out.println(N.toString(obj));
        }

        return obj;
    }

    /**
     *
     * @param format
     * @param args
     * @return the input <code>args</code>
     */
    @SafeVarargs
    public static <T> T[] fprintln(final String format, final T... args) {
        System.out.printf(format, args);
        System.out.println();
        return args;
    }

    /**
     * Returns the value of the {@code long} argument; throwing an exception if the value overflows an {@code int}.
     *
     * @param value the long value
     * @return the argument as an int
     * @throws ArithmeticException if the {@code argument} overflows an int
     */
    public static int toIntExact(long value) {
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new ArithmeticException("integer overflow");
        }

        return (int) value;
    }

    /**
     * Returns an empty <code>Nullable</code> if {@code val} is {@code null} while {@code targetType} is primitive or can not be assigned to {@code targetType}.
     * Please be aware that {@code null} can be assigned to any {@code Object} type except primitive types: {@code boolean/char/byte/short/int/long/double}.
     *
     * @param val
     * @param targetType
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> Nullable<T> castIfAssignable(final Object val, final Class<T> targetType) {
        if (Primitives.isPrimitiveType(targetType)) {
            return val != null && Primitives.wrap(targetType).isAssignableFrom(val.getClass()) ? Nullable.of((T) val) : Nullable.<T> empty();
        }

        return val == null || targetType.isAssignableFrom(val.getClass()) ? Nullable.of((T) val) : Nullable.<T> empty();
    }

    /**
     * Returns a {@code Nullable} with the value returned by {@code action} or an empty {@code Nullable} if exception happens.
     *
     * @param cmd
     * @return
     */
    public static <R> Nullable<R> tryOrEmpty(final Callable<R> cmd) {
        try {
            return Nullable.of(cmd.call());
        } catch (Exception e) {
            return Nullable.<R> empty();
        }
    }

    /**
     * Returns a {@code Nullable} with the value returned by {@code func.apply(init)} or an empty {@code Nullable} if exception happens.
     *
     * @param init
     * @param func
     * @return
     */
    public static <T, R, E extends Exception> Nullable<R> tryOrEmpty(final T init, final Try.Function<? super T, R, E> func) {
        try {
            return Nullable.of(func.apply(init));
        } catch (Exception e) {
            return Nullable.<R> empty();
        }
    }

    /**
     * Returns a {@code Nullable} with value got from the specified {@code supplier} if {@code b} is {@code true},
     * otherwise returns an empty {@code Nullable} if {@code b} is false.
     *
     * @param b
     * @param supplier
     * @return
     * @throws E
     */
    public static <R, E extends Exception> Nullable<R> ifOrEmpty(final boolean b, final Try.Supplier<R, E> supplier) throws E {
        if (b) {
            return Nullable.of(supplier.get());
        } else {
            return Nullable.empty();
        }
    }

    /**
     * Returns a {@code Nullable} with value returned by {@code func.apply(init)} if {@code b} is {@code true},
     * otherwise returns an empty {@code Nullable} if {@code b} is false.
     *
     * @param b
     * @param init
     * @param func
     * @return
     * @throws E
     */
    public static <T, R, E extends Exception> Nullable<R> ifOrEmpty(final boolean b, final T init, final Try.Function<? super T, R, E> func) throws E {
        if (b) {
            return Nullable.of(func.apply(init));
        } else {
            return Nullable.empty();
        }
    }

    /**
     *
     * @param b
     * @param actionForTrue do nothing if it's {@code null} even {@code b} is true.
     * @param actionForFalse do nothing if it's {@code null} even {@code b} is false.
     * @throws E1
     * @throws E2
     */
    public static <E1 extends Exception, E2 extends Exception> void ifOrElse(final boolean b, final Try.Runnable<E1> actionForTrue,
            final Try.Runnable<E2> actionForFalse) throws E1, E2 {
        if (b) {
            if (actionForTrue != null) {
                actionForTrue.run();
            }
        } else {
            if (actionForFalse != null) {
                actionForFalse.run();
            }
        }
    }

    /**
     *
     * @param b
     * @param init
     * @param actionForTrue do nothing if it's {@code null} even {@code b} is true.
     * @param actionForFalse do nothing if it's {@code null} even {@code b} is false.
     * @throws E1
     * @throws E2
     */
    public static <T, E1 extends Exception, E2 extends Exception> void ifOrElse(final boolean b, final T init, final Try.Consumer<? super T, E1> actionForTrue,
            final Try.Consumer<? super T, E2> actionForFalse) throws E1, E2 {
        if (b) {
            if (actionForTrue != null) {
                actionForTrue.accept(init);
            }
        } else {
            if (actionForFalse != null) {
                actionForFalse.accept(init);
            }
        }
    }

    //    /**
    //     *
    //     * @param value
    //     * @return
    //     */
    //    public static OptionalBoolean nullable(Boolean value) {
    //        return OptionalBoolean.ofNullable(value);
    //    }
    //
    //    /**
    //     *
    //     * @param value
    //     * @return
    //     */
    //    public static OptionalChar nullable(Character value) {
    //        return OptionalChar.ofNullable(value);
    //    }
    //
    //    /**
    //     *
    //     * @param value
    //     * @return
    //     */
    //    public static OptionalByte nullable(Byte value) {
    //        return OptionalByte.ofNullable(value);
    //    }
    //
    //    /**
    //     *
    //     * @param value
    //     * @return
    //     */
    //    public static OptionalShort nullable(Short value) {
    //        return OptionalShort.ofNullable(value);
    //    }
    //
    //    /**
    //     *
    //     * @param value
    //     * @return
    //     */
    //    public static OptionalInt nullable(Integer value) {
    //        return OptionalInt.ofNullable(value);
    //    }
    //
    //    /**
    //     *
    //     * @param value
    //     * @return
    //     */
    //    public static OptionalLong nullable(Long value) {
    //        return OptionalLong.ofNullable(value);
    //    }
    //
    //    /**
    //     *
    //     * @param value
    //     * @return
    //     */
    //    public static OptionalFloat nullable(Float value) {
    //        return OptionalFloat.ofNullable(value);
    //    }
    //
    //    /**
    //     *
    //     * @param value
    //     * @return
    //     */
    //    public static OptionalDouble nullable(Double value) {
    //        return OptionalDouble.ofNullable(value);
    //    }
    //
    //    /**
    //     *
    //     * @param value
    //     * @return
    //     */
    //    public static <T> Optional<T> nullable(T value) {
    //        return Optional.ofNullable(value);
    //    }

    /**
     *
     * @param a
     * @return
     * @see Iterators#concat(Object[]...)
     */
    @SafeVarargs
    public static <T> ObjIterator<T> iterate(final T[]... a) {
        return Iterators.concat(a);
    }

    /**
     *
     * @param a
     * @return
     * @see Iterators#concat(Collection)
     */
    @SafeVarargs
    public static <T> ObjIterator<T> iterate(final Collection<? extends T>... a) {
        return Iterators.concat(a);
    }

    /**
     *
     * @param c
     * @return
     * @see Iterators#concatt(Collection)
     */
    public static <T> ObjIterator<T> iterate(final Collection<? extends Collection<? extends T>> c) {
        return Iterators.concatt(c);
    }

    public static boolean disjoint(final Object[] a, final Object[] b) {
        if (isNullOrEmpty(a) || isNullOrEmpty(b)) {
            return true;
        }

        return a.length >= b.length ? disjoint(Arrays.asList(a), asSet(b)) : disjoint(asSet(a), Arrays.asList(b));
    }

    /**
     * Returns {@code true} if the two specified arrays have no elements in common.
     *
     * @param a
     * @param b
     * @return {@code true} if the two specified arrays have no elements in common.
     * @see Collections#disjoint(Collection, Collection)
     */
    public static boolean disjoint(final Collection<?> c1, final Collection<?> c2) {
        if (isNullOrEmpty(c1) || isNullOrEmpty(c2)) {
            return true;
        }

        if (c1 instanceof Set || (c2 instanceof Set == false && c1.size() > c2.size())) {
            for (Object e : c2) {
                if (c1.contains(e)) {
                    return false;
                }
            }
        } else {
            for (Object e : c1) {
                if (c2.contains(e)) {
                    return false;
                }
            }
        }

        return true;
    }

    public static <T, E extends Exception> List<T> merge(final T[] a, final T[] b, final Try.BiFunction<? super T, ? super T, Nth, E> nextSelector) throws E {
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? new ArrayList<T>() : asList(b);
        } else if (isNullOrEmpty(b)) {
            return asList(a);
        }

        final List<T> result = new ArrayList<>(a.length + b.length);
        final int lenA = a.length;
        final int lenB = b.length;
        int cursorA = 0;
        int cursorB = 0;

        while (cursorA < lenA || cursorB < lenB) {
            if (cursorA < lenA) {
                if (cursorB < lenB) {
                    if (nextSelector.apply(a[cursorA], b[cursorB]) == Nth.FIRST) {
                        result.add(a[cursorA++]);
                    } else {
                        result.add(b[cursorB++]);
                    }
                } else {
                    result.add(a[cursorA++]);
                }
            } else {
                result.add(b[cursorB++]);
            }
        }

        return result;
    }

    public static <T, E extends Exception> List<T> merge(final Collection<? extends T> a, final Collection<? extends T> b,
            final Try.BiFunction<? super T, ? super T, Nth, E> nextSelector) throws E {
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? new ArrayList<T>() : new ArrayList<T>(b);
        } else if (isNullOrEmpty(b)) {
            return new ArrayList<T>(a);
        }

        final List<T> result = new ArrayList<>(a.size() + b.size());
        final Iterator<? extends T> iterA = a.iterator();
        final Iterator<? extends T> iterB = b.iterator();

        T nextA = null;
        T nextB = null;
        boolean hasNextA = false;
        boolean hasNextB = false;

        while (hasNextA || hasNextB || iterA.hasNext() || iterB.hasNext()) {
            if (hasNextA) {
                if (iterB.hasNext()) {
                    if (nextSelector.apply(nextA, (nextB = iterB.next())) == Nth.FIRST) {
                        hasNextA = false;
                        hasNextB = true;
                        result.add(nextA);
                    } else {
                        result.add(nextB);
                    }
                } else {
                    hasNextA = false;
                    result.add(nextA);
                }
            } else if (hasNextB) {
                if (iterA.hasNext()) {
                    if (nextSelector.apply((nextA = iterA.next()), nextB) == Nth.FIRST) {
                        result.add(nextA);
                    } else {
                        hasNextA = true;
                        hasNextB = false;
                        result.add(nextB);
                    }
                } else {
                    hasNextB = false;
                    result.add(nextB);
                }
            } else if (iterA.hasNext()) {
                if (iterB.hasNext()) {
                    if (nextSelector.apply((nextA = iterA.next()), (nextB = iterB.next())) == Nth.FIRST) {
                        hasNextB = true;
                        result.add(nextA);
                    } else {
                        hasNextA = true;
                        result.add(nextB);
                    }
                } else {
                    result.add(iterA.next());
                }
            } else {
                result.add(iterB.next());
            }
        }

        return result;
    }

    public static <A, B, R, E extends Exception> List<R> zip(final A[] a, final B[] b, final Try.BiFunction<? super A, ? super B, R, E> zipFunction) throws E {
        if (isNullOrEmpty(a) || isNullOrEmpty(b)) {
            return new ArrayList<>();
        }

        final int minLen = min(a.length, b.length);
        final List<R> result = new ArrayList<>(minLen);

        for (int i = 0; i < minLen; i++) {
            result.add(zipFunction.apply(a[i], b[i]));
        }

        return result;
    }

    public static <A, B, R, E extends Exception> List<R> zip(final Collection<A> a, final Collection<B> b,
            final Try.BiFunction<? super A, ? super B, R, E> zipFunction) throws E {
        if (isNullOrEmpty(a) || isNullOrEmpty(b)) {
            return new ArrayList<>();
        }

        final Iterator<A> iterA = a.iterator();
        final Iterator<B> iterB = b.iterator();
        final int minLen = min(a.size(), b.size());
        final List<R> result = new ArrayList<>(minLen);

        for (int i = 0; i < minLen; i++) {
            result.add(zipFunction.apply(iterA.next(), iterB.next()));
        }

        return result;
    }

    public static <A, B, C, R, E extends Exception> List<R> zip(final A[] a, final B[] b, final C[] c,
            final Try.TriFunction<? super A, ? super B, ? super C, R, E> zipFunction) throws E {
        if (isNullOrEmpty(a) || isNullOrEmpty(b) || isNullOrEmpty(c)) {
            return new ArrayList<>();
        }

        final int minLen = min(a.length, b.length, c.length);
        final List<R> result = new ArrayList<>(minLen);

        for (int i = 0; i < minLen; i++) {
            result.add(zipFunction.apply(a[i], b[i], c[i]));
        }

        return result;
    }

    public static <A, B, C, R, E extends Exception> List<R> zip(final Collection<A> a, final Collection<B> b, final Collection<C> c,
            final Try.TriFunction<? super A, ? super B, ? super C, R, E> zipFunction) throws E {
        if (isNullOrEmpty(a) || isNullOrEmpty(b) || isNullOrEmpty(c)) {
            return new ArrayList<>();
        }

        final Iterator<A> iterA = a.iterator();
        final Iterator<B> iterB = b.iterator();
        final Iterator<C> iterC = c.iterator();
        final int minLen = min(a.size(), b.size(), c.size());
        final List<R> result = new ArrayList<>(minLen);

        for (int i = 0; i < minLen; i++) {
            result.add(zipFunction.apply(iterA.next(), iterB.next(), iterC.next()));
        }

        return result;
    }

    public static <A, B, R, E extends Exception> List<R> zip(final A[] a, final B[] b, final A valueForNoneA, final B valueForNoneB,
            final Try.BiFunction<? super A, ? super B, R, E> zipFunction) throws E {
        final int lenA = len(a);
        final int lenB = len(b);
        final int maxLen = max(lenA, lenB);
        final List<R> result = new ArrayList<>(maxLen);

        for (int i = 0; i < maxLen; i++) {
            result.add(zipFunction.apply(i < lenA ? a[i] : valueForNoneA, i < lenB ? b[i] : valueForNoneB));
        }

        return result;
    }

    public static <A, B, R, E extends Exception> List<R> zip(final Collection<A> a, final Collection<B> b, final A valueForNoneA, final B valueForNoneB,
            final Try.BiFunction<? super A, ? super B, R, E> zipFunction) throws E {
        final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a.iterator();
        final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b.iterator();
        final int lenA = size(a);
        final int lenB = size(b);
        final int maxLen = max(lenA, lenB);
        final List<R> result = new ArrayList<>(maxLen);

        for (int i = 0; i < maxLen; i++) {
            result.add(zipFunction.apply(i < lenA ? iterA.next() : valueForNoneA, i < lenB ? iterB.next() : valueForNoneB));
        }

        return result;
    }

    public static <A, B, C, R, E extends Exception> List<R> zip(final A[] a, final B[] b, final C[] c, final A valueForNoneA, final B valueForNoneB,
            final C valueForNoneC, final Try.TriFunction<? super A, ? super B, ? super C, R, E> zipFunction) throws E {
        final int lenA = len(a);
        final int lenB = len(b);
        final int lenC = len(c);
        final int maxLen = max(lenA, lenB, lenC);
        final List<R> result = new ArrayList<>(maxLen);

        for (int i = 0; i < maxLen; i++) {
            result.add(zipFunction.apply(i < lenA ? a[i] : valueForNoneA, i < lenB ? b[i] : valueForNoneB, i < lenC ? c[i] : valueForNoneC));
        }

        return result;
    }

    public static <A, B, C, R, E extends Exception> List<R> zip(final Collection<A> a, final Collection<B> b, final Collection<C> c, final A valueForNoneA,
            final B valueForNoneB, final C valueForNoneC, final Try.TriFunction<? super A, ? super B, ? super C, R, E> zipFunction) throws E {
        final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a.iterator();
        final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b.iterator();
        final Iterator<C> iterC = c == null ? ObjIterator.<C> empty() : c.iterator();
        final int lenA = size(a);
        final int lenB = size(b);
        final int lenC = size(c);
        final int maxLen = max(lenA, lenB, lenC);
        final List<R> result = new ArrayList<>(maxLen);

        for (int i = 0; i < maxLen; i++) {
            result.add(zipFunction.apply(i < lenA ? iterA.next() : valueForNoneA, i < lenB ? iterB.next() : valueForNoneB,
                    i < lenC ? iterC.next() : valueForNoneC));
        }

        return result;
    }

    /**
     *
     * @param c
     * @param unzip the second parameter is an output parameter.
     * @return
     */
    public static <T, L, R, E extends Exception> Pair<List<L>, List<R>> unzip(final Collection<? extends T> c,
            final Try.BiConsumer<? super T, Pair<L, R>, E> unzip) throws E {
        final int len = size(c);

        final List<L> l = new ArrayList<L>(len);
        final List<R> r = new ArrayList<R>(len);
        final Pair<L, R> p = new Pair<>();

        if (notNullOrEmpty(c)) {
            for (T e : c) {
                unzip.accept(e, p);

                l.add(p.left);
                r.add(p.right);
            }
        }

        return Pair.of(l, r);
    }

    /**
     *
     * @param c
     * @param unzip the second parameter is an output parameter.
     * @param supplier
     * @return
     */
    public static <T, L, R, LC extends Collection<L>, RC extends Collection<R>, E extends Exception> Pair<LC, RC> unzip(final Collection<? extends T> c,
            final Try.BiConsumer<? super T, Pair<L, R>, E> unzip, final IntFunction<? extends Collection<?>> supplier) throws E {
        final int len = size(c);

        final LC l = (LC) supplier.apply(len);
        final RC r = (RC) supplier.apply(len);
        final Pair<L, R> p = new Pair<>();

        if (notNullOrEmpty(c)) {
            for (T e : c) {
                unzip.accept(e, p);

                l.add(p.left);
                r.add(p.right);
            }
        }

        return Pair.of(l, r);
    }

    /**
     *
     * @param c
     * @param unzip the second parameter is an output parameter.
     * @return
     */
    public static <T, L, M, R, E extends Exception> Triple<List<L>, List<M>, List<R>> unzipp(final Collection<? extends T> c,
            final Try.BiConsumer<? super T, Triple<L, M, R>, E> unzip) throws E {
        final int len = size(c);

        final List<L> l = new ArrayList<L>(len);
        final List<M> m = new ArrayList<M>(len);
        final List<R> r = new ArrayList<R>(len);
        final Triple<L, M, R> t = new Triple<>();

        if (notNullOrEmpty(c)) {
            for (T e : c) {
                unzip.accept(e, t);

                l.add(t.left);
                m.add(t.middle);
                r.add(t.right);
            }
        }

        return Triple.of(l, m, r);
    }

    /**
     *
     * @param c
     * @param unzip the second parameter is an output parameter.
     * @param supplier
     * @return
     */
    public static <T, L, M, R, LC extends Collection<L>, MC extends Collection<M>, RC extends Collection<R>, E extends Exception> Triple<LC, MC, RC> unzipp(
            final Collection<? extends T> c, final Try.BiConsumer<? super T, Triple<L, M, R>, E> unzip, final IntFunction<? extends Collection<?>> supplier)
            throws E {
        final int len = size(c);

        final LC l = (LC) supplier.apply(len);
        final MC m = (MC) supplier.apply(len);
        final RC r = (RC) supplier.apply(len);
        final Triple<L, M, R> t = new Triple<>();

        if (notNullOrEmpty(c)) {
            for (T e : c) {
                unzip.accept(e, t);

                l.add(t.left);
                m.add(t.middle);
                r.add(t.right);
            }
        }

        return Triple.of(l, m, r);
    }

    static <T> T createMask(final Class<T> interfaceClass) {
        InvocationHandler h = new InvocationHandler() {
            @Override
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                throw new UnsupportedOperationException("It's a mask");
            }
        };

        return newProxyInstance(interfaceClass, h);
    }

    static class NullMask implements Serializable {
        private static final long serialVersionUID = 5887875956120266479L;

        private NullMask() {
        }

        @Override
        public String toString() {
            return "NULL";
        }

        private Object readResolve() {
            return NULL_MASK;
        }
    }
}
