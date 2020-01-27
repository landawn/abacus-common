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
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import java.security.SecureRandom;
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
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

import com.landawn.abacus.DataSet;
import com.landawn.abacus.DirtyMarker;
import com.landawn.abacus.annotation.NullSafe;
import com.landawn.abacus.core.DirtyMarkerUtil;
import com.landawn.abacus.core.MapEntity;
import com.landawn.abacus.core.RowDataSet;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.EntityInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.Iterables.Slice;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;

// TODO: Auto-generated Javadoc
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
class CommonUtil {
    // ... it has to be big enough to make it's safety to add element to
    /** The Constant POOL_SIZE. */
    // ArrayBlockingQueue.
    static final int POOL_SIZE = Internals.POOL_SIZE;

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

    /** The Constant NULL_STRING. */
    static final String NULL_STRING = "null".intern();

    /** The Constant NULL_CHAR_ARRAY. */
    static final char[] NULL_CHAR_ARRAY = NULL_STRING.toCharArray();

    /** The Constant TRUE. */
    static final String TRUE = Boolean.TRUE.toString().intern();

    /** The Constant TRUE_CHAR_ARRAY. */
    static final char[] TRUE_CHAR_ARRAY = TRUE.toCharArray();

    /** The Constant FALSE. */
    static final String FALSE = Boolean.FALSE.toString().intern();

    /** The Constant FALSE_CHAR_ARRAY. */
    static final char[] FALSE_CHAR_ARRAY = FALSE.toCharArray();

    /** The Constant CHAR_0. */
    // ...
    static final char CHAR_0 = WD.CHAR_0;

    /**
     * The Constant CHAR_LF.
     *
     * @see <a
     *      href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6">JLF:
     *      Escape Sequences for Character and String Literals</a>
     * @since 2.2
     */
    static final char CHAR_LF = WD.CHAR_LF;

    /**
     * The Constant CHAR_CR.
     *
     * @see <a
     *      href="http://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.10.6">JLF:
     *      Escape Sequences for Character and String Literals</a>
     * @since 2.2
     */
    static final char CHAR_CR = WD.CHAR_CR;

    /** The element separator. */
    public static final String ELEMENT_SEPARATOR = ", ".intern();

    /** The element separator char array. */
    public static final char[] ELEMENT_SEPARATOR_CHAR_ARRAY = ELEMENT_SEPARATOR.toCharArray();

    // ...
    /**
     * The index value when an element is not found in a list or array:
     * {@code -1}. This value is returned by methods in this class and can also
     * be used in comparisons with values returned by various method from
     * {@link java.util.List} .
     */
    public static final int INDEX_NOT_FOUND = -1;

    /** The Constant EMPTY_STRING. */
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

    /** The Constant EMPTY_LIST. */
    @SuppressWarnings("rawtypes")
    static final List EMPTY_LIST = Collections.emptyList();

    /** The Constant EMPTY_SET. */
    @SuppressWarnings("rawtypes")
    static final Set EMPTY_SET = Collections.emptySet();

    /** The Constant EMPTY_SORTED_SET. */
    @SuppressWarnings("rawtypes")
    static final SortedSet EMPTY_SORTED_SET = Collections.emptySortedSet();

    /** The Constant EMPTY_NAVIGABLE_SET. */
    @SuppressWarnings("rawtypes")
    static final NavigableSet EMPTY_NAVIGABLE_SET = Collections.emptyNavigableSet();

    /** The Constant EMPTY_MAP. */
    @SuppressWarnings("rawtypes")
    static final Map EMPTY_MAP = Collections.emptyMap();

    /** The Constant EMPTY_SORTED_MAP. */
    @SuppressWarnings("rawtypes")
    static final SortedMap EMPTY_SORTED_MAP = Collections.emptySortedMap();

    /** The Constant EMPTY_NAVIGABLE_MAP. */
    @SuppressWarnings("rawtypes")
    static final NavigableMap EMPTY_NAVIGABLE_MAP = Collections.emptyNavigableMap();

    /** The Constant EMPTY_ITERATOR. */
    @SuppressWarnings("rawtypes")
    static final Iterator EMPTY_ITERATOR = Collections.emptyIterator();

    /** The Constant EMPTY_LIST_ITERATOR. */
    @SuppressWarnings("rawtypes")
    static final ListIterator EMPTY_LIST_ITERATOR = Collections.emptyListIterator();

    /** The Constant NULL_MASK. */
    // ...
    static final Object NULL_MASK = new NullMask();

    /** The Constant BACKSLASH_ASTERISK. */
    static final String BACKSLASH_ASTERISK = "*";

    /** The Constant REVERSE_THRESHOLD. */
    // ...
    static final int REVERSE_THRESHOLD = 18;

    /** The Constant FILL_THRESHOLD. */
    static final int FILL_THRESHOLD = 25;

    /** The Constant REPLACEALL_THRESHOLD. */
    static final int REPLACEALL_THRESHOLD = 11;

    /** The Constant RAND. */
    // ...
    static final Random RAND = new SecureRandom();

    /** The Constant NULL_MIN_COMPARATOR. */
    @SuppressWarnings("rawtypes")
    static final Comparator NULL_MIN_COMPARATOR = Comparators.nullsFirst();

    /** The Constant NULL_MAX_COMPARATOR. */
    @SuppressWarnings("rawtypes")
    static final Comparator NULL_MAX_COMPARATOR = Comparators.nullsLast();

    /** The Constant NATURAL_ORDER. */
    @SuppressWarnings("rawtypes")
    static final Comparator NATURAL_ORDER = Comparators.naturalOrder();

    /** The Constant CLASS_EMPTY_ARRAY. */
    // ...
    static final Map<Class<?>, Object> CLASS_EMPTY_ARRAY = new ConcurrentHashMap<>();

    static {
        CLASS_EMPTY_ARRAY.put(boolean.class, CommonUtil.EMPTY_BOOLEAN_ARRAY);
        CLASS_EMPTY_ARRAY.put(Boolean.class, CommonUtil.EMPTY_BOOLEAN_OBJECT_ARRAY);

        CLASS_EMPTY_ARRAY.put(char.class, CommonUtil.EMPTY_CHAR_ARRAY);
        CLASS_EMPTY_ARRAY.put(Character.class, CommonUtil.EMPTY_CHARACTER_OBJECT_ARRAY);

        CLASS_EMPTY_ARRAY.put(byte.class, CommonUtil.EMPTY_BYTE_ARRAY);
        CLASS_EMPTY_ARRAY.put(Byte.class, CommonUtil.EMPTY_BYTE_OBJECT_ARRAY);

        CLASS_EMPTY_ARRAY.put(short.class, CommonUtil.EMPTY_SHORT_ARRAY);
        CLASS_EMPTY_ARRAY.put(Short.class, CommonUtil.EMPTY_SHORT_OBJECT_ARRAY);

        CLASS_EMPTY_ARRAY.put(int.class, CommonUtil.EMPTY_INT_ARRAY);
        CLASS_EMPTY_ARRAY.put(Integer.class, CommonUtil.EMPTY_INTEGER_OBJECT_ARRAY);

        CLASS_EMPTY_ARRAY.put(long.class, CommonUtil.EMPTY_LONG_ARRAY);
        CLASS_EMPTY_ARRAY.put(Long.class, CommonUtil.EMPTY_LONG_OBJECT_ARRAY);

        CLASS_EMPTY_ARRAY.put(float.class, CommonUtil.EMPTY_FLOAT_ARRAY);
        CLASS_EMPTY_ARRAY.put(Float.class, CommonUtil.EMPTY_FLOAT_OBJECT_ARRAY);

        CLASS_EMPTY_ARRAY.put(double.class, CommonUtil.EMPTY_DOUBLE_ARRAY);
        CLASS_EMPTY_ARRAY.put(Double.class, CommonUtil.EMPTY_DOUBLE_OBJECT_ARRAY);

        CLASS_EMPTY_ARRAY.put(String.class, CommonUtil.EMPTY_STRING_ARRAY);
        CLASS_EMPTY_ARRAY.put(Object.class, CommonUtil.EMPTY_OBJECT_ARRAY);
    }

    /** The Constant CLASS_TYPE_ENUM. */
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

    /** The Constant enumListPool. */
    // ...
    private static final Map<Class<? extends Enum<?>>, ImmutableList<? extends Enum<?>>> enumListPool = new ObjectPool<>(POOL_SIZE);

    /** The Constant enumSetPool. */
    private static final Map<Class<? extends Enum<?>>, ImmutableSet<? extends Enum<?>>> enumSetPool = new ObjectPool<>(POOL_SIZE);

    /** The Constant enumMapPool. */
    private static final Map<Class<? extends Enum<?>>, ImmutableBiMap<? extends Enum<?>, String>> enumMapPool = new ObjectPool<>(POOL_SIZE);

    /** The Constant nameTypePool. */
    private static final Map<String, Type<?>> nameTypePool = new ObjectPool<>(POOL_SIZE);

    /** The Constant clsTypePool. */
    private static final Map<Class<?>, Type<?>> clsTypePool = new ObjectPool<>(POOL_SIZE);

    /** The Constant listElementDataField. */
    // ...
    static final Field listElementDataField;

    /** The Constant listSizeField. */
    static final Field listSizeField;

    /** The is list element data field gettable. */
    static volatile boolean isListElementDataFieldGettable = true;

    /** The is list element data field settable. */
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
            ClassUtil.setAccessibleQuietly(listElementDataField, true);
        }

        tmp = null;

        try {
            tmp = ArrayList.class.getDeclaredField("size");
        } catch (Throwable e) {
            // ignore.
        }

        listSizeField = tmp != null && tmp.getType().equals(int.class) ? tmp : null;

        if (listSizeField != null) {
            ClassUtil.setAccessibleQuietly(listSizeField, true);
        }
    }

    /** The Constant charStringCache. */
    static final String[] charStringCache = new String[128];

    /** The Constant intStringCacheLow. */
    static final int intStringCacheLow = -1001;

    /** The Constant intStringCacheHigh. */
    static final int intStringCacheHigh = 10001;

    /** The Constant intStringCache. */
    static final String[] intStringCache = new String[intStringCacheHigh - intStringCacheLow];

    /** The Constant stringIntCache. */
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

    /** The Constant MIN_SIZE_FOR_COPY_ALL. */
    private static final int MIN_SIZE_FOR_COPY_ALL = 9;

    /**
     * Constructor for.
     */
    CommonUtil() {
        // no instance();
    }

    /**
     *
     * @param <T>
     * @param typeName
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> Type<T> typeOf(final String typeName) {
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
     */
    @SuppressWarnings("unchecked")
    public static <T> Type<T> typeOf(final Class<?> cls) {
        N.checkArgNotNull(cls, "cls");

        Type<?> type = clsTypePool.get(cls);

        if (type == null) {
            type = TypeFactory.getType(cls);
            clsTypePool.put(cls, type);
        }

        return (Type<T>) type;
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param str
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T valueOf(final Class<? extends T> targetClass, final String str) {
        return (str == null) ? defaultValueOf(targetClass) : (T) CommonUtil.typeOf(targetClass).valueOf(str);
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
        return (T) CommonUtil.typeOf(cls).defaultValue();
    }

    /**
     * Default if null.
     *
     * @param b
     * @return true, if successful
     */
    public static boolean defaultIfNull(Boolean b) {
        if (b == null) {
            return false;
        }

        return b.booleanValue();
    }

    /**
     * Default if null.
     *
     * @param b
     * @param defaultForNull
     * @return true, if successful
     */
    public static boolean defaultIfNull(Boolean b, boolean defaultForNull) {
        if (b == null) {
            return defaultForNull;
        }

        return b.booleanValue();
    }

    /**
     * Default if null.
     *
     * @param c
     * @return
     */
    public static char defaultIfNull(Character c) {
        if (c == null) {
            return CommonUtil.CHAR_0;
        }

        return c.charValue();
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

        return c.charValue();
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

        return b.byteValue();
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

        return b.byteValue();
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

        return b.shortValue();
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

        return b.shortValue();
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

        return b.intValue();
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

        return b.intValue();
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

        return b.longValue();
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

        return b.longValue();
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

        return b.floatValue();
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

        return b.floatValue();
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

        return b.doubleValue();
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

        return b.doubleValue();
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
     *
     * @param val
     * @return
     */
    public static String stringOf(final boolean val) {
        return String.valueOf(val);
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
        return (obj == null) ? null : CommonUtil.typeOf(obj.getClass()).stringOf(obj);
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
            enumList = ImmutableList.of(CommonUtil.asList(enumClass.getEnumConstants()));

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
            enumSet = ImmutableSet.of(EnumSet.allOf(enumClass));

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

            enumMap = ImmutableBiMap.of(new BiMap<>(keyMap, valueMap));

            enumMapPool.put(enumClass, enumMap);
        }

        return enumMap;
    }

    /**
     *
     * @param <T>
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
            } else if (cls.equals(Queue.class) || cls.equals(Deque.class)) {
                return (T) new ArrayDeque<>();
            } else if (cls.equals(SortedMap.class) || cls.equals(NavigableMap.class)) {
                return (T) new TreeMap<>();
            } else if (cls.equals(SortedSet.class) || cls.equals(NavigableSet.class)) {
                return (T) new TreeSet<>();
            } else if (cls.equals(BlockingQueue.class)) {
                return (T) new LinkedBlockingQueue<>();
            } else if (cls.equals(BlockingDeque.class)) {
                return (T) new LinkedBlockingDeque<>();
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

                CommonUtil.reverse(toInstantiate);

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
    @SuppressWarnings("unchecked")
    private static <T> T invoke(final Constructor<T> c, final Object... args)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (c.isAccessible() == false) {
            ClassUtil.setAccessible(c, true);
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
        return newProxyInstance(CommonUtil.asArray(interfaceClass), h);
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
        return (T) Proxy.newProxyInstance(CommonUtil.class.getClassLoader(), interfaceClasses, h);
    }

    /**
     *
     * @param <T>
     * @param componentType
     * @param length
     * @return T[]
     */
    @SuppressWarnings("unchecked")
    public static <T> T newArray(final Class<?> componentType, final int length) {
        return (T) Array.newInstance(componentType, length);
    }

    /**
     *
     * @param <T>
     * @param cls
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T newEntity(final Class<T> cls) {
        return newEntity(cls, null);
    }

    /**
     *
     * @param <T>
     * @param cls
     * @param entityName
     * @return
     */
    public static <T> T newEntity(final Class<T> cls, final String entityName) {
        if (MapEntity.class.isAssignableFrom(cls)) {
            return (T) new MapEntity(entityName);
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
     */
    public static int initHashCapacity(final int size) {
        CommonUtil.checkArgNotNegative(size, "size");

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

    /**
     * New array list.
     *
     * @param <T>
     * @return
     */
    public static <T> ArrayList<T> newArrayList() {
        return new ArrayList<>();
    }

    /**
     * New array list.
     *
     * @param <T>
     * @param initialCapacity
     * @return
     */
    public static <T> ArrayList<T> newArrayList(int initialCapacity) {
        return new ArrayList<>(initialCapacity);
    }

    /**
     * New array list.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> ArrayList<T> newArrayList(Collection<? extends T> c) {
        return CommonUtil.isNullOrEmpty(c) ? new ArrayList<>() : new ArrayList<>(c);
    }

    /**
     * New linked list.
     *
     * @param <T>
     * @return
     */
    public static <T> LinkedList<T> newLinkedList() {
        return new LinkedList<>();
    }

    /**
     * New linked list.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> LinkedList<T> newLinkedList(Collection<? extends T> c) {
        return CommonUtil.isNullOrEmpty(c) ? new LinkedList<>() : new LinkedList<>(c);
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
        return new HashSet<>(initialCapacity);
    }

    /**
     * New hash set.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> Set<T> newHashSet(Collection<? extends T> c) {
        return CommonUtil.isNullOrEmpty(c) ? new HashSet<>() : new HashSet<>(c);
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
        return new LinkedHashSet<>(initialCapacity);
    }

    /**
     * New linked hash set.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> Set<T> newLinkedHashSet(Collection<? extends T> c) {
        return CommonUtil.isNullOrEmpty(c) ? new LinkedHashSet<>() : new LinkedHashSet<>(c);
    }

    /**
     * New tree set.
     *
     * @param <T>
     * @return
     */
    public static <T extends Comparable<? super T>> TreeSet<T> newTreeSet() {
        return new TreeSet<>();
    }

    /**
     * New tree set.
     *
     * @param <T>
     * @param comparator
     * @return
     */
    public static <T> TreeSet<T> newTreeSet(Comparator<? super T> comparator) {
        return new TreeSet<>(comparator);
    }

    /**
     * New tree set.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Comparable<? super T>> TreeSet<T> newTreeSet(Collection<? extends T> c) {
        return CommonUtil.isNullOrEmpty(c) ? new TreeSet<>() : new TreeSet<>(c);
    }

    /**
     * New tree set.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> TreeSet<T> newTreeSet(SortedSet<T> c) {
        return CommonUtil.isNullOrEmpty(c) ? new TreeSet<>() : new TreeSet<>(c);
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
    public static <T> ArrayDeque<T> newArrayDeque() {
        return new ArrayDeque<>();
    }

    /**
     * Constructs an empty array deque with an initial capacity sufficient to hold the specified number of elements.
     *
     * @param <T>
     * @param numElements lower bound on initial capacity of the deque.
     * @return
     */
    public static <T> ArrayDeque<T> newArrayDeque(int numElements) {
        return new ArrayDeque<>(numElements);
    }

    /**
     * Constructs a deque containing the elements of the specified collection, in the order they are returned by the collection's iterator.
     *
     * @param <E>
     * @param c
     * @return
     */
    public static <E> ArrayDeque<E> newArrayDeque(Collection<? extends E> c) {
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
        return new HashMap<>(initialCapacity);
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
        return CommonUtil.isNullOrEmpty(m) ? new HashMap<>() : new HashMap<>(m);
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
     * @throws E the e
     */
    public static <K, V, E extends Exception> Map<K, V> newHashMap(final Collection<? extends V> c,
            final Throwables.Function<? super V, ? extends K, E> keyMapper) throws E {
        CommonUtil.checkArgNotNull(keyMapper);

        if (isNullOrEmpty(c)) {
            return new HashMap<>();
        }

        final HashMap<K, V> result = new HashMap<>(CommonUtil.initHashCapacity(c.size()));

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
        return new LinkedHashMap<>(initialCapacity);
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
        return CommonUtil.isNullOrEmpty(m) ? new LinkedHashMap<>() : new LinkedHashMap<>(m);
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
     * @throws E the e
     */
    public static <K, V, E extends Exception> Map<K, V> newLinkedHashMap(final Collection<? extends V> c,
            final Throwables.Function<? super V, ? extends K, E> keyMapper) throws E {
        CommonUtil.checkArgNotNull(keyMapper);

        if (isNullOrEmpty(c)) {
            return new LinkedHashMap<>();
        }

        final Map<K, V> result = new LinkedHashMap<>(CommonUtil.initHashCapacity(c.size()));

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
    public static <K extends Comparable<? super K>, V> TreeMap<K, V> newTreeMap() {
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
    public static <C, K extends C, V> TreeMap<K, V> newTreeMap(Comparator<C> comparator) {
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
    public static <K extends Comparable<? super K>, V> TreeMap<K, V> newTreeMap(Map<? extends K, ? extends V> m) {
        return CommonUtil.isNullOrEmpty(m) ? new TreeMap<>() : new TreeMap<>(m);
    }

    /**
     * New tree map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param m
     * @return
     */
    public static <K, V> TreeMap<K, V> newTreeMap(SortedMap<K, ? extends V> m) {
        return CommonUtil.isNullOrEmpty(m) ? new TreeMap<>() : new TreeMap<>(m);
    }

    /**
     * New identity hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    public static <K, V> IdentityHashMap<K, V> newIdentityHashMap() {
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
    public static <K, V> IdentityHashMap<K, V> newIdentityHashMap(int initialCapacity) {
        return new IdentityHashMap<>(initialCapacity);
    }

    /**
     * New identity hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param m
     * @return
     */
    public static <K, V> IdentityHashMap<K, V> newIdentityHashMap(Map<? extends K, ? extends V> m) {
        return CommonUtil.isNullOrEmpty(m) ? new IdentityHashMap<>() : new IdentityHashMap<>(m);
    }

    /**
     * New concurrent hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap() {
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
    public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap(int initialCapacity) {
        return new ConcurrentHashMap<>(initialCapacity);
    }

    /**
     * New concurrent hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param m
     * @return
     */
    public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap(Map<? extends K, ? extends V> m) {
        return CommonUtil.isNullOrEmpty(m) ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(m);
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
     * @param m
     * @return
     */
    public static <K, E> ListMultimap<K, E> newListMultimap(final Map<? extends K, ? extends E> m) {
        final ListMultimap<K, E> multiMap = newListMultimap();

        multiMap.putAll(m);

        return multiMap;
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
        return new ListMultimap<>(new LinkedHashMap<K, List<E>>(initialCapacity), ArrayList.class);
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
        final ListMultimap<K, E> multiMap = new ListMultimap<>(new LinkedHashMap<K, List<E>>(), ArrayList.class);

        multiMap.putAll(m);

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
        return new ListMultimap<>(new TreeMap<K, List<E>>(), ArrayList.class);
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
        final ListMultimap<K, E> multiMap = new ListMultimap<>(new TreeMap<K, List<E>>(), ArrayList.class);

        multiMap.putAll(m);

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
     * @param m
     * @return
     */
    public static <K, E> SetMultimap<K, E> newSetMultimap(final Map<? extends K, ? extends E> m) {
        final SetMultimap<K, E> multiMap = newSetMultimap();

        multiMap.putAll(m);

        return multiMap;
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
        return new SetMultimap<>(new LinkedHashMap<K, Set<E>>(initialCapacity), HashSet.class);
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
        final SetMultimap<K, E> multiMap = new SetMultimap<>(new LinkedHashMap<K, Set<E>>(), HashSet.class);

        multiMap.putAll(m);

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
        return new SetMultimap<>(new TreeMap<K, Set<E>>(), HashSet.class);
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
        final SetMultimap<K, E> multiMap = new SetMultimap<>(new TreeMap<K, Set<E>>(), HashSet.class);

        multiMap.putAll(m);

        return multiMap;
    }

    /** The Constant MAX_ARRAY_SIZE. */
    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /** The Constant MAX_HASH_LENGTH. */
    static final int MAX_HASH_LENGTH = (int) (MAX_ARRAY_SIZE / 1.25) - 1;

    /**
     * New empty data set.
     *
     * @return
     */
    public static DataSet newEmptyDataSet() {
        return new RowDataSet(new ArrayList<String>(), new ArrayList<List<Object>>());
    }

    /**
     * New empty data set.
     *
     * @param columnNames
     * @return
     */
    public static DataSet newEmptyDataSet(final Collection<String> columnNames) {
        if (CommonUtil.isNullOrEmpty(columnNames)) {
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

        final List<String> columnNameList = CommonUtil.asList(keyColumnName, valueColumnName);
        final List<List<Object>> columnList = CommonUtil.asList(keyColumn, valueColumn);

        return newDataSet(columnNameList, columnList);
    }

    /**
     * The first row will be used as column names if its type is array or list,
     * or obtain the column names from first row if its type is entity or map.
     *
     * @param <T>
     * @param rows list of row which can be: Map/Entity/Array/List
     * @return
     */
    public static <T> DataSet newDataSet(final Collection<T> rows) {
        return newDataSet(null, rows);
    }

    /**
     * If the specified {@code columnNames} is null or empty, the first row will be used as column names if its type is array or list,
     * or obtain the column names from first row if its type is entity or map.
     *
     * @param <T>
     * @param columnNames
     * @param rows list of row which can be: Map/Entity/Array/List
     * @return
     */
    public static <T> DataSet newDataSet(Collection<String> columnNames, Collection<T> rowList) {
        if (CommonUtil.isNullOrEmpty(columnNames) && CommonUtil.isNullOrEmpty(rowList)) {
            // throw new IllegalArgumentException("Column name list and row list can not be both null or empty");
            return CommonUtil.newEmptyDataSet();
        } else if (CommonUtil.isNullOrEmpty(rowList)) {
            return CommonUtil.newEmptyDataSet(columnNames);
        }

        int startRowIndex = 0;

        if (CommonUtil.isNullOrEmpty(columnNames)) {
            final T firstNonNullRow = N.firstNonNull(rowList).orElse(null);

            if (firstNonNullRow == null) {
                return CommonUtil.newEmptyDataSet();
            }

            final Class<?> cls = firstNonNullRow.getClass();
            final Type<?> type = CommonUtil.typeOf(cls);

            if (type.isMap()) {
                columnNames = new ArrayList<>(((Map<String, Object>) firstNonNullRow).keySet());
            } else if (type.isEntity()) {
                if (N.isDirtyMarker(cls)) {
                    final Set<String> signedPropNames = DirtyMarkerUtil.signedPropNames((DirtyMarker) firstNonNullRow);
                    columnNames = new ArrayList<>(signedPropNames.size());
                    Method method = null;

                    for (String signedPropName : signedPropNames) {
                        method = ClassUtil.getPropGetMethod(cls, signedPropName);

                        if (method != null) {
                            columnNames.add(ClassUtil.getPropNameByMethod(method));
                        }
                    }
                } else {
                    columnNames = new ArrayList<>(ClassUtil.getPropNameList(cls));
                }
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

            if (CommonUtil.isNullOrEmpty(columnNames)) {
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

            type = CommonUtil.typeOf(row.getClass());

            if (type.isMap()) {
                Map<String, Object> props = (Map<String, Object>) row;

                for (int i = 0; i < columnCount; i++) {
                    columnList.get(i).add(props.get(columnNameList.get(i)));
                }
            } else if (type.isEntity()) {
                final Class<?> cls = row.getClass();
                final EntityInfo entityInfo = ParserUtil.getEntityInfo(cls);
                PropInfo propInfo = null;

                for (int i = 0; i < columnCount; i++) {
                    propInfo = entityInfo.getPropInfo(columnNameList.get(i));

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
                        "Unsupported row type: " + ClassUtil.getCanonicalClassName(row.getClass()) + ". Only array, collection, map and entity are supported");
            }
        }

        return new RowDataSet(columnNameList, columnList);
    }

    /**
     * New data set.
     *
     * @param <C>
     * @param map keys are column names, values are columns
     * @return
     */
    public static <C extends Collection<?>> DataSet newDataSet(final Map<String, C> map) {
        if (CommonUtil.isNullOrEmpty(map)) {
            return CommonUtil.newEmptyDataSet();
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

            if (CommonUtil.notNullOrEmpty(v)) {
                column.addAll(v);
            }

            if (column.size() < maxColumnLen) {
                CommonUtil.fill(column, column.size(), maxColumnLen, null);
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
        if (CommonUtil.isNullOrEmpty(c)) {
            return CommonUtil.EMPTY_OBJECT_ARRAY;
        }

        return c.toArray(new Object[c.size()]);
    }

    /**
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static Object[] toArray(final Collection<?> c, final int fromIndex, final int toIndex) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, size(c));

        if (CommonUtil.isNullOrEmpty(c)) {
            return CommonUtil.EMPTY_OBJECT_ARRAY;
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
     * @param <A>
     * @param <T>
     * @param c
     * @param a
     * @return
     */
    public static <A, T extends A> A[] toArray(final Collection<T> c, final A[] a) {
        CommonUtil.checkArgNotNull(a);

        if (CommonUtil.isNullOrEmpty(c)) {
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
     * @return
     */
    public static <A, T extends A> A[] toArray(final Collection<T> c, final int fromIndex, final int toIndex, final A[] a) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, size(c));
        CommonUtil.checkArgNotNull(a);

        if (CommonUtil.isNullOrEmpty(c)) {
            return a;
        } else if (fromIndex == 0 || toIndex == c.size()) {
            return c.toArray(a);
        } else if (c instanceof List) {
            return ((List<T>) c).subList(fromIndex, toIndex).toArray(a);
        } else {
            final A[] res = a.length >= toIndex - fromIndex ? a : (A[]) CommonUtil.newArray(a.getClass().getComponentType(), toIndex - fromIndex);
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

    /**
     *
     * @param <A>
     * @param <T>
     * @param c
     * @param arraySupplier
     * @return
     */
    public static <A, T extends A> A[] toArray(final Collection<T> c, final IntFunction<A[]> arraySupplier) {
        CommonUtil.checkArgNotNull(arraySupplier);

        if (CommonUtil.isNullOrEmpty(c)) {
            return arraySupplier.apply(0);
        }

        return toArray(c, arraySupplier);
    }

    /**
     *
     * @param <A>
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param arraySupplier
     * @return
     */
    public static <A, T extends A> A[] toArray(final Collection<T> c, final int fromIndex, final int toIndex, final IntFunction<A[]> arraySupplier) {
        CommonUtil.checkArgNotNull(arraySupplier);
        CommonUtil.checkFromToIndex(fromIndex, toIndex, size(c));

        if (CommonUtil.isNullOrEmpty(c)) {
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

    /**
     *
     * @param <A>
     * @param <T>
     * @param targetClass
     * @param c
     * @return
     */
    public static <A, T extends A> A[] toArray(final Class<A[]> targetClass, final Collection<T> c) {
        CommonUtil.checkArgNotNull(targetClass);

        if (CommonUtil.isNullOrEmpty(c)) {
            return CommonUtil.newArray(targetClass.getComponentType(), 0);
        }

        return c.toArray((A[]) CommonUtil.newArray(targetClass.getComponentType(), c.size()));
    }

    /**
     *
     * @param <A>
     * @param <T>
     * @param targetClass
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <A, T extends A> A[] toArray(final Class<A[]> targetClass, final Collection<T> c, final int fromIndex, final int toIndex) {
        CommonUtil.checkArgNotNull(targetClass);
        CommonUtil.checkFromToIndex(fromIndex, toIndex, size(c));

        final A[] res = CommonUtil.newArray(targetClass.getComponentType(), toIndex - fromIndex);

        if (CommonUtil.isNullOrEmpty(c)) {
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
     */
    public static boolean[] toBooleanArray(final Collection<Boolean> c, final int fromIndex, final int toIndex, final boolean defaultForNull) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, size(c));

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
     */
    public static char[] toCharArray(final Collection<Character> c, final int fromIndex, final int toIndex, final char defaultForNull) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, size(c));

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
     */
    public static byte[] toByteArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex, final byte defaultForNull) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, size(c));

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
     */
    public static short[] toShortArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex, final short defaultForNull) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, size(c));

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
     */
    public static int[] toIntArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex, final int defaultForNull) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, size(c));

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
     */
    public static long[] toLongArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex, final long defaultForNull) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, size(c));

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
     */
    public static float[] toFloatArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex, final float defaultForNull) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, size(c));

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
     */
    public static double[] toDoubleArray(final Collection<? extends Number> c, final int fromIndex, final int toIndex, final double defaultForNull) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, size(c));

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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static List<Boolean> toList(final boolean[] a, final int fromIndex, final int toIndex) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static List<Character> toList(final char[] a, final int fromIndex, final int toIndex) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static List<Byte> toList(final byte[] a, final int fromIndex, final int toIndex) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static List<Short> toList(final short[] a, final int fromIndex, final int toIndex) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static List<Integer> toList(final int[] a, final int fromIndex, final int toIndex) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static List<Long> toList(final long[] a, final int fromIndex, final int toIndex) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static List<Float> toList(final float[] a, final int fromIndex, final int toIndex) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static List<Double> toList(final double[] a, final int fromIndex, final int toIndex) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

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
        if (CommonUtil.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        return CommonUtil.asList(a);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T> List<T> toList(final T[] a, final int fromIndex, final int toIndex) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return new ArrayList<>();
        } else if (fromIndex == 0 && toIndex == a.length) {
            return CommonUtil.asList(a);
        }

        final List<T> result = new ArrayList<>(toIndex - fromIndex);

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
    public static Set<Boolean> toSet(final boolean[] a) {
        return toSet(a, 0, len(a));
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static Set<Boolean> toSet(final boolean[] a, final int fromIndex, final int toIndex) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return CommonUtil.newHashSet();
        }

        final Set<Boolean> result = CommonUtil.newHashSet(toIndex - fromIndex);

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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static Set<Character> toSet(final char[] a, final int fromIndex, final int toIndex) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return CommonUtil.newHashSet();
        }

        final Set<Character> result = CommonUtil.newHashSet(toIndex - fromIndex);

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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static Set<Byte> toSet(final byte[] a, final int fromIndex, final int toIndex) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return CommonUtil.newHashSet();
        }

        final Set<Byte> result = CommonUtil.newHashSet(toIndex - fromIndex);

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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static Set<Short> toSet(final short[] a, final int fromIndex, final int toIndex) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return CommonUtil.newHashSet();
        }

        final Set<Short> result = CommonUtil.newHashSet(toIndex - fromIndex);

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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static Set<Integer> toSet(final int[] a, final int fromIndex, final int toIndex) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return CommonUtil.newHashSet();
        }

        final Set<Integer> result = CommonUtil.newHashSet(toIndex - fromIndex);

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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static Set<Long> toSet(final long[] a, final int fromIndex, final int toIndex) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return CommonUtil.newHashSet();
        }

        final Set<Long> result = CommonUtil.newHashSet(toIndex - fromIndex);

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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static Set<Float> toSet(final float[] a, final int fromIndex, final int toIndex) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return CommonUtil.newHashSet();
        }

        final Set<Float> result = CommonUtil.newHashSet(toIndex - fromIndex);

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
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static Set<Double> toSet(final double[] a, final int fromIndex, final int toIndex) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return CommonUtil.newHashSet();
        }

        final Set<Double> result = CommonUtil.newHashSet(toIndex - fromIndex);

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
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.newHashSet();
        }

        return CommonUtil.asSet(a);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T> Set<T> toSet(final T[] a, final int fromIndex, final int toIndex) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return CommonUtil.newHashSet();
        }

        final Set<T> result = CommonUtil.newHashSet(CommonUtil.initHashCapacity(toIndex - fromIndex));

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
    public static <C extends Collection<Boolean>> C toCollection(final boolean[] a, final IntFunction<? extends C> supplier) {
        return toCollection(a, 0, len(a), supplier);
    }

    /**
     *
     * @param <C>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     */
    public static <C extends Collection<Boolean>> C toCollection(final boolean[] a, final int fromIndex, final int toIndex,
            final IntFunction<? extends C> supplier) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

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
     * @param <C>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     */
    public static <C extends Collection<Character>> C toCollection(final char[] a, final int fromIndex, final int toIndex,
            final IntFunction<? extends C> supplier) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

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
     * @param <C>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     */
    public static <C extends Collection<Byte>> C toCollection(final byte[] a, final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

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
     * @param <C>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     */
    public static <C extends Collection<Short>> C toCollection(final short[] a, final int fromIndex, final int toIndex,
            final IntFunction<? extends C> supplier) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

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
     * @param <C>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     */
    public static <C extends Collection<Integer>> C toCollection(final int[] a, final int fromIndex, final int toIndex,
            final IntFunction<? extends C> supplier) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

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
     * @param <C>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     */
    public static <C extends Collection<Long>> C toCollection(final long[] a, final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

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
     * @param <C>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     */
    public static <C extends Collection<Float>> C toCollection(final float[] a, final int fromIndex, final int toIndex,
            final IntFunction<? extends C> supplier) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

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
     * @param <C>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     */
    public static <C extends Collection<Double>> C toCollection(final double[] a, final int fromIndex, final int toIndex,
            final IntFunction<? extends C> supplier) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

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
        if (CommonUtil.isNullOrEmpty(a)) {
            return supplier.apply(0);
        }

        return toCollection(a, 0, a.length, supplier);
    }

    /**
     *
     * @param <T>
     * @param <C>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param supplier
     * @return
     */
    public static <T, C extends Collection<T>> C toCollection(final T[] a, final int fromIndex, final int toIndex, final IntFunction<? extends C> supplier) {
        CommonUtil.checkFromToIndex(fromIndex, toIndex, len(a));

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

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k>
     * @param <v>
     * @param k1
     * @param v1
     * @return
     */
    public static <K, V, k extends K, v extends V> Map<K, V> asMap(final k k1, final v v1) {
        final Map<K, V> map = new HashMap<>(1);
        map.put(k1, v1);
        return map;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k>
     * @param <v>
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @return
     */
    public static <K, V, k extends K, v extends V> Map<K, V> asMap(final k k1, final v v1, final k k2, final v v2) {
        final Map<K, V> map = new HashMap<>(2);
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k>
     * @param <v>
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @return
     */
    public static <K, V, k extends K, v extends V> Map<K, V> asMap(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3) {
        final Map<K, V> map = new HashMap<>(3);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k>
     * @param <v>
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
    public static <K, V, k extends K, v extends V> Map<K, V> asMap(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3, final k k4,
            final v v4) {
        final Map<K, V> map = new HashMap<>(4);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        return map;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k>
     * @param <v>
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
    public static <K, V, k extends K, v extends V> Map<K, V> asMap(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3, final k k4,
            final v v4, final k k5, final v v5) {
        final Map<K, V> map = new HashMap<>(5);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        return map;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k>
     * @param <v>
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
    public static <K, V, k extends K, v extends V> Map<K, V> asMap(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3, final k k4,
            final v v4, final k k5, final v v5, final k k6, final v v6) {
        final Map<K, V> map = new HashMap<>(6);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        return map;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k>
     * @param <v>
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
    public static <K, V, k extends K, v extends V> Map<K, V> asMap(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3, final k k4,
            final v v4, final k k5, final v v5, final k k6, final v v6, final k k7, final v v7) {
        final Map<K, V> map = new HashMap<>(7);
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
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param a
     * @return
     */
    @SafeVarargs
    @NullSafe
    public static <K, V> Map<K, V> asMap(final Object... a) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return new HashMap<>();
        }

        return newMap(new HashMap<K, V>(CommonUtil.initHashCapacity(a.length / 2)), a);
    }

    /**
     * As linked hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k>
     * @param <v>
     * @param k1
     * @param v1
     * @return
     */
    public static <K, V, k extends K, v extends V> Map<K, V> asLinkedHashMap(final k k1, final v v1) {
        final Map<K, V> map = new LinkedHashMap<>(1);
        map.put(k1, v1);
        return map;
    }

    /**
     * As linked hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k>
     * @param <v>
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @return
     */
    public static <K, V, k extends K, v extends V> Map<K, V> asLinkedHashMap(final k k1, final v v1, final k k2, final v v2) {
        final Map<K, V> map = new LinkedHashMap<>(2);
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    /**
     * As linked hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k>
     * @param <v>
     * @param k1
     * @param v1
     * @param k2
     * @param v2
     * @param k3
     * @param v3
     * @return
     */
    public static <K, V, k extends K, v extends V> Map<K, V> asLinkedHashMap(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3) {
        final Map<K, V> map = new LinkedHashMap<>(3);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }

    /**
     * As linked hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k>
     * @param <v>
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
    public static <K, V, k extends K, v extends V> Map<K, V> asLinkedHashMap(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3, final k k4,
            final v v4) {
        final Map<K, V> map = new LinkedHashMap<>(4);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        return map;
    }

    /**
     * As linked hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k>
     * @param <v>
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
    public static <K, V, k extends K, v extends V> Map<K, V> asLinkedHashMap(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3, final k k4,
            final v v4, final k k5, final v v5) {
        final Map<K, V> map = new LinkedHashMap<>(5);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        return map;
    }

    /**
     * As linked hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k>
     * @param <v>
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
    public static <K, V, k extends K, v extends V> Map<K, V> asLinkedHashMap(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3, final k k4,
            final v v4, final k k5, final v v5, final k k6, final v v6) {
        final Map<K, V> map = new LinkedHashMap<>(6);
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        map.put(k6, v6);
        return map;
    }

    /**
     * As linked hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <k>
     * @param <v>
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
    public static <K, V, k extends K, v extends V> Map<K, V> asLinkedHashMap(final k k1, final v v1, final k k2, final v v2, final k k3, final v v3, final k k4,
            final v v4, final k k5, final v v5, final k k6, final v v6, final k k7, final v v7) {
        final Map<K, V> map = new LinkedHashMap<>(7);
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
     * As linked hash map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param a
     * @return
     */
    @SafeVarargs
    public static <K, V> Map<K, V> asLinkedHashMap(final Object... a) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return new LinkedHashMap<>();
        }

        return newMap(new LinkedHashMap<K, V>(CommonUtil.initHashCapacity(a.length / 2)), a);
    }

    /**
     *
     * @param a pairs of property name and value or a Java Entity Object what
     *            allows access to properties using getter and setter methods.
     * @return
     */
    @SafeVarargs
    public static Map<String, Object> asProps(final Object... a) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return new LinkedHashMap<>();
        }

        return newMap(new LinkedHashMap<String, Object>(CommonUtil.initHashCapacity(a.length / 2)), a);
    }

    /**
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
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    @NullSafe
    public static <T> List<T> asList(final T... a) {
        if (CommonUtil.isNullOrEmpty(a)) {
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

    /**
     * As linked list.
     *
     * @param <T>
     * @param e
     * @return
     */
    public static <T> LinkedList<T> asLinkedList(final T e) {
        final LinkedList<T> list = new LinkedList<>();
        list.add(e);
        return list;
    }

    /**
     * As linked list.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @return
     */
    public static <T> LinkedList<T> asLinkedList(final T e1, final T e2) {
        final LinkedList<T> list = new LinkedList<>();
        list.add(e1);
        list.add(e2);
        return list;
    }

    /**
     * As linked list.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @return
     */
    public static <T> LinkedList<T> asLinkedList(final T e1, final T e2, final T e3) {
        final LinkedList<T> list = new LinkedList<>();
        list.add(e1);
        list.add(e2);
        list.add(e3);
        return list;
    }

    /**
     * As linked list.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @return
     */
    public static <T> LinkedList<T> asLinkedList(final T e1, final T e2, final T e3, final T e4) {
        final LinkedList<T> list = new LinkedList<>();
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        return list;
    }

    /**
     * As linked list.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @param e5
     * @return
     */
    public static <T> LinkedList<T> asLinkedList(final T e1, final T e2, final T e3, final T e4, final T e5) {
        final LinkedList<T> list = new LinkedList<>();
        list.add(e1);
        list.add(e2);
        list.add(e3);
        list.add(e4);
        list.add(e5);
        return list;
    }

    /**
     * As linked list.
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

    /**
     * As linked list.
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

    /**
     * As linked list.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    @NullSafe
    public static <T> LinkedList<T> asLinkedList(final T... a) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return new LinkedList<>();
        }

        final LinkedList<T> list = new LinkedList<>();

        for (T e : a) {
            list.add(e);
        }

        return list;
    }

    /**
     *
     * @param <T>
     * @param e
     * @return
     */
    public static <T> Set<T> asSet(final T e) {
        final Set<T> set = CommonUtil.newHashSet(1);
        set.add(e);
        return set;
    }

    /**
     *
     * @param <T>
     * @param e1
     * @param e2
     * @return
     */
    public static <T> Set<T> asSet(final T e1, final T e2) {
        final Set<T> set = CommonUtil.newHashSet(2);
        set.add(e1);
        set.add(e2);
        return set;
    }

    /**
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @return
     */
    public static <T> Set<T> asSet(final T e1, final T e2, final T e3) {
        final Set<T> set = CommonUtil.newHashSet(3);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        return set;
    }

    /**
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @return
     */
    public static <T> Set<T> asSet(final T e1, final T e2, final T e3, final T e4) {
        final Set<T> set = CommonUtil.newHashSet(4);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        return set;
    }

    /**
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
        final Set<T> set = CommonUtil.newHashSet(5);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        set.add(e5);
        return set;
    }

    /**
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
        final Set<T> set = CommonUtil.newHashSet(6);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        set.add(e5);
        set.add(e6);
        return set;
    }

    /**
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
        final Set<T> set = CommonUtil.newHashSet(7);
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
        final Set<T> set = CommonUtil.newHashSet(8);
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
        final Set<T> set = CommonUtil.newHashSet(9);
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
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    @NullSafe
    public static <T> Set<T> asSet(final T... a) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.newHashSet();
        }

        final Set<T> set = CommonUtil.newHashSet(CommonUtil.initHashCapacity(a.length));

        for (T e : a) {
            set.add(e);
        }

        return set;
    }

    /**
     * As linked linked hash set.
     *
     * @param <T>
     * @param e
     * @return
     */
    public static <T> Set<T> asLinkedHashSet(final T e) {
        final Set<T> set = CommonUtil.newLinkedHashSet(1);
        set.add(e);
        return set;
    }

    /**
     * As linked linked hash set.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @return
     */
    public static <T> Set<T> asLinkedHashSet(final T e1, final T e2) {
        final Set<T> set = CommonUtil.newLinkedHashSet(2);
        set.add(e1);
        set.add(e2);
        return set;
    }

    /**
     * As linked linked hash set.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @return
     */
    public static <T> Set<T> asLinkedHashSet(final T e1, final T e2, final T e3) {
        final Set<T> set = CommonUtil.newLinkedHashSet(3);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        return set;
    }

    /**
     * As linked linked hash set.
     *
     * @param <T>
     * @param e1
     * @param e2
     * @param e3
     * @param e4
     * @return
     */
    public static <T> Set<T> asLinkedHashSet(final T e1, final T e2, final T e3, final T e4) {
        final Set<T> set = CommonUtil.newLinkedHashSet(4);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        return set;
    }

    /**
     * As linked linked hash set.
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
        final Set<T> set = CommonUtil.newLinkedHashSet(5);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        set.add(e5);
        return set;
    }

    /**
     * As linked linked hash set.
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
        final Set<T> set = CommonUtil.newLinkedHashSet(6);
        set.add(e1);
        set.add(e2);
        set.add(e3);
        set.add(e4);
        set.add(e5);
        set.add(e6);
        return set;
    }

    /**
     * As linked linked hash set.
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
        final Set<T> set = CommonUtil.newLinkedHashSet(7);
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
     * As linked hash set.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    @NullSafe
    public static <T> Set<T> asLinkedHashSet(final T... a) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.newLinkedHashSet();
        }

        final Set<T> set = CommonUtil.newLinkedHashSet(CommonUtil.initHashCapacity(a.length));

        for (T e : a) {
            set.add(e);
        }

        return set;
    }

    /**
     * As sorted set.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    @NullSafe
    public static <T> SortedSet<T> asSortedSet(final T... a) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return new TreeSet<>();
        }

        final SortedSet<T> set = new TreeSet<>();

        for (T e : a) {
            set.add(e);
        }

        return set;
    }

    /**
     * As navigable set.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> NavigableSet<T> asNavigableSet(final T... a) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return new TreeSet<>();
        }

        final NavigableSet<T> set = new TreeSet<>();

        for (T e : a) {
            set.add(e);
        }

        return set;
    }

    /**
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
     * As array blocking queue.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> ArrayBlockingQueue<T> asArrayBlockingQueue(final T... a) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return new ArrayBlockingQueue<>(0);
        }

        final ArrayBlockingQueue<T> queue = new ArrayBlockingQueue<>(a.length);

        for (T e : a) {
            queue.add(e);
        }

        return queue;
    }

    /**
     * As linked blocking queue.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> LinkedBlockingQueue<T> asLinkedBlockingQueue(final T... a) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return new LinkedBlockingQueue<>();
        }

        final LinkedBlockingQueue<T> queue = new LinkedBlockingQueue<>(a.length);

        for (T e : a) {
            queue.add(e);
        }

        return queue;
    }

    /**
     * As concurrent linked queue.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> ConcurrentLinkedQueue<T> asConcurrentLinkedQueue(final T... a) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return new ConcurrentLinkedQueue<>();
        }

        final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();

        for (T e : a) {
            queue.add(e);
        }

        return queue;
    }

    /**
     * As delay queue.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T extends Delayed> DelayQueue<T> asDelayQueue(final T... a) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return new DelayQueue<>();
        }

        final DelayQueue<T> queue = new DelayQueue<>();

        for (T e : a) {
            queue.add(e);
        }

        return queue;
    }

    /**
     * As priority queue.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> PriorityQueue<T> asPriorityQueue(final T... a) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return new PriorityQueue<>();
        }

        final PriorityQueue<T> queue = new PriorityQueue<>(a.length);

        for (T e : a) {
            queue.add(e);
        }

        return queue;
    }

    /**
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
     * As array deque.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> ArrayDeque<T> asArrayDeque(final T... a) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return new ArrayDeque<>();
        }

        final ArrayDeque<T> arrayDeque = new ArrayDeque<>(a.length);

        for (T e : a) {
            arrayDeque.add(e);
        }

        return arrayDeque;
    }

    /**
     * As linked blocking deque.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> LinkedBlockingDeque<T> asLinkedBlockingDeque(final T... a) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return new LinkedBlockingDeque<>();
        }

        final LinkedBlockingDeque<T> deque = new LinkedBlockingDeque<>(a.length);

        for (T e : a) {
            deque.add(e);
        }

        return deque;
    }

    /**
     * As concurrent linked deque.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> ConcurrentLinkedDeque<T> asConcurrentLinkedDeque(final T... a) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return new ConcurrentLinkedDeque<>();
        }

        final ConcurrentLinkedDeque<T> deque = new ConcurrentLinkedDeque<>();

        for (T e : a) {
            deque.add(e);
        }

        return deque;
    }

    /**
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
     * @return
     * @see java.util.Collections#singletonList(Object)
     */
    public static <T> List<T> asSingletonList(final T o) {
        return Collections.singletonList(o);
    }

    /**
     * Wrap the specified value with a singleton set.
     *
     * @param <T>
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
     * @param <K> the key type
     * @param <V> the value type
     * @param key
     * @param value
     * @return
     * @see java.util.Collections#singletonMap(Object, Object)
     */
    public static <K, V> Map<K, V> asSingletonMap(final K key, final V value) {
        return Collections.singletonMap(key, value);
    }

    /**
     * Try to convert the specified {@code obj} to the specified
     * {@code targetClass}. Default value of {@code targetClass} is returned if
     * {@code sourceObject} is null. An instance of {@code targetClass} is returned if
     * convert successfully
     *
     * @param <T>
     * @param obj
     * @param targetClass
     * @return
     */
    public static <T> T convert(final Object obj, final Class<? extends T> targetClass) {
        if (obj == null) {
            return CommonUtil.defaultValueOf(targetClass);
        }

        final Type<T> type = typeOf(targetClass);
        return convert(obj, type);
    }

    /**
     *
     * @param <T>
     * @param obj
     * @param targetType
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
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

        if (targetType.isEntity() && srcPropType.isEntity()) {
            if (srcPropType.isEntity()) {
                return copy(targetType.clazz(), obj);
            } else if (srcPropType.isMap()) {
                return Maps.map2Entity(targetType.clazz(), (Map<String, Object>) obj);
            }
        }

        if (targetType.isMap()) {
            if (srcPropType.isEntity() && targetType.getParameterTypes()[0].clazz().isAssignableFrom(String.class)
                    && Object.class.equals(targetType.getParameterTypes()[1].clazz())) {
                try {
                    return (T) Maps.entity2Map((Map<String, Object>) CommonUtil.newInstance(targetType.clazz()), obj);
                } catch (Exception e) {
                    // ignore.
                }
            } else if (srcPropType.isMap() && Object.class.equals(targetType.getParameterTypes()[0].clazz())
                    && Object.class.equals(targetType.getParameterTypes()[1].clazz())) {
                final Map result = (Map) CommonUtil.newInstance(targetType.clazz());
                result.putAll((Map) obj);
                return (T) result;
            }
        }

        if (targetType.isCollection()) {
            if (srcPropType.isCollection() && Object.class.equals(targetType.getParameterTypes()[0].clazz())) {
                final Collection result = (Collection) CommonUtil.newInstance(targetType.clazz());
                result.addAll((Collection) obj);
                return (T) result;
            }
        }

        if (targetType.isNumber() && srcPropType.isNumber() && CLASS_TYPE_ENUM.containsKey(targetType.clazz())) {
            switch (CLASS_TYPE_ENUM.get(targetType.clazz())) {
                case 3:
                case 23:
                    return (T) (Byte) ((Number) obj).byteValue();

                case 4:
                case 24:
                    return (T) (Short) ((Number) obj).shortValue();

                case 5:
                case 25:
                    return (T) (Integer) ((Number) obj).intValue();

                case 6:
                case 26:
                    return (T) (Long) ((Number) obj).longValue();

                case 7:
                case 27:
                    return (T) (Float) ((Number) obj).floatValue();

                case 8:
                case 28:
                    return (T) (Double) ((Number) obj).doubleValue();

            }
        } else if ((targetType.clazz().equals(int.class) || targetType.clazz().equals(Integer.class)) && srcPropType.clazz().equals(Character.class)) {
            return (T) (Integer.valueOf(((Character) obj).charValue()));
        } else if ((targetType.clazz().equals(char.class) || targetType.clazz().equals(Character.class)) && srcPropType.clazz().equals(Integer.class)) {
            return (T) (Character.valueOf((char) ((Integer) obj).intValue()));
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
     * @return
     */
    public static boolean parseBoolean(final String str) {
        return isNullOrEmpty(str) ? false : Boolean.valueOf(str);
    }

    /**
     * Parses the char.
     *
     * @param str
     * @return
     */
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
     */
    public static byte parseByte(final String str) {
        if (CommonUtil.isNullOrEmpty(str)) {
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
     */
    public static short parseShort(final String str) {
        if (CommonUtil.isNullOrEmpty(str)) {
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
     */
    public static int parseInt(final String str) {
        if (CommonUtil.isNullOrEmpty(str)) {
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
     */
    public static long parseLong(final String str) {
        if (CommonUtil.isNullOrEmpty(str)) {
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
     */
    public static double parseDouble(final String str) {
        return isNullOrEmpty(str) ? 0d : Double.parseDouble(str);
    }

    /**
     * Base 64 encode.
     *
     * @param binaryData
     * @return
     */
    public static String base64Encode(final byte[] binaryData) {
        if (CommonUtil.isNullOrEmpty(binaryData)) {
            return CommonUtil.EMPTY_STRING;
        }

        return Base64.encodeBase64String(binaryData);
    }

    /**
     * Base 64 encode chunked.
     *
     * @param binaryData
     * @return
     */
    public static String base64EncodeChunked(final byte[] binaryData) {
        if (CommonUtil.isNullOrEmpty(binaryData)) {
            return CommonUtil.EMPTY_STRING;
        }

        return new String(Base64.encodeBase64Chunked(binaryData), Charsets.US_ASCII);
    }

    /**
     * Base 64 decode.
     *
     * @param base64String
     * @return
     */
    public static byte[] base64Decode(final String base64String) {
        if (CommonUtil.isNullOrEmpty(base64String)) {
            return CommonUtil.EMPTY_BYTE_ARRAY;
        }

        return Base64.decodeBase64(base64String);
    }

    /**
     * Base 64 decode to string.
     *
     * @param base64String
     * @return
     */
    public static String base64DecodeToString(final String base64String) {
        if (CommonUtil.isNullOrEmpty(base64String)) {
            return CommonUtil.EMPTY_STRING;
        }

        return new String(base64Decode(base64String));
    }

    /**
     * Base 64 url encode.
     *
     * @param binaryData
     * @return
     */
    public static String base64UrlEncode(final byte[] binaryData) {
        if (CommonUtil.isNullOrEmpty(binaryData)) {
            return CommonUtil.EMPTY_STRING;
        }

        return Base64.encodeBase64URLSafeString(binaryData);
    }

    /**
     * Base 64 url decode.
     *
     * @param base64String
     * @return
     */
    public static byte[] base64UrlDecode(final String base64String) {
        if (CommonUtil.isNullOrEmpty(base64String)) {
            return CommonUtil.EMPTY_BYTE_ARRAY;
        }

        return Base64.decodeBase64URL(base64String);
    }

    /**
     * Base 64 url decode to string.
     *
     * @param base64String
     * @return
     */
    public static String base64UrlDecodeToString(final String base64String) {
        if (CommonUtil.isNullOrEmpty(base64String)) {
            return CommonUtil.EMPTY_STRING;
        }

        return new String(Base64.decodeBase64URL(base64String));
    }

    /**
     *
     * @param parameters
     * @return
     */
    public static String urlEncode(final Object parameters) {
        if (parameters == null) {
            return CommonUtil.EMPTY_STRING;
        }

        return URLEncodedUtil.encode(parameters);
    }

    /**
     *
     * @param parameters
     * @param charset
     * @return
     */
    public static String urlEncode(final Object parameters, final Charset charset) {
        if (parameters == null) {
            return CommonUtil.EMPTY_STRING;
        }

        return URLEncodedUtil.encode(parameters, charset);
    }

    /**
     *
     * @param urlQuery
     * @return
     */
    public static Map<String, String> urlDecode(final String urlQuery) {
        if (CommonUtil.isNullOrEmpty(urlQuery)) {
            return new LinkedHashMap<>();
        }

        return URLEncodedUtil.decode(urlQuery);
    }

    /**
     *
     * @param urlQuery
     * @param charset
     * @return
     */
    public static Map<String, String> urlDecode(final String urlQuery, final Charset charset) {
        if (CommonUtil.isNullErrorMsg(urlQuery)) {
            return new LinkedHashMap<>();
        }

        return URLEncodedUtil.decode(urlQuery, charset);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param urlQuery
     * @return
     */
    public static <T> T urlDecode(final Class<? extends T> targetClass, final String urlQuery) {
        if (CommonUtil.isNullErrorMsg(urlQuery)) {
            return CommonUtil.newInstance(targetClass);
        }

        return URLEncodedUtil.decode(targetClass, urlQuery);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param urlQuery
     * @param charset
     * @return
     */
    public static <T> T urlDecode(final Class<? extends T> targetClass, final String urlQuery, final Charset charset) {
        if (CommonUtil.isNullOrEmpty(urlQuery)) {
            return CommonUtil.newInstance(targetClass);
        }

        return URLEncodedUtil.decode(targetClass, urlQuery, charset);
    }

    /**
     * Returns the UUID without '-'.
     *
     * @return
     * @see UUID#randomUUID().
     */
    public static String guid() {
        return uuid().replace("-", "");
    }

    /**
     * Returns an UUID.
     *
     * @return
     * @see UUID#randomUUID().
     */
    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * Checks if is entity.
     *
     * @param cls
     * @return true, if is entity
     * @see ClassUtil#isEntity(Class)
     * @deprecated replaced by {@code ClassUtil.isEntity(Class)}
     */
    @Deprecated
    public static boolean isEntity(final Class<?> cls) {
        return ClassUtil.isEntity(cls);
    }

    /**
     * Checks if is dirty marker.
     *
     * @param cls
     * @return true, if is dirty marker
     * @see DirtyMarkerUtil#isDirtyMarker(Class)
     * @deprecated replaced by {@code ClassUtil.isDirtyMarker(Class)}
     */
    @Deprecated
    public static boolean isDirtyMarker(final Class<?> cls) {
        return DirtyMarkerUtil.isDirtyMarker(cls);
    }

    /** The Constant notKryoCompatible. */
    private static final Set<Class<?>> notKryoCompatible = CommonUtil.newHashSet();

    /**
     *
     * @param <T>
     * @param entity
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T clone(final T entity) {
        return (T) clone(entity.getClass(), entity);
    }

    /**
     * Deeply copy by: entity -> serialize -> String/bytes -> deserialize -> new
     * entity.
     *
     * @param <T>
     * @param targetClass a Java Object what allows access to properties using getter
     *            and setter methods.
     * @param entity a Java Object what allows access to properties using getter
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

            DirtyMarkerUtil.setDirtyMarker(entity, copy);
        }

        return (T) copy;
    }

    /**
     * Returns a new created instance of the same class and set with same
     * properties retrieved by 'getXXX' method in the specified {@code entity}.
     *
     * @param <T>
     * @param entity a Java Object what allows access to properties using getter
     *            and setter methods.
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T copy(final T entity) {
        return copy((Class<T>) entity.getClass(), entity);
    }

    /**
     *
     * @param <T>
     * @param entity
     * @param selectPropNames
     * @return
     */
    public static <T> T copy(final T entity, final Collection<String> selectPropNames) {
        return copy((Class<T>) entity.getClass(), entity, selectPropNames);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param entity
     * @return
     */
    public static <T> T copy(final Class<? extends T> targetClass, final Object entity) {
        return copy(targetClass, entity, null);
    }

    /**
     * Returns a new created instance of the specified {@code cls} and set with
     * same properties retrieved by 'getXXX' method in the specified
     * {@code entity}.
     *
     * @param <T>
     * @param targetClass a Java Object what allows access to properties using getter
     *            and setter methods.
     * @param entity a Java Object what allows access to properties using getter
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

        copy = CommonUtil.newInstance(targetClass);

        merge(entity, copy, selectPropNames);

        DirtyMarkerUtil.setDirtyMarker(entity, copy);

        return copy;
    }

    /**
     *
     * @param <T>
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

        copy = CommonUtil.newInstance(targetClass);

        merge(entity, copy, ignoreUnknownProperty, ignorePropNames);

        DirtyMarkerUtil.setDirtyMarker(entity, copy);

        return copy;
    }

    /**
     *
     * @param sourceEntity
     * @param targetEntity
     */
    public static void merge(final Object sourceEntity, final Object targetEntity) {
        merge(sourceEntity, targetEntity, null);
    }

    /**
     * Set all the signed properties(including all primitive type properties) in
     * the specified {@code sourceEntity} to the specified {@code targetEntity}.
     *
     * @param sourceEntity a Java Object what allows access to properties using getter
     *            and setter methods.
     * @param targetEntity a Java Object what allows access to properties using getter
     *            and setter methods.
     * @param selectPropNames
     */
    public static void merge(final Object sourceEntity, final Object targetEntity, final Collection<String> selectPropNames) {
        final EntityInfo srcEntityInfo = ParserUtil.getEntityInfo(sourceEntity.getClass());
        final EntityInfo targetEntityInfo = ParserUtil.getEntityInfo(targetEntity.getClass());
        final boolean ignoreUnknownProperty = selectPropNames == null;

        if (selectPropNames == null) {
            if (sourceEntity instanceof DirtyMarker) {
                final Set<String> signedPropNames = DirtyMarkerUtil.signedPropNames((DirtyMarker) sourceEntity);

                if (signedPropNames.size() == 0) {
                    // logger.warn("no property is signed in the specified source entity: "
                    // + toString(entity));
                } else {
                    for (String propName : signedPropNames) {
                        targetEntityInfo.setPropValue(targetEntity, propName, srcEntityInfo.getPropValue(sourceEntity, propName), ignoreUnknownProperty);
                    }
                }
            } else {
                for (PropInfo propInfo : srcEntityInfo.propInfoList) {
                    targetEntityInfo.setPropValue(targetEntity, propInfo.name, propInfo.getPropValue(sourceEntity), ignoreUnknownProperty);
                }
            }
        } else {
            for (String propName : selectPropNames) {
                targetEntityInfo.setPropValue(targetEntity, propName, srcEntityInfo.getPropValue(sourceEntity, propName), ignoreUnknownProperty);
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
    public static void merge(final Object sourceEntity, final Object targetEntity, final boolean ignoreUnknownProperty, final Set<String> ignorePropNames) {
        final EntityInfo srcEntityInfo = ParserUtil.getEntityInfo(sourceEntity.getClass());
        final EntityInfo targetEntityInfo = ParserUtil.getEntityInfo(targetEntity.getClass());

        if (sourceEntity instanceof DirtyMarker) {
            final Set<String> signedPropNames = DirtyMarkerUtil.signedPropNames((DirtyMarker) sourceEntity);

            if (signedPropNames.size() == 0) {
                // logger.warn("no property is signed in the specified source entity: "
                // + toString(entity));
            } else {
                for (String propName : signedPropNames) {
                    if (ignorePropNames == null || ignorePropNames.contains(propName) == false) {
                        targetEntityInfo.setPropValue(targetEntity, propName, srcEntityInfo.getPropValue(sourceEntity, propName), ignoreUnknownProperty);
                    }
                }
            }
        } else {
            for (PropInfo propInfo : srcEntityInfo.propInfoList) {
                if (ignorePropNames == null || ignorePropNames.contains(propInfo.name) == false) {
                    targetEntityInfo.setPropValue(targetEntity, propInfo.name, propInfo.getPropValue(sourceEntity), ignoreUnknownProperty);
                }
            }
        }
    }

    /**
     *
     * @param entity
     * @param propNames
     */
    @SafeVarargs
    public static void erase(final Object entity, final String... propNames) {
        if (entity == null || CommonUtil.isNullOrEmpty(propNames)
                || (entity instanceof DirtyMarker && DirtyMarkerUtil.signedPropNames((DirtyMarker) entity).size() == 0)) {
            return;
        }

        final EntityInfo entityInfo = ParserUtil.getEntityInfo(entity.getClass());

        for (String propName : propNames) {
            entityInfo.setPropValue(entity, propName, null);
        }

        if (entity instanceof DirtyMarker) {
            final DirtyMarker dirtyMarkerEntity = (DirtyMarker) entity;

            for (String propName : propNames) {
                DirtyMarkerUtil.signedPropNames(dirtyMarkerEntity).remove(propName);
                DirtyMarkerUtil.dirtyPropNames(dirtyMarkerEntity).remove(propName);
            }
        }
    }

    /**
     *
     * @param entity
     * @param propNames
     */
    public static void erase(final Object entity, final Collection<String> propNames) {
        if (entity == null || CommonUtil.isNullOrEmpty(propNames)
                || (entity instanceof DirtyMarker && DirtyMarkerUtil.signedPropNames((DirtyMarker) entity).size() == 0)) {
            return;
        }

        final EntityInfo entityInfo = ParserUtil.getEntityInfo(entity.getClass());

        for (String propName : propNames) {
            entityInfo.setPropValue(entity, propName, null);
        }

        if (entity instanceof DirtyMarker) {
            final DirtyMarker dirtyMarkerEntity = (DirtyMarker) entity;

            DirtyMarkerUtil.signedPropNames(dirtyMarkerEntity).removeAll(propNames);
            DirtyMarkerUtil.dirtyPropNames(dirtyMarkerEntity).removeAll(propNames);
        }
    }

    /**
     *
     * @param entity
     */
    public static void eraseAll(final Object entity) {
        if (entity == null) {
            return;
        }

        final Class<?> cls = entity.getClass();
        final EntityInfo entityInfo = ParserUtil.getEntityInfo(cls);

        if (entity instanceof DirtyMarker) {
            final DirtyMarker dirtyMarkerEntity = (DirtyMarker) entity;
            final Set<String> signedPropNames = DirtyMarkerUtil.signedPropNames(dirtyMarkerEntity);

            if (signedPropNames.size() == 0) {
                // logger.warn("No property is signed in the specified source entity: " + toString(entity));
                return;
            }

            for (String propName : signedPropNames) {
                entityInfo.setPropValue(entity, propName, null);
            }

            DirtyMarkerUtil.signedPropNames(dirtyMarkerEntity).clear();
            DirtyMarkerUtil.dirtyPropNames(dirtyMarkerEntity).clear();
        } else {
            for (PropInfo propInfo : entityInfo.propInfoList) {
                propInfo.setPropValue(entity, null);
            }
        }
    }

    /**
     * Returns an empty {@code List} that is immutable.
     *
     * @param <T>
     * @return
     * @see Collections#emptyList()
     */
    public static <T> List<T> emptyList() {
        return EMPTY_LIST;
    }

    /**
     * Returns an empty {@code Set} that is immutable.
     *
     * @param <T>
     * @return
     * @see Collections#emptySet()
     */
    public static <T> Set<T> emptySet() {
        return EMPTY_SET;
    }

    /**
     * Returns an empty {@code SortedSet} that is immutable.
     *
     * @param <T>
     * @return
     * @see Collections#emptySortedSet()
     */
    public static <T> SortedSet<T> emptySortedSet() {
        return EMPTY_SORTED_SET;
    }

    /**
     * Returns an empty {@code emptyNavigableSet} that is immutable.
     *
     * @param <T>
     * @return
     * @see Collections#emptyNavigableSet()
     */
    public static <T> NavigableSet<T> emptyNavigableSet() {
        return EMPTY_NAVIGABLE_SET;
    }

    /**
     * Returns an empty {@code Map} that is immutable.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     * @see Collections#emptyMap()
     */
    public static <K, V> Map<K, V> emptyMap() {
        return EMPTY_MAP;
    }

    /**
     * Returns an empty {@code SortedMap} that is immutable.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     * @see Collections#emptySortedMap()
     */
    public static <K, V> SortedMap<K, V> emptySortedMap() {
        return EMPTY_SORTED_MAP;
    }

    /**
     * Returns an empty {@code NavigableMap} that is immutable.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     * @see Collections#emptyNavigableMap()
     */
    public static <K, V> NavigableMap<K, V> emptyNavigableMap() {
        return EMPTY_NAVIGABLE_MAP;
    }

    /**
     * Returns an empty {@code Iterator} that is immutable.
     *
     * @param <T>
     * @return
     * @see Collections#emptyIterator()
     */
    public static <T> Iterator<T> emptyIterator() {
        return EMPTY_ITERATOR;
    }

    /**
     * Returns an empty {@code ListIterator} that is immutable.
     *
     * @param <T>
     * @return
     * @see Collections#emptyListIterator()
     */
    public static <T> ListIterator<T> emptyListIterator() {
        return EMPTY_LIST_ITERATOR;
    }

    /** The Constant EMPTY_INPUT_STREAM. */
    private static final ByteArrayInputStream EMPTY_INPUT_STREAM = new ByteArrayInputStream(CommonUtil.EMPTY_BYTE_ARRAY);

    /**
     * Empty input stream.
     *
     * @return
     */
    public static InputStream emptyInputStream() {
        return EMPTY_INPUT_STREAM;
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return true, if successful
     */
    public static <T> boolean anyNull(final T a, final T b) {
        return a == null || b == null;
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @param c
     * @return true, if successful
     */
    public static <T> boolean anyNull(final T a, final T b, final T c) {
        return a == null || b == null || c == null;
    }

    /**
     *
     * @param <T>
     * @param a
     * @return true, if successful
     */
    @SafeVarargs
    public static <T> boolean anyNull(final T... a) {
        if (CommonUtil.isNullOrEmpty(a)) {
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
     * @param <T>
     * @param c
     * @return true, if successful
     */
    public static <T> boolean anyNull(final Collection<T> c) {
        if (CommonUtil.isNullOrEmpty(c)) {
            return false;
        }

        for (T e : c) {
            if (e == null) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return true, if successful
     */
    public static <T> boolean allNull(final T a, final T b) {
        return a == null && b == null;
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @param c
     * @return true, if successful
     */
    public static <T> boolean allNull(final T a, final T b, final T c) {
        return a == null && b == null && c == null;
    }

    /**
     *
     * @param <T>
     * @param a
     * @return true, if successful
     */
    @SafeVarargs
    public static <T> boolean allNull(final T... a) {
        if (CommonUtil.isNullOrEmpty(a)) {
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
     * @param <T>
     * @param c
     * @return true, if successful
     */
    public static <T> boolean allNull(final Collection<T> c) {
        if (CommonUtil.isNullOrEmpty(c)) {
            return true;
        }

        for (T e : c) {
            if (e != null) {
                return false;
            }
        }

        return true;
    }

    /**
     * Any null or empty.
     *
     * @param cs1
     * @param cs2
     * @param cs3
     * @return true, if successful
     */
    public static boolean anyNullOrEmpty(final CharSequence cs1, final CharSequence cs2, final CharSequence cs3) {
        return CommonUtil.isNullOrEmpty(cs1) || CommonUtil.isNullOrEmpty(cs2) || CommonUtil.isNullOrEmpty(cs3);
    }

    /**
     * Any null or empty.
     *
     * @param css
     * @return true, if successful
     */
    @SafeVarargs
    public static boolean anyNullOrEmpty(final CharSequence... css) {
        if (CommonUtil.isNullOrEmpty(css)) {
            return false;
        }

        for (CharSequence cs : css) {
            if (CommonUtil.isNullOrEmpty(cs)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Any null or empty.
     *
     * @param css
     * @return true, if successful
     */
    public static boolean anyNullOrEmpty(final Collection<? extends CharSequence> css) {
        if (CommonUtil.isNullOrEmpty(css)) {
            return false;
        }

        for (CharSequence cs : css) {
            if (CommonUtil.isNullOrEmpty(cs)) {
                return true;
            }
        }

        return false;
    }

    /**
     * All null or empty.
     *
     * @param cs1
     * @param cs2
     * @param cs3
     * @return true, if successful
     */
    public static boolean allNullOrEmpty(final CharSequence cs1, final CharSequence cs2, final CharSequence cs3) {
        return CommonUtil.isNullOrEmpty(cs1) && CommonUtil.isNullOrEmpty(cs2) && CommonUtil.isNullOrEmpty(cs3);
    }

    /**
     * All null or empty.
     *
     * @param css
     * @return true, if successful
     */
    @SafeVarargs
    public static boolean allNullOrEmpty(final CharSequence... css) {
        if (CommonUtil.isNullOrEmpty(css)) {
            return true;
        }

        for (CharSequence cs : css) {
            if (CommonUtil.isNullOrEmpty(cs) == false) {
                return false;
            }
        }

        return true;
    }

    /**
     * All null or empty.
     *
     * @param css
     * @return true, if successful
     */
    public static boolean allNullOrEmpty(final Collection<? extends CharSequence> css) {
        if (CommonUtil.isNullOrEmpty(css)) {
            return true;
        }

        for (CharSequence cs : css) {
            if (CommonUtil.isNullOrEmpty(cs) == false) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> Nullable<T> first(final Collection<T> c) {
        if (CommonUtil.isNullOrEmpty(c)) {
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
     * @param c
     * @return
     */
    public static <T> Nullable<T> last(final Collection<T> c) {
        if (CommonUtil.isNullOrEmpty(c)) {
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
        if (CommonUtil.isNullOrEmpty(a)) {
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
    public static <T> Optional<T> firstNonNull(final Collection<T> c) {
        if (CommonUtil.isNullOrEmpty(c)) {
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
        if (CommonUtil.isNullOrEmpty(a)) {
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
    public static <T> Optional<T> lastNonNull(final Collection<T> c) {
        if (CommonUtil.isNullOrEmpty(c)) {
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
            Iterators.lastNonNull(c.iterator());
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

        return Iterators.lastNonNull(map.entrySet().iterator());
    }

    /**
     * First or null if empty.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> T firstOrNullIfEmpty(final Collection<T> c) {
        return firstOrDefaultIfEmpty(c, null);
    }

    /**
     * First or default if empty.
     *
     * @param <T>
     * @param c
     * @param defaultValueForEmpty
     * @return
     */
    public static <T> T firstOrDefaultIfEmpty(final Collection<T> c, final T defaultValueForEmpty) {
        if (CommonUtil.isNullOrEmpty(c)) {
            return defaultValueForEmpty;
        }

        if (c instanceof List && c instanceof RandomAccess) {
            return ((List<T>) c).get(0);
        } else {
            return c.iterator().next();
        }
    }

    /**
     * Last or null if empty.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> T lastOrNullIfEmpty(final Collection<T> c) {
        return lastOrDefaultIfEmpty(c, null);
    }

    /**
     * Last or default if empty.
     *
     * @param <T>
     * @param c
     * @param defaultValueForEmpty
     * @return
     */
    public static <T> T lastOrDefaultIfEmpty(final Collection<T> c, final T defaultValueForEmpty) {
        if (CommonUtil.isNullOrEmpty(c)) {
            return defaultValueForEmpty;
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
     * Find first.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param predicate the predicate
     * @return the nullable
     * @throws E the e
     */
    @SuppressWarnings("deprecation")
    public static <T, E extends Exception> Nullable<T> findFirst(final T[] a, final Throwables.Predicate<? super T, E> predicate) throws E {
        return Iterables.findFirst(a, predicate);
    }

    /**
     * Find first.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param predicate the predicate
     * @return the nullable
     * @throws E the e
     */
    @SuppressWarnings("deprecation")
    public static <T, E extends Exception> Nullable<T> findFirst(final Collection<? extends T> c, Throwables.Predicate<? super T, E> predicate) throws E {
        return Iterables.findFirst(c, predicate);
    }

    /**
     * Find last.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param predicate the predicate
     * @return the nullable
     * @throws E the e
     */
    @SuppressWarnings("deprecation")
    public static <T, E extends Exception> Nullable<T> findLast(final T[] a, final Throwables.Predicate<? super T, E> predicate) throws E {
        return Iterables.findLast(a, predicate);
    }

    /**
     * Find last.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param predicate the predicate
     * @return the nullable
     * @throws E the e
     */
    @SuppressWarnings("deprecation")
    public static <T, E extends Exception> Nullable<T> findLast(final Collection<? extends T> c, Throwables.Predicate<? super T, E> predicate) throws E {
        return Iterables.findLast(c, predicate);
    }

    /**
     * Find first non null.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param predicate the predicate
     * @return the optional
     * @throws E the e
     */
    @SuppressWarnings("deprecation")
    public static <T, E extends Exception> Optional<T> findFirstNonNull(final T[] a, final Throwables.Predicate<? super T, E> predicate) throws E {
        return Iterables.findFirstNonNull(a, predicate);
    }

    /**
     * Find first non null.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param predicate the predicate
     * @return the optional
     * @throws E the e
     */
    @SuppressWarnings("deprecation")
    public static <T, E extends Exception> Optional<T> findFirstNonNull(final Collection<? extends T> c, Throwables.Predicate<? super T, E> predicate)
            throws E {
        return Iterables.findFirstNonNull(c, predicate);
    }

    /**
     * Find last non null.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param predicate the predicate
     * @return the optional
     * @throws E the e
     */
    @SuppressWarnings("deprecation")
    public static <T, E extends Exception> Optional<T> findLastNonNull(final T[] a, final Throwables.Predicate<? super T, E> predicate) throws E {
        return Iterables.findLastNonNull(a, predicate);
    }

    /**
     * Find last non null.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param predicate the predicate
     * @return the optional
     * @throws E the e
     */
    @SuppressWarnings("deprecation")
    public static <T, E extends Exception> Optional<T> findLastNonNull(final Collection<? extends T> c, Throwables.Predicate<? super T, E> predicate) throws E {
        return Iterables.findLastNonNull(c, predicate);
    }

    /**
     * Return at most first <code>n</code> elements.
     *
     * @param <T> the generic type
     * @param c the c
     * @param n the n
     * @return the list
     */
    public static <T> List<T> first(final Collection<? extends T> c, final int n) {
        N.checkArgument(n >= 0, "'n' can't be negative: " + n);

        if (N.isNullOrEmpty(c) || n == 0) {
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
     * @param <T> the generic type
     * @param c the c
     * @param n the n
     * @return the list
     */
    public static <T> List<T> last(final Collection<? extends T> c, final int n) {
        N.checkArgument(n >= 0, "'n' can't be negative: " + n);

        if (N.isNullOrEmpty(c) || n == 0) {
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
     * Returns a read-only <code>ImmutableCollection</code>.
     *
     * @param <T> the generic type
     * @param c the c
     * @param fromIndex the from index
     * @param toIndex the to index
     * @return the immutable collection<? extends t>
     */
    public static <T> ImmutableList<? extends T> slice(final List<? extends T> c, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.size(c));

        if (N.isNullOrEmpty(c)) {
            return ImmutableList.empty();
        }

        return ImmutableList.of(((List<T>) c).subList(fromIndex, toIndex));
    }

    /**
     * Returns a read-only <code>ImmutableCollection</code>.
     *
     * @param <T> the generic type
     * @param c the c
     * @param fromIndex the from index
     * @param toIndex the to index
     * @return the immutable collection<? extends t>
     */
    public static <T> ImmutableCollection<? extends T> slice(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.size(c));

        if (N.isNullOrEmpty(c)) {
            return ImmutableList.empty();
        }

        if (c instanceof List) {
            return slice((List<T>) c, fromIndex, toIndex);
        }

        return new Slice<>(c, fromIndex, toIndex);
    }

    /**
     * Returns a read-only <code>Seq</code>.
     *
     * @param <T> the generic type
     * @param a the a
     * @param fromIndex the from index
     * @param toIndex the to index
     * @return the immutable collection<? extends t>
     */
    public static <T> ImmutableList<? extends T> slice(final T[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a)) {
            return ImmutableList.empty();
        }

        return slice(Array.asList(a), fromIndex, toIndex);
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
     * @param <T>
     * @param list
     * @return
     */
    public static <T> List<T> nullToEmpty(final List<T> list) {
        return list == null ? CommonUtil.<T> emptyList() : list;
    }

    /**
     * Returns an immutable empty set if the specified Set is <code>null</code>, otherwise itself is returned.
     *
     * @param <T>
     * @param set
     * @return
     */
    public static <T> Set<T> nullToEmpty(final Set<T> set) {
        return set == null ? CommonUtil.<T> emptySet() : set;
    }

    /**
     * Returns an immutable empty <code>SortedSet</code> if the specified SortedSet is <code>null</code>, otherwise itself is returned.
     *
     * @param <T>
     * @param set
     * @return
     */
    public static <T> SortedSet<T> nullToEmpty(final SortedSet<T> set) {
        return set == null ? CommonUtil.<T> emptySortedSet() : set;
    }

    /**
     * Returns an immutable empty <code>NavigableSet</code> if the specified NavigableSet is <code>null</code>, otherwise itself is returned.
     *
     * @param <T>
     * @param set
     * @return
     */
    public static <T> NavigableSet<T> nullToEmpty(final NavigableSet<T> set) {
        return set == null ? CommonUtil.<T> emptyNavigableSet() : set;
    }

    /**
     * Returns an immutable empty map if the specified Map is <code>null</code>, otherwise itself is returned.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     */
    public static <K, V> Map<K, V> nullToEmpty(final Map<K, V> map) {
        return map == null ? CommonUtil.<K, V> emptyMap() : map;
    }

    /**
     * Returns an immutable empty <code>SortedMap</code> if the specified SortedMap is <code>null</code>, otherwise itself is returned.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     */
    public static <K, V> SortedMap<K, V> nullToEmpty(final SortedMap<K, V> map) {
        return map == null ? CommonUtil.<K, V> emptySortedMap() : map;
    }

    /**
     * Returns an immutable empty <code>NavigableMap</code> if the specified NavigableMap is <code>null</code>, otherwise itself is returned.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     */
    public static <K, V> NavigableMap<K, V> nullToEmpty(final NavigableMap<K, V> map) {
        return map == null ? CommonUtil.<K, V> emptyNavigableMap() : map;
    }

    /**
     * Returns an immutable empty <code>Iterator</code> if the specified Iterator is <code>null</code>, otherwise itself is returned.
     *
     * @param <T>
     * @param iter
     * @return
     */
    public static <T> Iterator<T> nullToEmpty(final Iterator<T> iter) {
        return iter == null ? CommonUtil.<T> emptyIterator() : iter;
    }

    /**
     * Returns an immutable empty <code>ListIterator</code> if the specified ListIterator is <code>null</code>, otherwise itself is returned.
     *
     * @param <T>
     * @param iter
     * @return
     */
    public static <T> ListIterator<T> nullToEmpty(final ListIterator<T> iter) {
        return iter == null ? CommonUtil.<T> emptyListIterator() : iter;
    }

    /**
     * Null to empty.
     *
     * @param str
     * @return
     */
    public static String nullToEmpty(final String str) {
        return str == null ? EMPTY_STRING : str;
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
    public static String[] nullToEmpty(final String[] a) {
        return a == null ? EMPTY_STRING_ARRAY : a;
    }

    /**
     * Null to empty.
     *
     * @param a
     * @return
     * @deprecated replaced by {@link CommonUtil#nullToEmpty(Object[], Class)}
     */
    @Deprecated
    public static Object[] nullToEmpty(final Object[] a) {
        return a == null ? EMPTY_OBJECT_ARRAY : a;
    }

    /**
     * Null to empty.
     *
     * @param <T>
     * @param arrayType
     * @param a
     * @return
     * @deprecated replaced by {@link CommonUtil#nullToEmpty(Object[], Class)}
     */
    @Deprecated
    public static <T> T[] nullToEmpty(final Class<T[]> arrayType, final T[] a) {
        return a == null ? (T[]) CommonUtil.newArray(arrayType.getComponentType(), 0) : a;
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
        return a == null ? (T[]) CommonUtil.newArray(arrayType.getComponentType(), 0) : a;
    }

    /**
     * Returns an immutable empty Collection if the specified ImmutableCollection is <code>null</code>, otherwise itself is returned.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> ImmutableCollection<T> nullToEmpty(final ImmutableCollection<T> c) {
        return c == null ? ImmutableList.<T> empty() : c;
    }

    /**
     * Returns an immutable empty list if the specified ImmutableList is <code>null</code>, otherwise itself is returned.
     *
     * @param <T>
     * @param list
     * @return
     */
    public static <T> ImmutableList<T> nullToEmpty(final ImmutableList<T> list) {
        return list == null ? ImmutableList.<T> empty() : list;
    }

    /**
    * Returns an immutable empty list if the specified ImmutableSet is <code>null</code>, otherwise itself is returned.
    *
    * @param <T>
    * @param set
    * @return
    */
    public static <T> ImmutableSet<T> nullToEmpty(final ImmutableSet<T> set) {
        return set == null ? ImmutableSet.<T> empty() : set;
    }

    /**
     * Returns an immutable empty list if the specified ImmutableSortedSet is <code>null</code>, otherwise itself is returned.
     *
     * @param <T>
     * @param set
     * @return
     */
    public static <T> ImmutableSortedSet<T> nullToEmpty(final ImmutableSortedSet<T> set) {
        return set == null ? ImmutableSortedSet.<T> empty() : set;
    }

    /**
     * Returns an immutable empty list if the specified ImmutableNavigableSet is <code>null</code>, otherwise itself is returned.
     *
     * @param <T>
     * @param set
     * @return
     */
    public static <T> ImmutableNavigableSet<T> nullToEmpty(final ImmutableNavigableSet<T> set) {
        return set == null ? ImmutableNavigableSet.<T> empty() : set;
    }

    /**
     * Returns an immutable empty map if the specified ImmutableMap is <code>null</code>, otherwise itself is returned.
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
     * Returns an immutable empty map if the specified ImmutableSortedMap is <code>null</code>, otherwise itself is returned.
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
     * Returns an immutable empty map if the specified ImmutableNavigableMap is <code>null</code>, otherwise itself is returned.
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
     * Returns an immutable empty map if the specified ImmutableBiMap is <code>null</code>, otherwise itself is returned.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map
     * @return
     */
    public static <K, V> ImmutableBiMap<K, V> nullToEmpty(final ImmutableBiMap<K, V> map) {
        return map == null ? ImmutableBiMap.<K, V> empty() : map;
    }

    /**
     * Checks if is null or empty.
     *
     * @param s
     * @return true, if is null or empty
     */
    public static boolean isNullOrEmpty(final CharSequence s) {
        return (s == null) || (s.length() == 0);
    }

    /**
     * Checks if is null or empty.
     *
     * @param a
     * @return true, if is null or empty
     */
    public static boolean isNullOrEmpty(final boolean[] a) {
        return (a == null) || (a.length == 0);
    }

    /**
     * Checks if is null or empty.
     *
     * @param a
     * @return true, if is null or empty
     */
    public static boolean isNullOrEmpty(final char[] a) {
        return (a == null) || (a.length == 0);
    }

    /**
     * Checks if is null or empty.
     *
     * @param a
     * @return true, if is null or empty
     */
    public static boolean isNullOrEmpty(final byte[] a) {
        return (a == null) || (a.length == 0);
    }

    /**
     * Checks if is null or empty.
     *
     * @param a
     * @return true, if is null or empty
     */
    public static boolean isNullOrEmpty(final short[] a) {
        return (a == null) || (a.length == 0);
    }

    /**
     * Checks if is null or empty.
     *
     * @param a
     * @return true, if is null or empty
     */
    public static boolean isNullOrEmpty(final int[] a) {
        return (a == null) || (a.length == 0);
    }

    /**
     * Checks if is null or empty.
     *
     * @param a
     * @return true, if is null or empty
     */
    public static boolean isNullOrEmpty(final long[] a) {
        return (a == null) || (a.length == 0);
    }

    /**
     * Checks if is null or empty.
     *
     * @param a
     * @return true, if is null or empty
     */
    public static boolean isNullOrEmpty(final float[] a) {
        return (a == null) || (a.length == 0);
    }

    /**
     * Checks if is null or empty.
     *
     * @param a
     * @return true, if is null or empty
     */
    public static boolean isNullOrEmpty(final double[] a) {
        return (a == null) || (a.length == 0);
    }

    /**
     * Checks if is null or empty.
     *
     * @param a
     * @return true, if is null or empty
     */
    public static boolean isNullOrEmpty(final Object[] a) {
        return (a == null) || (a.length == 0);
    }

    /**
     * Checks if is null or empty.
     *
     * @param c
     * @return true, if is null or empty
     */
    public static boolean isNullOrEmpty(final Collection<?> c) {
        return (c == null) || (c.isEmpty());
    }

    /**
     * Checks if is null or empty.
     *
     * @param m
     * @return true, if is null or empty
     */
    public static boolean isNullOrEmpty(final Map<?, ?> m) {
        return (m == null) || (m.isEmpty());
    }

    /**
     * Checks if is null or empty.
     *
     * @param list
     * @return true, if is null or empty
     */
    @SuppressWarnings("rawtypes")
    public static boolean isNullOrEmpty(final PrimitiveList list) {
        return (list == null) || (list.isEmpty());
    }

    /**
     * Checks if is null or empty.
     *
     * @param s
     * @return true, if is null or empty
     */
    public static boolean isNullOrEmpty(final Multiset<?> s) {
        return (s == null) || (s.isEmpty());
    }

    /**
     * Checks if is null or empty.
     *
     * @param s
     * @return true, if is null or empty
     */
    public static boolean isNullOrEmpty(final LongMultiset<?> s) {
        return (s == null) || (s.isEmpty());
    }

    /**
     * Checks if is null or empty.
     *
     * @param m
     * @return true, if is null or empty
     */
    public static boolean isNullOrEmpty(final Multimap<?, ?, ?> m) {
        return (m == null) || (m.isEmpty());
    }

    /**
     * Checks if is null or empty.
     *
     * @param rs
     * @return true, if is null or empty
     */
    public static boolean isNullOrEmpty(final DataSet rs) {
        return (rs == null) || (rs.isEmpty());
    }

    /**
     * Checks if is null or empty or blank.
     *
     * @param s
     * @return true, if is null or empty or blank
     */
    // DON'T change 'OrEmptyOrBlank' to 'OrBlank' because of the occurring order in the auto-completed context menu.
    public static boolean isNullOrEmptyOrBlank(final CharSequence s) {
        if (CommonUtil.isNullOrEmpty(s)) {
            return true;
        }

        for (int i = 0, len = s.length(); i < len; i++) {
            if (Character.isWhitespace(s.charAt(i)) == false) {
                return false;
            }
        }

        return true;
    }

    /**
     * Not null or empty.
     *
     * @param s
     * @return true, if successful
     */
    public static boolean notNullOrEmpty(final CharSequence s) {
        return (s != null) && (s.length() > 0);
    }

    /**
     * Not null or empty.
     *
     * @param a
     * @return true, if successful
     */
    public static boolean notNullOrEmpty(final boolean[] a) {
        return (a != null) && (a.length > 0);
    }

    /**
     * Not null or empty.
     *
     * @param a
     * @return true, if successful
     */
    public static boolean notNullOrEmpty(final char[] a) {
        return (a != null) && (a.length > 0);
    }

    /**
     * Not null or empty.
     *
     * @param a
     * @return true, if successful
     */
    public static boolean notNullOrEmpty(final byte[] a) {
        return (a != null) && (a.length > 0);
    }

    /**
     * Not null or empty.
     *
     * @param a
     * @return true, if successful
     */
    public static boolean notNullOrEmpty(final short[] a) {
        return (a != null) && (a.length > 0);
    }

    /**
     * Not null or empty.
     *
     * @param a
     * @return true, if successful
     */
    public static boolean notNullOrEmpty(final int[] a) {
        return (a != null) && (a.length > 0);
    }

    /**
     * Not null or empty.
     *
     * @param a
     * @return true, if successful
     */
    public static boolean notNullOrEmpty(final long[] a) {
        return (a != null) && (a.length > 0);
    }

    /**
     * Not null or empty.
     *
     * @param a
     * @return true, if successful
     */
    public static boolean notNullOrEmpty(final float[] a) {
        return (a != null) && (a.length > 0);
    }

    /**
     * Not null or empty.
     *
     * @param a
     * @return true, if successful
     */
    public static boolean notNullOrEmpty(final double[] a) {
        return (a != null) && (a.length > 0);
    }

    /**
     * Not null or empty.
     *
     * @param a
     * @return true, if successful
     */
    public static boolean notNullOrEmpty(final Object[] a) {
        return (a != null) && (a.length > 0);
    }

    /**
     * Not null or empty.
     *
     * @param c
     * @return true, if successful
     */
    public static boolean notNullOrEmpty(final Collection<?> c) {
        return (c != null) && (c.size() > 0);
    }

    /**
     * Not null or empty.
     *
     * @param m
     * @return true, if successful
     */
    public static boolean notNullOrEmpty(final Map<?, ?> m) {
        return (m != null) && (m.size() > 0);
    }

    /**
     * Not null or empty.
     *
     * @param list
     * @return true, if successful
     */
    @SuppressWarnings("rawtypes")
    public static boolean notNullOrEmpty(final PrimitiveList list) {
        return (list != null) && (list.size() > 0);
    }

    /**
     * Not null or empty.
     *
     * @param s
     * @return true, if successful
     */
    public static boolean notNullOrEmpty(final Multiset<?> s) {
        return (s != null) && (s.size() > 0);
    }

    /**
     * Not null or empty.
     *
     * @param s
     * @return true, if successful
     */
    public static boolean notNullOrEmpty(final LongMultiset<?> s) {
        return (s != null) && (s.size() > 0);
    }

    /**
     * Not null or empty.
     *
     * @param m
     * @return true, if successful
     */
    public static boolean notNullOrEmpty(final Multimap<?, ?, ?> m) {
        return (m != null) && (m.size() > 0);
    }

    /**
     * Not null or empty.
     *
     * @param rs
     * @return true, if successful
     */
    public static boolean notNullOrEmpty(final DataSet rs) {
        return (rs != null) && (rs.size() > 0);
    }

    /**
     * Not null or empty or blank.
     *
     * @param s
     * @return true, if successful
     */
    // DON'T change 'OrEmptyOrBlank' to 'OrBlank' because of the occurring order in the auto-completed context menu.
    public static boolean notNullOrEmptyOrBlank(final CharSequence s) {
        return !CommonUtil.isNullOrEmptyOrBlank(s);
    }

    /**
     * Checks if is null error msg.
     *
     * @param msg
     * @return true, if is null error msg
     */
    private static boolean isNullErrorMsg(final String msg) {
        // shortest message: "it is null"
        return msg.length() > 9 && msg.indexOf(WD._SPACE) > 0;
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
     * @param fromIndex
     * @param size
     * @param length
     * @throws IndexOutOfBoundsException the index out of bounds exception
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
     *
     * @param b
     * @param errorMessageTemplate
     * @param p
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
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
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
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     * @param p3
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
     *
     * @param b
     * @param errorMessageTemplate
     * @param p
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
     *
     * @param b
     * @param errorMessageTemplate
     * @param p
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
     *
     * @param b
     * @param errorMessageTemplate
     * @param p
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
     *
     * @param b
     * @param errorMessageTemplate
     * @param p
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
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
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
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     * @param p3
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
     *
     * @param b
     * @param errorMessageTemplate
     * @param p
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
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
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
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     * @param p3
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
     *
     * @param b
     * @param errorMessageTemplate
     * @param p
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
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
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
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     * @param p3
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
     *
     * @param b
     * @param errorMessageTemplate
     * @param p
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
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
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
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     * @param p3
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
     *
     * @param b
     * @param errorMessageTemplate
     * @param p
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
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
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
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     * @param p3
     */
    public static void checkArgument(boolean b, String errorMessageTemplate, Object p1, Object p2, Object p3) {
        if (!b) {
            throw new IllegalArgumentException(format(errorMessageTemplate, p1, p2, p3));
        }
    }

    /**
     *
     * @param <E>
     * @param b
     * @param errorMessageSupplier
     */
    public static void checkArgument(boolean b, Supplier<String> errorMessageSupplier) {
        if (!b) {
            throw new IllegalArgumentException(errorMessageSupplier.get());
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
    public static <T> T checkArgNotNull(final T obj) {
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
     * @param <T>
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static <T extends CharSequence> T checkArgNotNullOrEmpty(final T arg, final String argNameOrErrorMsg) {
        if (CommonUtil.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == CommonUtil.INDEX_NOT_FOUND) {
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
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static boolean[] checkArgNotNullOrEmpty(final boolean[] arg, final String argNameOrErrorMsg) {
        if (CommonUtil.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == CommonUtil.INDEX_NOT_FOUND) {
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
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static char[] checkArgNotNullOrEmpty(final char[] arg, final String argNameOrErrorMsg) {
        if (CommonUtil.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == CommonUtil.INDEX_NOT_FOUND) {
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
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static byte[] checkArgNotNullOrEmpty(final byte[] arg, final String argNameOrErrorMsg) {
        if (CommonUtil.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == CommonUtil.INDEX_NOT_FOUND) {
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
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static short[] checkArgNotNullOrEmpty(final short[] arg, final String argNameOrErrorMsg) {
        if (CommonUtil.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == CommonUtil.INDEX_NOT_FOUND) {
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
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static int[] checkArgNotNullOrEmpty(final int[] arg, final String argNameOrErrorMsg) {
        if (CommonUtil.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == CommonUtil.INDEX_NOT_FOUND) {
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
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static long[] checkArgNotNullOrEmpty(final long[] arg, final String argNameOrErrorMsg) {
        if (CommonUtil.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == CommonUtil.INDEX_NOT_FOUND) {
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
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static float[] checkArgNotNullOrEmpty(final float[] arg, final String argNameOrErrorMsg) {
        if (CommonUtil.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == CommonUtil.INDEX_NOT_FOUND) {
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
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static double[] checkArgNotNullOrEmpty(final double[] arg, final String argNameOrErrorMsg) {
        if (CommonUtil.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == CommonUtil.INDEX_NOT_FOUND) {
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
     * @param <T>
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static <T> T[] checkArgNotNullOrEmpty(final T[] arg, final String argNameOrErrorMsg) {
        if (CommonUtil.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == CommonUtil.INDEX_NOT_FOUND) {
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
     * @param <T>
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static <T extends Collection<?>> T checkArgNotNullOrEmpty(final T arg, final String argNameOrErrorMsg) {
        if (CommonUtil.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == CommonUtil.INDEX_NOT_FOUND) {
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
     * @param <T>
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is {@code null} or empty.
     */
    public static <T extends Map<?, ?>> T checkArgNotNullOrEmpty(final T arg, final String argNameOrErrorMsg) {
        if (CommonUtil.isNullOrEmpty(arg)) {
            if (argNameOrErrorMsg.indexOf(' ') == CommonUtil.INDEX_NOT_FOUND) {
                throw new IllegalArgumentException("'" + argNameOrErrorMsg + "' can not be null or empty");
            } else {
                throw new IllegalArgumentException(argNameOrErrorMsg);
            }
        }

        return arg;
    }

    /**
     * Check if the specified parameter is null or empty or blank.
     *
     * @param <T>
     * @param arg
     * @param msg name of parameter or error message
     * @return
     * @throws IllegalArgumentException if the specified parameter is null or empty.
     */
    // DON'T change 'OrEmptyOrBlank' to 'OrBlank' because of the occurring order in the auto-completed context menu.
    public static <T extends CharSequence> T checkArgNotNullOrEmptyOrBlank(final T arg, final String msg) {
        if (CommonUtil.isNullOrEmptyOrBlank(arg)) {
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
     * @return
     * @throws IllegalArgumentException if the specified {@code arg} is negative.
     */
    public static int checkArgNotNegative(final int arg, final String argNameOrErrorMsg) {
        if (arg < 0) {
            if (argNameOrErrorMsg.indexOf(' ') == CommonUtil.INDEX_NOT_FOUND) {
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
    public static long checkArgNotNegative(final long arg, final String argNameOrErrorMsg) {
        if (arg < 0) {
            if (argNameOrErrorMsg.indexOf(' ') == CommonUtil.INDEX_NOT_FOUND) {
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
    public static double checkArgNotNegative(final double arg, final String argNameOrErrorMsg) {
        if (arg < 0) {
            if (argNameOrErrorMsg.indexOf(' ') == CommonUtil.INDEX_NOT_FOUND) {
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
    public static int checkArgPositive(final int arg, final String argNameOrErrorMsg) {
        if (arg <= 0) {
            if (argNameOrErrorMsg.indexOf(' ') == CommonUtil.INDEX_NOT_FOUND) {
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
    public static long checkArgPositive(final long arg, final String argNameOrErrorMsg) {
        if (arg <= 0) {
            if (argNameOrErrorMsg.indexOf(' ') == CommonUtil.INDEX_NOT_FOUND) {
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
    public static double checkArgPositive(final double arg, final String argNameOrErrorMsg) {
        if (arg <= 0) {
            if (argNameOrErrorMsg.indexOf(' ') == CommonUtil.INDEX_NOT_FOUND) {
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
     * @param <E>
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
            sb.append(CommonUtil.toString(arg));
            sb.append(template, placeholderStart + 2, template.length());
        } else {
            sb.append(" [");
            sb.append(CommonUtil.toString(arg));
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
            sb.append(CommonUtil.toString(arg1));
            templateStart = placeholderStart + 2;
            placeholderStart = template.indexOf(placeholder, templateStart);

            if (placeholderStart >= 0) {
                cnt++;
                sb.append(template, templateStart, placeholderStart);
                sb.append(CommonUtil.toString(arg2));
                templateStart = placeholderStart + 2;
            }

            sb.append(template, templateStart, template.length());
        }

        if (cnt == 0) {
            sb.append(" [");
            sb.append(CommonUtil.toString(arg1));
            sb.append(", ");
            sb.append(CommonUtil.toString(arg2));
            sb.append(']');
        } else if (cnt == 1) {
            sb.append(" [");
            sb.append(CommonUtil.toString(arg2));
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
            sb.append(CommonUtil.toString(arg1));
            templateStart = placeholderStart + 2;
            placeholderStart = template.indexOf(placeholder, templateStart);

            if (placeholderStart >= 0) {
                cnt++;
                sb.append(template, templateStart, placeholderStart);
                sb.append(CommonUtil.toString(arg2));
                templateStart = placeholderStart + 2;
                placeholderStart = template.indexOf(placeholder, templateStart);

                if (placeholderStart >= 0) {
                    cnt++;
                    sb.append(template, templateStart, placeholderStart);
                    sb.append(CommonUtil.toString(arg3));
                    templateStart = placeholderStart + 2;
                }
            }

            sb.append(template, templateStart, template.length());
        }

        if (cnt == 0) {
            sb.append(" [");
            sb.append(CommonUtil.toString(arg1));
            sb.append(", ");
            sb.append(CommonUtil.toString(arg2));
            sb.append(", ");
            sb.append(CommonUtil.toString(arg3));
            sb.append(']');
        } else if (cnt == 1) {
            sb.append(" [");
            sb.append(CommonUtil.toString(arg2));
            sb.append(", ");
            sb.append(CommonUtil.toString(arg3));
            sb.append(']');
        } else if (cnt == 2) {
            sb.append(" [");
            sb.append(CommonUtil.toString(arg3));
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

        if (CommonUtil.isNullOrEmpty(args)) {
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
            sb.append(CommonUtil.toString(args[i++]));
            templateStart = placeholderStart + 2;
            placeholderStart = template.indexOf(placeholder, templateStart);
        }

        sb.append(template, templateStart, template.length());

        // if we run out of placeholders, append the extra args in square braces
        if (i < args.length) {
            sb.append(" [");
            sb.append(CommonUtil.toString(args[i++]));
            while (i < args.length) {
                sb.append(", ");
                sb.append(CommonUtil.toString(args[i++]));
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
        return a == null ? (b == null ? 0 : -1) : (b == null ? 1 : (cmp == null ? NATURAL_ORDER : cmp).compare(a, b));
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
        int res = CommonUtil.compare(a1, b1);

        return res == 0 ? CommonUtil.compare(a2, b2) : res;
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
    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> int compare(T1 a1, T1 b1, T2 a2, T2 b2, T3 a3, T3 b3) {
        int res = 0;

        if ((res = CommonUtil.compare(a1, b1)) != 0) {
            return res;
        } else if ((res = CommonUtil.compare(a2, b2)) != 0) {
            return res;
        }

        return CommonUtil.compare(a3, b3);
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
     */
    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> int compare(T1 a1, T1 b1, T2 a2,
            T2 b2, T3 a3, T3 b3, T4 a4, T4 b4) {
        int res = 0;

        if ((res = CommonUtil.compare(a1, b1)) != 0) {
            return res;
        } else if ((res = CommonUtil.compare(a2, b2)) != 0) {
            return res;
        } else if ((res = CommonUtil.compare(a3, b3)) != 0) {
            return res;
        }

        return CommonUtil.compare(a4, b4);
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
     */
    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> int compare(
            T1 a1, T1 b1, T2 a2, T2 b2, T3 a3, T3 b3, T4 a4, T4 b4, T5 a5, T5 b5) {
        int res = 0;

        if ((res = CommonUtil.compare(a1, b1)) != 0) {
            return res;
        } else if ((res = CommonUtil.compare(a2, b2)) != 0) {
            return res;
        } else if ((res = CommonUtil.compare(a3, b3)) != 0) {
            return res;
        } else if ((res = CommonUtil.compare(a4, b4)) != 0) {
            return res;
        }

        return CommonUtil.compare(a5, b5);
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
     */
    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>, T6 extends Comparable<T6>> int compare(
            T1 a1, T1 b1, T2 a2, T2 b2, T3 a3, T3 b3, T4 a4, T4 b4, T5 a5, T5 b5, T6 a6, T6 b6) {
        int res = 0;

        if ((res = CommonUtil.compare(a1, b1)) != 0) {
            return res;
        } else if ((res = CommonUtil.compare(a2, b2)) != 0) {
            return res;
        } else if ((res = CommonUtil.compare(a3, b3)) != 0) {
            return res;
        } else if ((res = CommonUtil.compare(a4, b4)) != 0) {
            return res;
        } else if ((res = CommonUtil.compare(a5, b5)) != 0) {
            return res;
        }

        return CommonUtil.compare(a6, b6);
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
     */
    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>, T6 extends Comparable<T6>, T7 extends Comparable<T7>> int compare(
            T1 a1, T1 b1, T2 a2, T2 b2, T3 a3, T3 b3, T4 a4, T4 b4, T5 a5, T5 b5, T6 a6, T6 b6, T7 a7, T7 b7) {
        int res = 0;

        if ((res = CommonUtil.compare(a1, b1)) != 0) {
            return res;
        } else if ((res = CommonUtil.compare(a2, b2)) != 0) {
            return res;
        } else if ((res = CommonUtil.compare(a3, b3)) != 0) {
            return res;
        } else if ((res = CommonUtil.compare(a4, b4)) != 0) {
            return res;
        } else if ((res = CommonUtil.compare(a5, b5)) != 0) {
            return res;
        } else if ((res = CommonUtil.compare(a6, b6)) != 0) {
            return res;
        }

        return CommonUtil.compare(a7, b7);
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static int compare(final boolean[] a, final boolean[] b) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.isNullOrEmpty(b) ? 0 : -1;
        } else if (CommonUtil.isNullOrEmpty(b)) {
            return 1;
        }

        for (int i = 0, minLen = N.min(a.length, b.length); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] ? 1 : -1;
            }
        }

        return a.length - b.length;
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
    public static int compare(final boolean[] a, final int fromIndexA, final boolean[] b, final int fromIndexB, final int len) {
        CommonUtil.checkArgNotNegative(len, "len");
        CommonUtil.checkFromIndexSize(fromIndexA, len, len(a));
        CommonUtil.checkFromIndexSize(fromIndexB, len, len(b));

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
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.isNullOrEmpty(b) ? 0 : -1;
        } else if (CommonUtil.isNullOrEmpty(b)) {
            return 1;
        }

        for (int i = 0, minLen = N.min(a.length, b.length); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return a.length - b.length;
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
    public static int compare(final char[] a, final int fromIndexA, final char[] b, final int fromIndexB, final int len) {
        CommonUtil.checkArgNotNegative(len, "len");
        CommonUtil.checkFromIndexSize(fromIndexA, len, len(a));
        CommonUtil.checkFromIndexSize(fromIndexB, len, len(b));

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
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.isNullOrEmpty(b) ? 0 : -1;
        } else if (CommonUtil.isNullOrEmpty(b)) {
            return 1;
        }

        for (int i = 0, minLen = N.min(a.length, b.length); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return a.length - b.length;
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
    public static int compare(final byte[] a, final int fromIndexA, final byte[] b, final int fromIndexB, final int len) {
        CommonUtil.checkArgNotNegative(len, "len");
        CommonUtil.checkFromIndexSize(fromIndexA, len, len(a));
        CommonUtil.checkFromIndexSize(fromIndexB, len, len(b));

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
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.isNullOrEmpty(b) ? 0 : -1;
        } else if (CommonUtil.isNullOrEmpty(b)) {
            return 1;
        }

        for (int i = 0, minLen = N.min(a.length, b.length); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return a.length - b.length;
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
    public static int compare(final short[] a, final int fromIndexA, final short[] b, final int fromIndexB, final int len) {
        CommonUtil.checkArgNotNegative(len, "len");
        CommonUtil.checkFromIndexSize(fromIndexA, len, len(a));
        CommonUtil.checkFromIndexSize(fromIndexB, len, len(b));

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
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.isNullOrEmpty(b) ? 0 : -1;
        } else if (CommonUtil.isNullOrEmpty(b)) {
            return 1;
        }

        for (int i = 0, minLen = N.min(a.length, b.length); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return a.length - b.length;
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
    public static int compare(final int[] a, final int fromIndexA, final int[] b, final int fromIndexB, final int len) {
        CommonUtil.checkArgNotNegative(len, "len");
        CommonUtil.checkFromIndexSize(fromIndexA, len, len(a));
        CommonUtil.checkFromIndexSize(fromIndexB, len, len(b));

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
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.isNullOrEmpty(b) ? 0 : -1;
        } else if (CommonUtil.isNullOrEmpty(b)) {
            return 1;
        }

        for (int i = 0, minLen = N.min(a.length, b.length); i < minLen; i++) {
            if (a[i] != b[i]) {
                return a[i] > b[i] ? 1 : -1;
            }
        }

        return a.length - b.length;
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
    public static int compare(final long[] a, final int fromIndexA, final long[] b, final int fromIndexB, final int len) {
        CommonUtil.checkArgNotNegative(len, "len");
        CommonUtil.checkFromIndexSize(fromIndexA, len, len(a));
        CommonUtil.checkFromIndexSize(fromIndexB, len, len(b));

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
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.isNullOrEmpty(b) ? 0 : -1;
        } else if (CommonUtil.isNullOrEmpty(b)) {
            return 1;
        }

        int value = 0;

        for (int i = 0, minLen = N.min(a.length, b.length); i < minLen; i++) {
            if ((value = Float.compare(a[i], b[i])) != 0) {
                return value;
            }
        }

        return a.length - b.length;
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
    public static int compare(final float[] a, final int fromIndexA, final float[] b, final int fromIndexB, final int len) {
        CommonUtil.checkArgNotNegative(len, "len");
        CommonUtil.checkFromIndexSize(fromIndexA, len, len(a));
        CommonUtil.checkFromIndexSize(fromIndexB, len, len(b));

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

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static int compare(final double[] a, final double[] b) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.isNullOrEmpty(b) ? 0 : -1;
        } else if (CommonUtil.isNullOrEmpty(b)) {
            return 1;
        }

        int value = 0;

        for (int i = 0, minLen = N.min(a.length, b.length); i < minLen; i++) {
            if ((value = Double.compare(a[i], b[i])) != 0) {
                return value;
            }
        }

        return a.length - b.length;
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
    public static int compare(final double[] a, final int fromIndexA, final double[] b, final int fromIndexB, final int len) {
        CommonUtil.checkArgNotNegative(len, "len");
        CommonUtil.checkFromIndexSize(fromIndexA, len, len(a));
        CommonUtil.checkFromIndexSize(fromIndexB, len, len(b));

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

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    public static <T extends Comparable<? super T>> int compare(final T[] a, final T[] b) {
        final Comparator<T> cmp = NATURAL_ORDER;

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
        final Comparator<T> cmp = NATURAL_ORDER;

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
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.isNullOrEmpty(b) ? 0 : -1;
        } else if (CommonUtil.isNullOrEmpty(b)) {
            return 1;
        }

        cmp = cmp == null ? NATURAL_ORDER : cmp;

        int value = 0;

        for (int i = 0, minLen = N.min(a.length, b.length); i < minLen; i++) {
            if ((value = cmp.compare(a[i], b[i])) != 0) {
                return value;
            }
        }

        return a.length - b.length;
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @param cmp
     * @return
     */
    public static <T> int compare(final T[] a, final int fromIndexA, final T[] b, final int fromIndexB, final int len, Comparator<? super T> cmp) {
        CommonUtil.checkArgNotNegative(len, "len");
        CommonUtil.checkFromIndexSize(fromIndexA, len, len(a));
        CommonUtil.checkFromIndexSize(fromIndexB, len, len(b));

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

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    public static <T extends Comparable<? super T>> int compare(final Collection<T> a, final Collection<T> b) {
        final Comparator<T> cmp = NATURAL_ORDER;

        return compare(a, b, cmp);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @param cmp
     * @return
     */
    public static <T> int compare(final Collection<T> a, final Collection<T> b, Comparator<? super T> cmp) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.isNullOrEmpty(b) ? 0 : -1;
        } else if (CommonUtil.isNullOrEmpty(b)) {
            return 1;
        }

        cmp = cmp == null ? NATURAL_ORDER : cmp;

        final Iterator<T> iterA = a.iterator();
        final Iterator<T> iterB = b.iterator();
        int value = 0;

        for (int i = 0, minLen = N.min(a.size(), b.size()); i < minLen; i++) {
            if ((value = cmp.compare(iterA.next(), iterB.next())) != 0) {
                return value;
            }
        }

        return a.size() - b.size();
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @param cmp
     * @return
     */
    public static <T> int compare(final Collection<T> a, int fromIndexA, final Collection<T> b, int fromIndexB, final int len, Comparator<? super T> cmp) {
        CommonUtil.checkArgNotNegative(len, "len");
        CommonUtil.checkFromIndexSize(fromIndexA, len, size(a));
        CommonUtil.checkFromIndexSize(fromIndexB, len, size(b));

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
     * @return true, if successful
     */
    public static boolean equals(final String a, final String b) {
        return (a == null) ? b == null : (b == null ? false : a.length() == b.length() && a.equals(b));
    }

    /**
     * Equals ignore case.
     *
     * @param a
     * @param b
     * @return true, if successful
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
        return a == b || (a != null && b != null && a.length == b.length && equals(a, 0, b, 0, a.length));
    }

    /**
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return true, if successful
     */
    public static boolean equals(final boolean[] a, final int fromIndexA, final boolean[] b, final int fromIndexB, final int len) {
        CommonUtil.checkArgNotNegative(len, "len");
        CommonUtil.checkFromIndexSize(fromIndexA, len, len(a));
        CommonUtil.checkFromIndexSize(fromIndexB, len, len(b));

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
        return a == b || (a != null && b != null && a.length == b.length && equals(a, 0, b, 0, a.length));
    }

    /**
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return true, if successful
     */
    public static boolean equals(final char[] a, final int fromIndexA, final char[] b, final int fromIndexB, final int len) {
        CommonUtil.checkArgNotNegative(len, "len");
        CommonUtil.checkFromIndexSize(fromIndexA, len, len(a));
        CommonUtil.checkFromIndexSize(fromIndexB, len, len(b));

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
        return a == b || (a != null && b != null && a.length == b.length && equals(a, 0, b, 0, a.length));
    }

    /**
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return true, if successful
     */
    public static boolean equals(final byte[] a, final int fromIndexA, final byte[] b, final int fromIndexB, final int len) {
        CommonUtil.checkArgNotNegative(len, "len");
        CommonUtil.checkFromIndexSize(fromIndexA, len, len(a));
        CommonUtil.checkFromIndexSize(fromIndexB, len, len(b));

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
        return a == b || (a != null && b != null && a.length == b.length && equals(a, 0, b, 0, a.length));
    }

    /**
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return true, if successful
     */
    public static boolean equals(final short[] a, final int fromIndexA, final short[] b, final int fromIndexB, final int len) {
        CommonUtil.checkArgNotNegative(len, "len");
        CommonUtil.checkFromIndexSize(fromIndexA, len, len(a));
        CommonUtil.checkFromIndexSize(fromIndexB, len, len(b));

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
        return a == b || (a != null && b != null && a.length == b.length && equals(a, 0, b, 0, a.length));
    }

    /**
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return true, if successful
     */
    public static boolean equals(final int[] a, final int fromIndexA, final int[] b, final int fromIndexB, final int len) {
        CommonUtil.checkArgNotNegative(len, "len");
        CommonUtil.checkFromIndexSize(fromIndexA, len, len(a));
        CommonUtil.checkFromIndexSize(fromIndexB, len, len(b));

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
        return a == b || (a != null && b != null && a.length == b.length && equals(a, 0, b, 0, a.length));
    }

    /**
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return true, if successful
     */
    public static boolean equals(final long[] a, final int fromIndexA, final long[] b, final int fromIndexB, final int len) {
        CommonUtil.checkArgNotNegative(len, "len");
        CommonUtil.checkFromIndexSize(fromIndexA, len, len(a));
        CommonUtil.checkFromIndexSize(fromIndexB, len, len(b));

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
        return a == b || (a != null && b != null && a.length == b.length && equals(a, 0, b, 0, a.length));
    }

    /**
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return true, if successful
     */
    public static boolean equals(final float[] a, final int fromIndexA, final float[] b, final int fromIndexB, final int len) {
        CommonUtil.checkArgNotNegative(len, "len");
        CommonUtil.checkFromIndexSize(fromIndexA, len, len(a));
        CommonUtil.checkFromIndexSize(fromIndexB, len, len(b));

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
        return a == b || (a != null && b != null && a.length == b.length && equals(a, 0, b, 0, a.length));
    }

    /**
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return true, if successful
     */
    public static boolean equals(final double[] a, final int fromIndexA, final double[] b, final int fromIndexB, final int len) {
        CommonUtil.checkArgNotNegative(len, "len");
        CommonUtil.checkFromIndexSize(fromIndexA, len, len(a));
        CommonUtil.checkFromIndexSize(fromIndexB, len, len(b));

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
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return true, if successful
     */
    public static boolean equals(final Object[] a, final int fromIndexA, final Object[] b, final int fromIndexB, final int len) {
        CommonUtil.checkArgNotNegative(len, "len");
        CommonUtil.checkFromIndexSize(fromIndexA, len, len(a));
        CommonUtil.checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return true;
        } else if (a.getClass().equals(b.getClass()) == false) {
            return false;
        }

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if (CommonUtil.equals(a[i], b[j]) == false) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param a
     * @param b
     * @return true, if successful
     * @see Arrays#deepEquals(Object[], Object[])
     */
    public static boolean deepEquals(final Object[] a, final Object[] b) {
        return a == b || (a != null && b != null && a.length == b.length && deepEquals(a, 0, b, 0, a.length));
    }

    /**
     *
     * @param a
     * @param fromIndexA
     * @param b
     * @param fromIndexB
     * @param len
     * @return true, if successful
     */
    public static boolean deepEquals(final Object[] a, final int fromIndexA, final Object[] b, final int fromIndexB, final int len) {
        CommonUtil.checkArgNotNegative(len, "len");
        CommonUtil.checkFromIndexSize(fromIndexA, len, len(a));
        CommonUtil.checkFromIndexSize(fromIndexB, len, len(b));

        if ((fromIndexA == fromIndexB && a == b) || len == 0) {
            return true;
        } else if (a.getClass().equals(b.getClass()) == false) {
            return false;
        }

        for (int i = fromIndexA, j = fromIndexB, k = 0; k < len; i++, j++, k++) {
            if (CommonUtil.deepEquals(a[i], b[j]) == false) {
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
     * @return true, if successful
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
     * @return true, if successful
     */
    public static boolean equalsIgnoreCase(final String[] a, final int fromIndexA, final String[] b, final int fromIndexB, final int len) {
        CommonUtil.checkArgNotNegative(len, "len");
        CommonUtil.checkFromIndexSize(fromIndexA, len, len(a));
        CommonUtil.checkFromIndexSize(fromIndexB, len, len(b));

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
                joiner.append(CommonUtil.toString(iter.next()));
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
     *
     * @param a
     * @return
     * @see Arrays#toString(boolean[])
     */
    public static String toString(final boolean[] a) {
        if (a == null) {
            return CommonUtil.NULL_STRING;
        }

        if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static String toString(final boolean[] a, final int from, final int to) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, from, to);

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
            sb.append(NULL_STRING);
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
     * @param from
     * @param to
     */
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
     *
     * @param a
     * @return
     * @see Arrays#toString(char[])
     */
    public static String toString(final char[] a) {
        if (a == null) {
            return CommonUtil.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static String toString(final char[] a, final int from, final int to) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, from, to);

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
            sb.append(NULL_STRING);
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
     * @param from
     * @param to
     */
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
     *
     * @param a
     * @return
     * @see Arrays#toString(byte[])
     */
    public static String toString(final byte[] a) {
        if (a == null) {
            return CommonUtil.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static String toString(final byte[] a, final int from, final int to) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, from, to);

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
            sb.append(NULL_STRING);
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
     * @param from
     * @param to
     */
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
     *
     * @param a
     * @return
     * @see Arrays#toString(short[])
     */
    public static String toString(final short[] a) {
        if (a == null) {
            return CommonUtil.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static String toString(final short[] a, final int from, final int to) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, from, to);

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
            sb.append(NULL_STRING);
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
     * @param from
     * @param to
     */
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
     *
     * @param a
     * @return
     * @see Arrays#toString(int[])
     */
    public static String toString(final int[] a) {
        if (a == null) {
            return CommonUtil.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static String toString(final int[] a, final int from, final int to) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, from, to);

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
            sb.append(NULL_STRING);
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
     * @param from
     * @param to
     */
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
     *
     * @param a
     * @return
     * @see Arrays#toString(long[])
     */
    public static String toString(final long[] a) {
        if (a == null) {
            return CommonUtil.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static String toString(final long[] a, final int from, final int to) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, from, to);

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
            sb.append(NULL_STRING);
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
     * @param from
     * @param to
     */
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
     *
     * @param a
     * @return
     * @see Arrays#toString(float[])
     */
    public static String toString(final float[] a) {
        if (a == null) {
            return CommonUtil.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static String toString(final float[] a, final int from, final int to) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, from, to);

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
            sb.append(NULL_STRING);
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
     * @param from
     * @param to
     */
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
     *
     * @param a
     * @return
     * @see Arrays#toString(double[])
     */
    public static String toString(final double[] a) {
        if (a == null) {
            return CommonUtil.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static String toString(final double[] a, final int from, final int to) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, from, to);

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
            sb.append(NULL_STRING);
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
     * @param from
     * @param to
     */
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
     *
     * @param a
     * @return
     * @see Arrays#toString(Object[])
     */
    public static String toString(final Object[] a) {
        if (a == null) {
            return CommonUtil.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return toString(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static String toString(final Object[] a, final int from, final int to) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            toString(sb, a, from, to);

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
            sb.append(NULL_STRING);
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
     * @param from
     * @param to
     */
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
     * Deep to string.
     *
     * @param a
     * @return
     * @see Arrays#deepToString(Object[])
     */
    public static String deepToString(final Object[] a) {
        if (a == null) {
            return CommonUtil.NULL_STRING;
        } else if (a.length == 0) {
            return "[]";
        }

        return deepToString(a, 0, a.length);
    }

    /**
     * Deep to string.
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
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
     * @param from
     * @param to
     * @param processedElements
     */
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
                sb.append(CommonUtil.NULL_CHAR_ARRAY);

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
        if (CommonUtil.isNullOrEmpty(a)) {
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

        if (CommonUtil.isNullOrEmpty(a) || a.length == 1) {
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
        if (CommonUtil.isNullOrEmpty(a)) {
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

        if (CommonUtil.isNullOrEmpty(a) || a.length == 1) {
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
        if (CommonUtil.isNullOrEmpty(a)) {
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

        if (CommonUtil.isNullOrEmpty(a) || a.length == 1) {
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
        if (CommonUtil.isNullOrEmpty(a)) {
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

        if (CommonUtil.isNullOrEmpty(a) || a.length == 1) {
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
        if (CommonUtil.isNullOrEmpty(a)) {
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

        if (CommonUtil.isNullOrEmpty(a) || a.length == 1) {
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
        if (CommonUtil.isNullOrEmpty(a)) {
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

        if (CommonUtil.isNullOrEmpty(a) || a.length == 1) {
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
        if (CommonUtil.isNullOrEmpty(a)) {
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

        if (CommonUtil.isNullOrEmpty(a) || a.length == 1) {
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
        if (CommonUtil.isNullOrEmpty(a)) {
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

        if (CommonUtil.isNullOrEmpty(a) || a.length == 1) {
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
        if (CommonUtil.isNullOrEmpty(a)) {
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

        if (CommonUtil.isNullOrEmpty(a) || a.length == 1) {
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
        if (CommonUtil.isNullOrEmpty(list)) {
            return;
        }

        reverse(list, 0, list.size());
    }

    /**
     *
     * @param list
     * @param fromIndex
     * @param toIndex
     */
    public static void reverse(final List<?> list, int fromIndex, int toIndex) {
        checkFromToIndex(fromIndex, toIndex, size(list));

        if (CommonUtil.isNullOrEmpty(list) || list.size() == 1) {
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
        if (CommonUtil.isNullOrEmpty(c) || c.size() < 2) {
            return;
        }

        if (c instanceof List) {
            CommonUtil.reverse((List) c);
        } else {
            final Object[] tmp = c.toArray();
            CommonUtil.reverse(tmp);
            c.clear();
            c.addAll((List) Arrays.asList(tmp));
        }
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
        if (CommonUtil.isNullOrEmpty(c) || c.size() < 2) {
            return;
        }

        if (c instanceof List) {
            CommonUtil.rotate((List) c, distance);
        } else {
            final Object[] tmp = c.toArray();
            CommonUtil.rotate(tmp, distance);
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
        if (CommonUtil.isNullOrEmpty(a) || a.length == 1) {
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
        if (CommonUtil.isNullOrEmpty(a) || a.length == 1) {
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
        if (CommonUtil.isNullOrEmpty(a) || a.length == 1) {
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
        if (CommonUtil.isNullOrEmpty(a) || a.length == 1) {
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
        if (CommonUtil.isNullOrEmpty(a) || a.length == 1) {
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
        if (CommonUtil.isNullOrEmpty(a) || a.length == 1) {
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
        if (CommonUtil.isNullOrEmpty(a) || a.length == 1) {
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
        if (CommonUtil.isNullOrEmpty(a) || a.length == 1) {
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
        if (CommonUtil.isNullOrEmpty(a) || a.length == 1) {
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
        if (CommonUtil.isNullOrEmpty(list) || list.size() == 1) {
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
        if (CommonUtil.isNullOrEmpty(c) || c.size() < 2) {
            return;
        }

        if (c instanceof List) {
            CommonUtil.shuffle((List) c);
        } else {
            final Object[] tmp = c.toArray();
            CommonUtil.shuffle(tmp);
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
        if (CommonUtil.isNullOrEmpty(c) || c.size() < 2) {
            return;
        }

        if (c instanceof List) {
            CommonUtil.shuffle((List) c, rnd);
        } else {
            final Object[] tmp = c.toArray();
            CommonUtil.shuffle(tmp, rnd);
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
     * @return true, if successful
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
     * @return true, if successful
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
     * @param <T>
     * @param entityClass entity class with getter/setter methods
     * @return
     */
    public static <T> T fill(Class<T> entityClass) {
        return TestUtil.fill(entityClass);
    }

    /**
     * Fill the properties of the entity with random values.
     *
     * @param <T>
     * @param entityClass entity class with getter/setter methods
     * @param count
     * @return
     */
    public static <T> List<T> fill(Class<T> entityClass, int count) {
        return TestUtil.fill(entityClass, count);
    }

    /**
     *
     * @param <T>
     * @param value
     * @param n
     * @return
     */
    public static <T> List<T> repeat(final T value, final int n) {
        CommonUtil.checkArgNotNegative(n, "n");

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
     * @param <T>
     * @param c
     * @param n
     * @return
     */
    public static <T> List<T> repeatEach(final Collection<T> c, final int n) {
        CommonUtil.checkArgNotNegative(n, "n");

        if (n == 0 || isNullOrEmpty(c)) {
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
     * <pre>
     * <code>
     * Seq.repeat(N.asList(1, 2, 3), 2) => [1, 2, 3, 1, 2, 3]
     * </code>
     * </pre>
     *
     * @param <T>
     * @param c
     * @param n
     * @return
     */
    public static <T> List<T> repeatAll(final Collection<T> c, final int n) {
        CommonUtil.checkArgNotNegative(n, "n");

        if (n == 0 || isNullOrEmpty(c)) {
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
     * Seq.nRepeatToSize(N.asList(1, 2, 3), 5) => [1, 1, 2, 2, 3]
     * </code>
     * </pre>
     *
     * @param <T>
     * @param c
     * @param size
     * @return
     */
    public static <T> List<T> repeatEachToSize(final Collection<T> c, final int size) {
        CommonUtil.checkArgNotNegative(size, "size");
        checkArgument(size == 0 || notNullOrEmpty(c), "Collection can not be empty or null when size > 0");

        if (size == 0 || isNullOrEmpty(c)) {
            return new ArrayList<>();
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
     * <pre>
     * <code>
     * Seq.repeatToSize(N.asList(1, 2, 3), 5) => [1, 2, 3, 1, 2]
     * </code>
     * </pre>
     *
     * @param <T>
     * @param c
     * @param size
     * @return
     */
    public static <T> List<T> repeatAllToSize(final Collection<T> c, final int size) {
        CommonUtil.checkArgNotNegative(size, "size");
        checkArgument(size == 0 || notNullOrEmpty(c), "Collection can not be empty or null when size > 0");

        if (size == 0 || isNullOrEmpty(c)) {
            return new ArrayList<>();
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
        if (src.size() > dest.size()) {
            throw new IllegalArgumentException("Source does not fit in dest");
        }

        Collections.copy(dest, src);
    }

    /**
     *
     * @param <T>
     * @param src
     * @param srcPos
     * @param dest
     * @param destPos
     * @param length
     */
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

    /**
     *
     * @param src
     * @param srcPos
     * @param dest
     * @param destPos
     * @param length
     */
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

    /**
     *
     * @param src
     * @param srcPos
     * @param dest
     * @param destPos
     * @param length
     */
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

    /**
     *
     * @param src
     * @param srcPos
     * @param dest
     * @param destPos
     * @param length
     */
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

    /**
     *
     * @param src
     * @param srcPos
     * @param dest
     * @param destPos
     * @param length
     */
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

    /**
     *
     * @param src
     * @param srcPos
     * @param dest
     * @param destPos
     * @param length
     */
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

    /**
     *
     * @param src
     * @param srcPos
     * @param dest
     * @param destPos
     * @param length
     */
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

    /**
     *
     * @param src
     * @param srcPos
     * @param dest
     * @param destPos
     * @param length
     */
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

    /**
     *
     * @param src
     * @param srcPos
     * @param dest
     * @param destPos
     * @param length
     */
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

    /**
     *
     * @param src
     * @param srcPos
     * @param dest
     * @param destPos
     * @param length
     */
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
     *
     * @param src
     * @param srcPos
     * @param dest
     * @param destPos
     * @param length
     * @see System#arraycopy(Object, int, Object, int, int) is called
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

        if (CommonUtil.notNullOrEmpty(original)) {
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

        if (CommonUtil.notNullOrEmpty(original)) {
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

        if (CommonUtil.notNullOrEmpty(original)) {
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

        if (CommonUtil.notNullOrEmpty(original)) {
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

        if (CommonUtil.notNullOrEmpty(original)) {
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

        if (CommonUtil.notNullOrEmpty(original)) {
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

        if (CommonUtil.notNullOrEmpty(original)) {
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

        if (CommonUtil.notNullOrEmpty(original)) {
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
        final T[] copy = Object[].class.equals(newType) ? (T[]) new Object[newLength] : (T[]) CommonUtil.newArray(newType.getComponentType(), newLength);

        if (CommonUtil.notNullOrEmpty(original)) {
            copy(original, 0, copy, 0, Math.min(original.length, newLength));
        }

        return copy;
    }

    /**
     * Copy of range.
     *
     * @param original
     * @param from
     * @param to
     * @return
     * @see Arrays#copyOfRange(boolean[], int, int)
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
     * @see CommonUtil#copyOfRange(int[], int, int, int)
     */
    public static boolean[] copyOfRange(final boolean[] original, int from, final int to, final int step) {
        CommonUtil.checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (from == to || from < to != step > 0) {
            return CommonUtil.EMPTY_BOOLEAN_ARRAY;
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
     * Copy of range.
     *
     * @param original
     * @param from
     * @param to
     * @return
     * @see Arrays#copyOfRange(char[], int, int)
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
     * @see CommonUtil#copyOfRange(int[], int, int, int)
     */
    public static char[] copyOfRange(final char[] original, int from, final int to, final int step) {
        CommonUtil.checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (from == to || from < to != step > 0) {
            return CommonUtil.EMPTY_CHAR_ARRAY;
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
     * Copy of range.
     *
     * @param original
     * @param from
     * @param to
     * @return
     * @see Arrays#copyOfRange(byte[], int, int)
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
     * @see CommonUtil#copyOfRange(int[], int, int, int)
     */
    public static byte[] copyOfRange(final byte[] original, int from, final int to, final int step) {
        CommonUtil.checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (from == to || from < to != step > 0) {
            return CommonUtil.EMPTY_BYTE_ARRAY;
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
     * Copy of range.
     *
     * @param original
     * @param from
     * @param to
     * @return
     * @see Arrays#copyOfRange(short[], int, int)
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
     * @see CommonUtil#copyOfRange(int[], int, int, int)
     */
    public static short[] copyOfRange(final short[] original, int from, final int to, final int step) {
        CommonUtil.checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (from == to || from < to != step > 0) {
            return CommonUtil.EMPTY_SHORT_ARRAY;
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
     * Copy of range.
     *
     * @param original
     * @param from
     * @param to
     * @return
     * @see Arrays#copyOfRange(int[], int, int)
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
     *
     * @param original
     * @param from
     * @param to
     * @param step
     * @return
     */
    public static int[] copyOfRange(final int[] original, int from, final int to, final int step) {
        CommonUtil.checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (from == to || from < to != step > 0) {
            return CommonUtil.EMPTY_INT_ARRAY;
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
     * Copy of range.
     *
     * @param original
     * @param from
     * @param to
     * @return
     * @see Arrays#copyOfRange(long[], int, int)
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
     * @see CommonUtil#copyOfRange(int[], int, int, int)
     */
    public static long[] copyOfRange(final long[] original, int from, final int to, final int step) {
        CommonUtil.checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (from == to || from < to != step > 0) {
            return CommonUtil.EMPTY_LONG_ARRAY;
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
     * Copy of range.
     *
     * @param original
     * @param from
     * @param to
     * @return
     * @see Arrays#copyOfRange(float[], int, int)
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
     * @see CommonUtil#copyOfRange(int[], int, int, int)
     */
    public static float[] copyOfRange(final float[] original, int from, final int to, final int step) {
        CommonUtil.checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (from == to || from < to != step > 0) {
            return CommonUtil.EMPTY_FLOAT_ARRAY;
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
     * Copy of range.
     *
     * @param original
     * @param from
     * @param to
     * @return
     * @see Arrays#copyOfRange(double[], int, int)
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
     * @see CommonUtil#copyOfRange(int[], int, int, int)
     */
    public static double[] copyOfRange(final double[] original, int from, final int to, final int step) {
        CommonUtil.checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (from == to || from < to != step > 0) {
            return CommonUtil.EMPTY_DOUBLE_ARRAY;
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
     * Copy of range.
     *
     * @param <T>
     * @param original
     * @param from
     * @param to
     * @return
     * @see Arrays#copyOfRange(T[], int, int)
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
     * @param <T>
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
     * {@link Arrays#copyOfRange(Object[], int, int, Class)}.
     *
     * @param <T>
     * @param <U>
     * @param original
     * @param from
     * @param to
     * @param newType
     * @return
     */
    public static <T, U> T[] copyOfRange(final U[] original, final int from, final int to, final Class<? extends T[]> newType) {
        final int newLength = to - from;
        final T[] copy = Object[].class.equals(newType) ? (T[]) new Object[newLength] : (T[]) CommonUtil.newArray(newType.getComponentType(), newLength);
        copy(original, from, copy, 0, Math.min(original.length - from, newLength));
        return copy;
    }

    /**
     * Copy all the elements in <code>original</code>, through <code>to</code>-<code>from</code>, by <code>step</code>.
     *
     * @param <T>
     * @param original
     * @param from
     * @param to
     * @param step
     * @param newType
     * @return
     * @see CommonUtil#copyOfRange(int[], int, int, int)
     */
    public static <T> T[] copyOfRange(final T[] original, int from, final int to, final int step, final Class<? extends T[]> newType) {
        CommonUtil.checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from, original.length);

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (from == to || from < to != step > 0) {
            return Object[].class.equals(newType) ? (T[]) new Object[0] : (T[]) CommonUtil.newArray(newType.getComponentType(), 0);
        }

        if (step == 1) {
            return copyOfRange(original, from, to);
        }

        from = from > to ? N.min(original.length - 1, from) : from;
        final int len = (to - from) / step + ((to - from) % step == 0 ? 0 : 1);
        final T[] copy = Object[].class.equals(newType) ? (T[]) new Object[len] : (T[]) CommonUtil.newArray(newType.getComponentType(), len);

        for (int i = 0, j = from; i < len; i++, j += step) {
            copy[i] = original[j];
        }

        return copy;
    }

    /**
     * Copy of range.
     *
     * @param <T>
     * @param c
     * @param from
     * @param to
     * @return
     * @see Arrays#copyOfRange(T[], int, int)
     */
    public static <T> List<T> copyOfRange(final List<T> c, final int from, final int to) {
        CommonUtil.checkFromToIndex(from, to, c.size());

        final List<T> result = new ArrayList<>(to - from);
        result.addAll(c.subList(from, to));
        return result;
    }

    /**
     * Copy all the elements in <code>original</code>, through <code>to</code>-<code>from</code>, by <code>step</code>.
     *
     * @param <T>
     * @param c
     * @param from
     * @param to
     * @param step
     * @return
     */
    public static <T> List<T> copyOfRange(final List<T> c, int from, final int to, final int step) {
        CommonUtil.checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from, c.size());

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
            result = createList(CommonUtil.copyOfRange(a, 0, a.length, step));
        }

        return result;
    }

    /**
     * Create an array list by initializing its elements data with the specified array <code>a</code>.
     * The returned list may share the same elements with the specified array <code>a</code>.
     * That's to say any change on the List/Array will affect the Array/List.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    static <T> List<T> createList(final T... a) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        if (CommonUtil.isListElementDataFieldSettable && CommonUtil.listElementDataField != null && CommonUtil.listSizeField != null) {
            final List<T> list = new ArrayList<>();

            try {
                CommonUtil.listElementDataField.set(list, a);
                CommonUtil.listSizeField.set(list, a.length);

                return list;
            } catch (Throwable e) {
                // ignore;
                CommonUtil.isListElementDataFieldSettable = false;
            }
        }

        return CommonUtil.asList(a);
    }

    /**
     * Copy of range.
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
     * Copy of range.
     *
     * @param str
     * @param from
     * @param to
     * @param step
     * @return
     * @see CommonUtil#copyOfRange(int[], int, int, int)
     */
    @SuppressWarnings("deprecation")
    public static String copyOfRange(final String str, int from, final int to, final int step) {
        CommonUtil.checkFromToIndex(from < to ? from : (to == -1 ? 0 : to), from < to ? to : from, str.length());

        if (step == 0) {
            throw new IllegalArgumentException("The input parameter 'by' can not be zero");
        }

        if (from == to || from < to != step > 0) {
            return CommonUtil.EMPTY_STRING;
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
     * @param <T>
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
     * @param <T>
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
     * @param <T>
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

    /**
     *
     * @param <T>
     * @param newType
     * @param a
     * @return
     */
    public static <T> T[] copy(Class<T[]> newType, Object[] a) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.newArray(newType.getComponentType(), 0);
        }

        return CommonUtil.copyOf(a, a.length, newType);
    }

    /**
     *
     * @param <T>
     * @param newType
     * @param a
     * @return
     */
    public static <T> T[][] copy(Class<T[][]> newType, Object[][] a) {
        final Class<T[]> componentType = (Class<T[]>) newType.getComponentType();

        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.newArray(componentType, 0);
        }

        final int len = CommonUtil.len(a);
        final T[][] result = CommonUtil.newArray(componentType, len);

        for (int i = 0; i < len; i++) {
            result[i] = copy(componentType, a[i]);
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param newType
     * @param a
     * @return
     */
    public static <T> T[][][] copy(Class<T[][][]> newType, Object[][][] a) {
        final Class<T[][]> componentType = (Class<T[][]>) newType.getComponentType();

        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.newArray(componentType, 0);
        }

        final int len = CommonUtil.len(a);
        final T[][][] result = CommonUtil.newArray(componentType, len);

        for (int i = 0; i < len; i++) {
            result[i] = copy(componentType, a[i]);
        }

        return result;
    }

    /**
     *
     * @param a
     */
    public static void sort(final boolean[] a) {
        Array.sort(a);
    }

    /**
     *
     * @param a
     */
    public static void sort(final char[] a) {
        Array.sort(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void sort(final char[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void sort(final byte[] a) {
        Array.sort(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void sort(final byte[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void sort(final short[] a) {
        Array.sort(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void sort(final short[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void sort(final int[] a) {
        Array.sort(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void sort(final int[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void sort(final long[] a) {
        Array.sort(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void sort(final long[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void sort(final float[] a) {
        Array.sort(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void sort(final float[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void sort(final double[] a) {
        Array.sort(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void sort(final double[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void sort(final Object[] a) {
        Array.sort(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void sort(final Object[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param cmp
     */
    public static <T> void sort(final T[] a, final Comparator<? super T> cmp) {
        Array.sort(a, cmp);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param cmp
     */
    public static <T> void sort(final T[] a, final int fromIndex, final int toIndex, final Comparator<? super T> cmp) {
        Array.sort(a, fromIndex, toIndex, cmp);
    }

    /**
     *
     * @param <T>
     * @param c
     */
    public static <T extends Comparable<? super T>> void sort(final List<? extends T> c) {
        Array.sort(c);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     */
    public static <T extends Comparable<? super T>> void sort(final List<? extends T> c, final int fromIndex, final int toIndex) {
        Array.sort(c, fromIndex, toIndex);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param cmp
     */
    public static <T> void sort(final List<? extends T> c, final Comparator<? super T> cmp) {
        Array.sort(c, cmp);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param cmp
     */
    public static <T> void sort(final List<? extends T> c, final int fromIndex, final int toIndex, final Comparator<? super T> cmp) {
        Array.sort(c, fromIndex, toIndex, cmp);
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
     * @param c
     * @param keyMapper
     */
    public static <T, U extends Comparable<? super U>> void sortBy(final List<? extends T> c, final Function<? super T, ? extends U> keyMapper) {
        sort(c, Comparators.comparingBy(keyMapper));
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
     * @param c
     * @param keyMapper
     */
    public static <T> void sortByInt(final List<? extends T> c, final ToIntFunction<? super T> keyMapper) {
        sort(c, Comparators.comparingInt(keyMapper));
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
     * @param c
     * @param keyMapper
     */
    public static <T> void sortByLong(final List<? extends T> c, final ToLongFunction<? super T> keyMapper) {
        sort(c, Comparators.comparingLong(keyMapper));
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
     * @param c
     * @param keyMapper
     */
    public static <T> void sortByFloat(final List<? extends T> c, final ToFloatFunction<? super T> keyMapper) {
        sort(c, Comparators.comparingFloat(keyMapper));
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
     * @param c
     * @param keyMapper
     */
    public static <T> void sortByDouble(final List<? extends T> c, final ToDoubleFunction<? super T> keyMapper) {
        sort(c, Comparators.comparingDouble(keyMapper));
    }

    /**
     *
     * @param a
     */
    public static void parallelSort(final char[] a) {
        Array.parallelSort(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void parallelSort(final char[] a, final int fromIndex, final int toIndex) {
        Array.parallelSort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void parallelSort(final byte[] a) {
        Array.parallelSort(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void parallelSort(final byte[] a, final int fromIndex, final int toIndex) {
        Array.parallelSort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void parallelSort(final short[] a) {
        Array.parallelSort(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void parallelSort(final short[] a, final int fromIndex, final int toIndex) {
        Array.parallelSort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void parallelSort(final int[] a) {
        Array.parallelSort(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void parallelSort(final int[] a, final int fromIndex, final int toIndex) {
        Array.parallelSort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void parallelSort(final long[] a) {
        Array.parallelSort(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void parallelSort(final long[] a, final int fromIndex, final int toIndex) {
        Array.parallelSort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void parallelSort(final float[] a) {
        Array.parallelSort(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void parallelSort(final float[] a, final int fromIndex, final int toIndex) {
        Array.parallelSort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void parallelSort(final double[] a) {
        Array.parallelSort(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void parallelSort(final double[] a, final int fromIndex, final int toIndex) {
        Array.parallelSort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void parallelSort(final Object[] a) {
        Array.parallelSort(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void parallelSort(final Object[] a, final int fromIndex, final int toIndex) {
        Array.parallelSort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param cmp
     */
    public static <T> void parallelSort(final T[] a, final Comparator<? super T> cmp) {
        Array.parallelSort(a, cmp);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param cmp
     */
    public static <T> void parallelSort(final T[] a, final int fromIndex, final int toIndex, final Comparator<? super T> cmp) {
        Array.parallelSort(a, fromIndex, toIndex, cmp);
    }

    /**
     *
     * @param <T>
     * @param c
     */
    public static <T extends Comparable<? super T>> void parallelSort(final List<? extends T> c) {
        Array.parallelSort(c);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     */
    public static <T extends Comparable<? super T>> void parallelSort(final List<? extends T> c, final int fromIndex, final int toIndex) {
        Array.parallelSort(c, fromIndex, toIndex);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param cmp
     */
    public static <T> void parallelSort(final List<? extends T> c, final Comparator<? super T> cmp) {
        Array.parallelSort(c, cmp);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param cmp
     */
    public static <T> void parallelSort(final List<? extends T> c, final int fromIndex, final int toIndex, final Comparator<? super T> cmp) {
        Array.parallelSort(c, fromIndex, toIndex, cmp);
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
     * @param c
     * @param keyMapper
     */
    public static <T, U extends Comparable<? super U>> void parallelSortBy(final List<? extends T> c, final Function<? super T, ? extends U> keyMapper) {
        parallelSort(c, Comparators.comparingBy(keyMapper));
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
     * @param c
     * @param keyMapper
     */
    public static <T> void parallelSortByInt(final List<? extends T> c, final ToIntFunction<? super T> keyMapper) {
        parallelSort(c, Comparators.comparingInt(keyMapper));
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
     * @param c
     * @param keyMapper
     */
    public static <T> void parallelSortByLong(final List<? extends T> c, final ToLongFunction<? super T> keyMapper) {
        parallelSort(c, Comparators.comparingLong(keyMapper));
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
     * @param c
     * @param keyMapper
     */
    public static <T> void parallelSortByFloat(final List<? extends T> c, final ToFloatFunction<? super T> keyMapper) {
        parallelSort(c, Comparators.comparingFloat(keyMapper));
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
     * @param c
     * @param keyMapper
     */
    public static <T> void parallelSortByDouble(final List<? extends T> c, final ToDoubleFunction<? super T> keyMapper) {
        parallelSort(c, Comparators.comparingDouble(keyMapper));
    }

    /**
     *
     * @param a
     */
    public static void reverseSort(final boolean[] a) {
        Array.reverseSort(a);
    }

    /**
     *
     * @param a
     */
    public static void reverseSort(final char[] a) {
        Array.sort(a);
        reverse(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void reverseSort(final char[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
        reverse(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void reverseSort(final byte[] a) {
        Array.sort(a);
        reverse(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void reverseSort(final byte[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
        reverse(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void reverseSort(final short[] a) {
        Array.sort(a);
        reverse(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void reverseSort(final short[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
        reverse(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void reverseSort(final int[] a) {
        Array.sort(a);
        reverse(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void reverseSort(final int[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
        reverse(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void reverseSort(final long[] a) {
        Array.sort(a);
        reverse(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void reverseSort(final long[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
        reverse(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void reverseSort(final float[] a) {
        Array.sort(a);
        reverse(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void reverseSort(final float[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
        reverse(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void reverseSort(final double[] a) {
        Array.sort(a);
        reverse(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void reverseSort(final double[] a, final int fromIndex, final int toIndex) {
        Array.sort(a, fromIndex, toIndex);
        reverse(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void reverseSort(final Object[] a) {
        sort(a, Fn.reversedOrder());
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void reverseSort(final Object[] a, final int fromIndex, final int toIndex) {
        sort(a, fromIndex, toIndex, Fn.reversedOrder());
    }

    /**
     *
     * @param <T>
     * @param c
     */
    public static <T extends Comparable<? super T>> void reverseSort(final List<? extends T> c) {
        sort(c, Fn.reversedOrder());
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     */
    public static <T extends Comparable<? super T>> void reverseSort(final List<? extends T> c, final int fromIndex, final int toIndex) {
        sort(c, fromIndex, toIndex, Fn.reversedOrder());
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
     * @param c
     * @param keyMapper
     */
    public static <T, U extends Comparable<? super U>> void reverseSortBy(final List<? extends T> c, final Function<? super T, ? extends U> keyMapper) {
        sort(c, Comparators.reversedComparingBy(keyMapper));
    }

    /**
     *
     * @param a
     */
    public static void bucketSort(final int[] a) {
        Array.bucketSort(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void bucketSort(final int[] a, final int fromIndex, final int toIndex) {
        Array.bucketSort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void bucketSort(final long[] a) {
        Array.bucketSort(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void bucketSort(final long[] a, final int fromIndex, final int toIndex) {
        Array.bucketSort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void bucketSort(final float[] a) {
        Array.bucketSort(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    public static void bucketSort(final float[] a, final int fromIndex, final int toIndex) {
        Array.bucketSort(a, fromIndex, toIndex);
    }

    /**
     *
     * @param a
     */
    public static void bucketSort(final double[] a) {
        Array.bucketSort(a);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
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

    /**
     *
     * @param <T>
     * @param a
     * @param cmp
     */
    public static <T> void bucketSort(final T[] a, final Comparator<? super T> cmp) {
        Array.bucketSort(a, cmp);
    }

    /**
     * Note: All the objects with same value will be replaced with first element with the same value.
     *
     * @param <T>
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
     * @param <T>
     * @param c
     */
    public static <T extends Comparable<? super T>> void bucketSort(final List<T> c) {
        Array.bucketSort(c);
    }

    /**
     * Note: All the objects with same value will be replaced with first element with the same value.
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     */
    public static <T extends Comparable<? super T>> void bucketSort(final List<T> c, final int fromIndex, final int toIndex) {
        Array.bucketSort(c, fromIndex, toIndex);
    }

    /**
     * Note: All the objects with same value will be replaced with first element with the same value.
     *
     * @param <T>
     * @param c
     * @param cmp
     */
    public static <T> void bucketSort(final List<? extends T> c, final Comparator<? super T> cmp) {
        Array.bucketSort(c, cmp);
    }

    /**
     * Note: All the objects with same value will be replaced with first element with the same value.
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param cmp
     */
    public static <T> void bucketSort(final List<? extends T> c, final int fromIndex, final int toIndex, final Comparator<? super T> cmp) {
        Array.bucketSort(c, fromIndex, toIndex, cmp);
    }

    /**
     * Bucket sort by.
     *
     * @param <T>
     * @param <U>
     * @param a
     * @param keyMapper
     */
    public static <T, U extends Comparable<? super U>> void bucketSortBy(final T[] a, final Function<? super T, ? extends U> keyMapper) {
        bucketSort(a, Comparators.comparingBy(keyMapper));
    }

    /**
     * Bucket sort by.
     *
     * @param <T>
     * @param <U>
     * @param c
     * @param keyMapper
     */
    public static <T, U extends Comparable<? super U>> void bucketSortBy(final List<? extends T> c, final Function<? super T, ? extends U> keyMapper) {
        bucketSort(c, Comparators.comparingBy(keyMapper));
    }

    /**
     * {@link Arrays#binarySearch(boolean[], boolean)}.
     *
     * @param a
     * @param key
     * @return
     */
    static int binarySearch(final boolean[] a, final boolean key) {
        return Array.binarySearch(a, key);
    }

    /**
     * {@link Arrays#binarySearch(char[], char)}.
     *
     * @param a
     * @param key
     * @return
     */
    public static int binarySearch(final char[] a, final char key) {
        return Array.binarySearch(a, key);
    }

    /**
     * {@link Arrays#binarySearch(char[], int, int, char)}.
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
     * {@link Arrays#binarySearch(byte[], byte)}.
     *
     * @param a
     * @param key
     * @return
     */
    public static int binarySearch(final byte[] a, final byte key) {
        return Array.binarySearch(a, key);
    }

    /**
     * {@link Arrays#binarySearch(byte[], int, int, byte)}.
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
     * {@link Arrays#binarySearch(short[], short)}.
     *
     * @param a
     * @param key
     * @return
     */
    public static int binarySearch(final short[] a, final short key) {
        return Array.binarySearch(a, key);
    }

    /**
     * {@link Arrays#binarySearch(short[], int, int, short)}.
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
     * {@link Arrays#binarySearch(int[], int)}.
     *
     * @param a
     * @param key
     * @return
     */
    public static int binarySearch(final int[] a, final int key) {
        return Array.binarySearch(a, key);
    }

    /**
     * {@link Arrays#binarySearch(int[], int, int, int)}.
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
     * {@link Arrays#binarySearch(long[], long)}.
     *
     * @param a
     * @param key
     * @return
     */
    public static int binarySearch(final long[] a, final long key) {
        return Array.binarySearch(a, key);
    }

    /**
     * {@link Arrays#binarySearch(long[], int, int, long)}.
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
     * {@link Arrays#binarySearch(float[], float)}.
     *
     * @param a
     * @param key
     * @return
     */
    public static int binarySearch(final float[] a, final float key) {
        return Array.binarySearch(a, key);
    }

    /**
     * {@link Arrays#binarySearch(float[], int, int, float)}.
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
     * {@link Arrays#binarySearch(double[], double)}.
     *
     * @param a
     * @param key
     * @return
     */
    public static int binarySearch(final double[] a, final double key) {
        return Array.binarySearch(a, key);
    }

    /**
     * {@link Arrays#binarySearch(double[], int, int, double)}.
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
     * {@link Arrays#binarySearch(Object[], Object)}.
     *
     * @param a
     * @param key
     * @return
     */
    public static int binarySearch(final Object[] a, final Object key) {
        return Array.binarySearch(a, key);
    }

    /**
     * {@link Arrays#binarySearch(Object[], int, int, Object)}.
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
     * {@link Arrays#binarySearch(Object[], Object, Comparator)}.
     *
     * @param <T>
     * @param a
     * @param key
     * @param cmp
     * @return
     */
    public static <T> int binarySearch(final T[] a, final T key, final Comparator<? super T> cmp) {
        return Array.binarySearch(a, key, cmp);
    }

    /**
     * {@link Arrays#binarySearch(Object[], int, int, Object, Comparator)}.
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param key
     * @param cmp
     * @return
     */
    public static <T> int binarySearch(final T[] a, final int fromIndex, final int toIndex, final T key, final Comparator<? super T> cmp) {
        return Array.binarySearch(a, fromIndex, toIndex, key, cmp);
    }

    /**
     * {@link Collections#binarySearch(List, Object)}.
     *
     * @param <T>
     * @param c
     * @param key
     * @return
     */
    public static <T extends Comparable<? super T>> int binarySearch(final List<? extends T> c, final T key) {
        return Array.binarySearch(c, key);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param key
     * @return
     */
    public static <T extends Comparable<? super T>> int binarySearch(final List<? extends T> c, final int fromIndex, final int toIndex, final T key) {
        return Array.binarySearch(c, fromIndex, toIndex, key);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param key
     * @param cmp
     * @return
     */
    public static <T> int binarySearch(final List<? extends T> c, final T key, final Comparator<? super T> cmp) {
        return Array.binarySearch(c, key, cmp);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param key
     * @param cmp
     * @return
     * @see Collections#binarySearch(List, Object, Comparator)
     */
    public static <T> int binarySearch(final List<? extends T> c, final int fromIndex, final int toIndex, final T key, final Comparator<? super T> cmp) {
        return Array.binarySearch(c, fromIndex, toIndex, key, cmp);
    }

    /**
     * Binary search by.
     *
     * @param <T>
     * @param <U>
     * @param a
     * @param key
     * @param keyMapper
     * @return
     */
    public static <T, U extends Comparable<? super U>> int binarySearchBy(final T[] a, final T key, final Function<? super T, ? extends U> keyMapper) {
        return binarySearch(a, key, Comparators.comparingBy(keyMapper));
    }

    /**
     * Binary search by.
     *
     * @param <T>
     * @param <U>
     * @param c
     * @param key
     * @param keyMapper
     * @return
     */
    public static <T, U extends Comparable<? super U>> int binarySearchBy(final List<? extends T> c, final T key,
            final Function<? super T, ? extends U> keyMapper) {
        return binarySearch(c, key, Comparators.comparingBy(keyMapper));
    }

    /**
     *
     * @param a
     * @param e
     * @return
     */
    public static int indexOf(final boolean[] a, final boolean e) {
        return indexOf(a, 0, e);
    }

    /**
     *
     * @param a
     * @param fromIndex the index from which to start the search.
     * @param e
     * @return
     */
    public static int indexOf(final boolean[] a, final int fromIndex, final boolean e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        for (int i = fromIndex, len = a.length; i < len; i++) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param e
     * @return
     */
    public static int indexOf(final char[] a, final char e) {
        return indexOf(a, 0, e);
    }

    /**
     *
     * @param a
     * @param fromIndex the index from which to start the search.
     * @param e
     * @return
     */
    public static int indexOf(final char[] a, final int fromIndex, final char e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        for (int i = fromIndex, len = a.length; i < len; i++) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param e
     * @return
     */
    public static int indexOf(final byte[] a, final byte e) {
        return indexOf(a, 0, e);

    }

    /**
     *
     * @param a
     * @param fromIndex the index from which to start the search.
     * @param e
     * @return
     */
    public static int indexOf(final byte[] a, final int fromIndex, final byte e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        for (int i = fromIndex, len = a.length; i < len; i++) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param e
     * @return
     */
    public static int indexOf(final short[] a, final short e) {
        return indexOf(a, 0, e);
    }

    /**
     *
     * @param a
     * @param fromIndex the index from which to start the search.
     * @param e
     * @return
     */
    public static int indexOf(final short[] a, final int fromIndex, final short e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        for (int i = fromIndex, len = a.length; i < len; i++) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param e
     * @return
     */
    public static int indexOf(final int[] a, final int e) {
        return indexOf(a, 0, e);
    }

    /**
     *
     * @param a
     * @param fromIndex the index from which to start the search.
     * @param e
     * @return
     */
    public static int indexOf(final int[] a, final int fromIndex, final int e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        for (int i = fromIndex, len = a.length; i < len; i++) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param e
     * @return
     */
    public static int indexOf(final long[] a, final long e) {
        return indexOf(a, 0, e);
    }

    /**
     *
     * @param a
     * @param fromIndex the index from which to start the search.
     * @param e
     * @return
     */
    public static int indexOf(final long[] a, final int fromIndex, final long e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        for (int i = fromIndex, len = a.length; i < len; i++) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param e
     * @return
     */
    public static int indexOf(final float[] a, final float e) {
        return indexOf(a, 0, e);
    }

    /**
     *
     * @param a
     * @param fromIndex the index from which to start the search.
     * @param e
     * @return
     */
    public static int indexOf(final float[] a, final int fromIndex, final float e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        for (int i = fromIndex, len = a.length; i < len; i++) {
            if (Float.compare(a[i], e) == 0) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param e
     * @return
     */
    public static int indexOf(final double[] a, final double e) {
        return indexOf(a, 0, e);
    }

    /**
     *
     * @param a
     * @param fromIndex the index from which to start the search.
     * @param e
     * @return
     */
    public static int indexOf(final double[] a, final int fromIndex, final double e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        for (int i = fromIndex, len = a.length; i < len; i++) {
            if (Double.compare(a[i], e) == 0) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param e
     * @return
     */
    public static int indexOf(final Object[] a, final Object e) {
        return indexOf(a, 0, e);
    }

    /**
     *
     * @param a
     * @param fromIndex the index from which to start the search.
     * @param e
     * @return
     */
    public static int indexOf(final Object[] a, final int fromIndex, final Object e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        for (int i = fromIndex, len = a.length; i < len; i++) {
            if (equals(a[i], e)) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     *
     * @param list
     * @param e
     * @return
     */
    public static int indexOf(final List<?> list, final Object e) {
        return indexOf(list, 0, e);
    }

    /**
     *
     * @param list
     * @param fromIndex the index from which to start the search.
     * @param e
     * @return
     */
    public static int indexOf(final List<?> list, final int fromIndex, final Object e) {
        if (CommonUtil.isNullOrEmpty(list)) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        for (int i = fromIndex, len = list.size(); i < len; i++) {
            if (equals(list.get(i), e)) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     * Index of sub list.
     *
     * @param sourceList
     * @param targetSubList
     * @return
     * @see java.util.Collections#indexOfSubList(List, List)
     */
    public static int indexOfSubList(final List<?> sourceList, final List<?> targetSubList) {
        if (CommonUtil.isNullOrEmpty(sourceList) || CommonUtil.isNullOrEmpty(targetSubList)) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        return Collections.indexOfSubList(sourceList, targetSubList);
    }

    /**
     * Last index of.
     *
     * @param a
     * @param e
     * @return
     */
    public static int lastIndexOf(final boolean[] a, final boolean e) {
        return lastIndexOf(a, a.length - 1, e);
    }

    /**
     * Last index of.
     *
     * @param a
     * @param fromIndex the start index to traverse backwards from
     * @param e
     * @return
     */
    public static int lastIndexOf(final boolean[] a, final int fromIndex, final boolean e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        for (int i = N.min(fromIndex, a.length - 1); i >= 0; i--) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     * Last index of.
     *
     * @param a
     * @param e
     * @return
     */
    public static int lastIndexOf(final char[] a, final char e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, e);
    }

    /**
     * Last index of.
     *
     * @param a
     * @param fromIndex the start index to traverse backwards from
     * @param e
     * @return
     */
    public static int lastIndexOf(final char[] a, final int fromIndex, final char e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        for (int i = N.min(fromIndex, a.length - 1); i >= 0; i--) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     * Last index of.
     *
     * @param a
     * @param e
     * @return
     */
    public static int lastIndexOf(final byte[] a, final byte e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, e);

    }

    /**
     * Last index of.
     *
     * @param a
     * @param fromIndex the start index to traverse backwards from
     * @param e
     * @return
     */
    public static int lastIndexOf(final byte[] a, final int fromIndex, final byte e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        for (int i = N.min(fromIndex, a.length - 1); i >= 0; i--) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     * Last index of.
     *
     * @param a
     * @param e
     * @return
     */
    public static int lastIndexOf(final short[] a, final short e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, e);
    }

    /**
     * Last index of.
     *
     * @param a
     * @param fromIndex the start index to traverse backwards from
     * @param e
     * @return
     */
    public static int lastIndexOf(final short[] a, final int fromIndex, final short e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        for (int i = N.min(fromIndex, a.length - 1); i >= 0; i--) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     * Last index of.
     *
     * @param a
     * @param e
     * @return
     */
    public static int lastIndexOf(final int[] a, final int e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, e);
    }

    /**
     * Last index of.
     *
     * @param a
     * @param fromIndex the start index to traverse backwards from
     * @param e
     * @return
     */
    public static int lastIndexOf(final int[] a, final int fromIndex, final int e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        for (int i = N.min(fromIndex, a.length - 1); i >= 0; i--) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     * Last index of.
     *
     * @param a
     * @param e
     * @return
     */
    public static int lastIndexOf(final long[] a, final long e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, e);
    }

    /**
     * Last index of.
     *
     * @param a
     * @param fromIndex the start index to traverse backwards from
     * @param e
     * @return
     */
    public static int lastIndexOf(final long[] a, final int fromIndex, final long e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        for (int i = N.min(fromIndex, a.length - 1); i >= 0; i--) {
            if (a[i] == e) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     * Last index of.
     *
     * @param a
     * @param e
     * @return
     */
    public static int lastIndexOf(final float[] a, final float e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, e);
    }

    /**
     * Last index of.
     *
     * @param a
     * @param fromIndex the start index to traverse backwards from
     * @param e
     * @return
     */
    public static int lastIndexOf(final float[] a, final int fromIndex, final float e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        for (int i = N.min(fromIndex, a.length - 1); i >= 0; i--) {
            if (Float.compare(a[i], e) == 0) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     * Last index of.
     *
     * @param a
     * @param e
     * @return
     */
    public static int lastIndexOf(final double[] a, final double e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, e);
    }

    /**
     * Last index of.
     *
     * @param a
     * @param fromIndex the start index to traverse backwards from
     * @param e
     * @return
     */
    public static int lastIndexOf(final double[] a, final int fromIndex, final double e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        for (int i = N.min(fromIndex, a.length - 1); i >= 0; i--) {
            if (Double.compare(a[i], e) == 0) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     * Last index of.
     *
     * @param a
     * @param e
     * @return
     */
    public static int lastIndexOf(final Object[] a, final Object e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(a, a.length - 1, e);
    }

    /**
     * Last index of.
     *
     * @param a
     * @param fromIndex the start index to traverse backwards from
     * @param e
     * @return
     */
    public static int lastIndexOf(final Object[] a, final int fromIndex, final Object e) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        for (int i = N.min(fromIndex, a.length - 1); i >= 0; i--) {
            if (equals(a[i], e)) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     * Last index of.
     *
     * @param list
     * @param e
     * @return
     */
    public static int lastIndexOf(final List<?> list, final Object e) {
        if (CommonUtil.isNullOrEmpty(list)) {
            return INDEX_NOT_FOUND;
        }

        return lastIndexOf(list, list.size() - 1, e);
    }

    /**
     * Last index of.
     *
     * @param list
     * @param fromIndex the start index to traverse backwards from
     * @param e
     * @return
     */
    public static int lastIndexOf(final List<?> list, final int fromIndex, final Object e) {
        if (CommonUtil.isNullOrEmpty(list)) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        for (int i = N.min(fromIndex, list.size() - 1); i >= 0; i--) {
            if (equals(list.get(i), e)) {
                return i;
            }
        }

        return INDEX_NOT_FOUND;
    }

    /**
     * Last index of sub list.
     *
     * @param sourceList
     * @param targetSubList
     * @return
     * @see java.util.Collections#lastIndexOfSubList(List, List)
     */
    public static int lastIndexOfSubList(final List<?> sourceList, final List<?> targetSubList) {
        if (CommonUtil.isNullOrEmpty(sourceList) || CommonUtil.isNullOrEmpty(targetSubList)) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        return Collections.lastIndexOfSubList(sourceList, targetSubList);
    }

    /**
     * Find first index.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param predicate the predicate
     * @return the optional int
     * @throws E the e
     */
    @SuppressWarnings("deprecation")
    public static <T, E extends Exception> OptionalInt findFirstIndex(final T[] a, final Throwables.Predicate<? super T, E> predicate) throws E {
        return Iterables.findFirstIndex(a, predicate);
    }

    /**
     * Find first index.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param predicate the predicate
     * @return the optional int
     * @throws E the e
     */
    @SuppressWarnings("deprecation")
    public static <T, E extends Exception> OptionalInt findFirstIndex(final Collection<? extends T> c, final Throwables.Predicate<? super T, E> predicate)
            throws E {
        return Iterables.findFirstIndex(c, predicate);
    }

    /**
     * Find last index.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param predicate the predicate
     * @return the optional int
     * @throws E the e
     */
    @SuppressWarnings("deprecation")
    public static <T, E extends Exception> OptionalInt findLastIndex(final T[] a, final Throwables.Predicate<? super T, E> predicate) throws E {
        return Iterables.findLastIndex(a, predicate);
    }

    /**
     * Find last index.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param predicate the predicate
     * @return the optional int
     * @throws E the e
     */
    @SuppressWarnings("deprecation")
    public static <T, E extends Exception> OptionalInt findLastIndex(final Collection<? extends T> c, final Throwables.Predicate<? super T, E> predicate)
            throws E {
        return Iterables.findLastIndex(c, predicate);
    }

    /**
     * Creates the mask.
     *
     * @param <T>
     * @param interfaceClass
     * @return
     */
    static <T> T createMask(final Class<T> interfaceClass) {
        InvocationHandler h = new InvocationHandler() {
            @Override
            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                throw new UnsupportedOperationException("It's a mask");
            }
        };

        return newProxyInstance(interfaceClass, h);
    }

    /**
     * The Class NullMask.
     */
    static class NullMask implements Serializable {

        /** The Constant serialVersionUID. */
        private static final long serialVersionUID = 5887875956120266479L;

        /**
         * Instantiates a new null mask.
         */
        private NullMask() {
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return "NULL";
        }

        /**
         *
         * @return
         */
        private Object readResolve() {
            return NULL_MASK;
        }
    }
}
