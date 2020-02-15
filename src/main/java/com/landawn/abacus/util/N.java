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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.RandomAccess;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.landawn.abacus.exception.DuplicatedResultException;
import com.landawn.abacus.exception.UncheckedException;
import com.landawn.abacus.parser.DeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.parser.JSONSerializationConfig;
import com.landawn.abacus.parser.XMLDeserializationConfig;
import com.landawn.abacus.parser.XMLDeserializationConfig.XDC;
import com.landawn.abacus.parser.XMLSerializationConfig;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Fn.Factory;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;

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
public final class N extends CommonUtil {

    private static final float LOAD_FACTOR_FOR_FLAT_MAP = 1.75f;

    private static final int LOAD_FACTOR_FOR_TWO_FLAT_MAP = 2;

    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    /** The Constant asyncExecutor. */
    private static final AsyncExecutor asyncExecutor = new AsyncExecutor(Math.max(8, CPU_CORES), Math.max(256, CPU_CORES), 180L, TimeUnit.SECONDS);

    /** The Constant SCHEDULED_EXECUTOR. */
    private static final ScheduledExecutorService SCHEDULED_EXECUTOR;

    static {
        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(8);
        executor.setKeepAliveTime(180, TimeUnit.SECONDS);
        executor.allowCoreThreadTimeOut(true);
        executor.setRemoveOnCancelPolicy(true);
        SCHEDULED_EXECUTOR = MoreExecutors.getExitingScheduledExecutorService(executor);
    }

    private N() {
        // Utility class.
    }

    /**
     *
     * @param a
     * @param objectToFind
     * @return
     */
    public static int occurrencesOf(final boolean[] a, final boolean objectToFind) {
        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param a
     * @param objectToFind
     * @return
     */
    public static int occurrencesOf(final char[] a, final char objectToFind) {
        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param a
     * @param objectToFind
     * @return
     */
    public static int occurrencesOf(final byte[] a, final byte objectToFind) {
        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param a
     * @param objectToFind
     * @return
     */
    public static int occurrencesOf(final short[] a, final short objectToFind) {
        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param a
     * @param objectToFind
     * @return
     */
    public static int occurrencesOf(final int[] a, final int objectToFind) {
        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param a
     * @param objectToFind
     * @return
     */
    public static int occurrencesOf(final long[] a, final long objectToFind) {
        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param a
     * @param objectToFind
     * @return
     */
    public static int occurrencesOf(final float[] a, final float objectToFind) {
        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param a
     * @param objectToFind
     * @return
     */
    public static int occurrencesOf(final double[] a, final double objectToFind) {
        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param a
     * @param objectToFind
     * @return
     */
    public static int occurrencesOf(final Object[] a, final Object objectToFind) {
        if (isNullOrEmpty(a)) {
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
     * @see java.util.Collections#frequency(Collection, Object)
     */
    public static int occurrencesOf(final Collection<?> c, final Object objectToFind) {
        if (isNullOrEmpty(c)) {
            return 0;
        }

        return Collections.frequency(c, objectToFind);
    }

    /**
     *
     * @param a
     * @param objectToFind
     * @return true, if successful
     */
    public static boolean contains(final boolean[] a, final boolean objectToFind) {
        return indexOf(a, objectToFind) != INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param objectToFind
     * @return true, if successful
     */
    public static boolean contains(final char[] a, final char objectToFind) {
        return indexOf(a, objectToFind) != INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param objectToFind
     * @return true, if successful
     */
    public static boolean contains(final byte[] a, final byte objectToFind) {
        return indexOf(a, objectToFind) != INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param objectToFind
     * @return true, if successful
     */
    public static boolean contains(final short[] a, final short objectToFind) {
        return indexOf(a, objectToFind) != INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param objectToFind
     * @return true, if successful
     */
    public static boolean contains(final int[] a, final int objectToFind) {
        return indexOf(a, objectToFind) != INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param objectToFind
     * @return true, if successful
     */
    public static boolean contains(final long[] a, final long objectToFind) {
        return indexOf(a, objectToFind) != INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param objectToFind
     * @return true, if successful
     */
    public static boolean contains(final float[] a, final float objectToFind) {
        return indexOf(a, objectToFind) != INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param objectToFind
     * @return true, if successful
     */
    public static boolean contains(final double[] a, final double objectToFind) {
        return indexOf(a, objectToFind) != INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param objectToFind
     * @return true, if successful
     */
    public static boolean contains(final Object[] a, final Object objectToFind) {
        return indexOf(a, objectToFind) != INDEX_NOT_FOUND;
    }

    /**
     *
     * @param c
     * @param e
     * @return true, if successful
     */
    public static boolean contains(final Collection<?> c, final Object e) {
        if (isNullOrEmpty(c)) {
            return false;
        }

        return c.contains(e);
    }

    /**
     * Contains all.
     *
     * @param c the c
     * @param objsToFind the objs to find
     * @return true, if successful
     */
    public static boolean containsAll(final Collection<?> c, final Collection<?> objsToFind) {
        if (N.isNullOrEmpty(objsToFind)) {
            return true;
        } else if (N.isNullOrEmpty(c)) {
            return false;
        }

        return c.containsAll(objsToFind);
    }

    /**
     * Contains all.
     *
     * @param c the c
     * @param objsToFind the objs to find
     * @return true, if successful
     */
    public static boolean containsAll(final Collection<?> c, final Object[] objsToFind) {
        if (N.isNullOrEmpty(objsToFind)) {
            return true;
        } else if (N.isNullOrEmpty(c)) {
            return false;
        }

        return c.containsAll(Array.asList(objsToFind));
    }

    /**
     * Contains any.
     *
     * @param c the c
     * @param objsToFind the objs to find
     * @return true, if successful
     */
    public static boolean containsAny(final Collection<?> c, final Collection<?> objsToFind) {
        if (N.isNullOrEmpty(c) || N.isNullOrEmpty(objsToFind)) {
            return false;
        }

        return !N.disjoint(c, objsToFind);
    }

    /**
     * Contains any.
     *
     * @param c the c
     * @param objsToFind the objs to find
     * @return true, if successful
     */
    public static boolean containsAny(final Collection<?> c, final Object[] objsToFind) {
        if (N.isNullOrEmpty(c) || N.isNullOrEmpty(objsToFind)) {
            return false;
        }

        return !N.disjoint(c, Array.asList(objsToFind));
    }

    /**
     * Gets the only element.
     *
     * @param <T> the generic type
     * @param iterable the iterable
     * @return throws DuplicatedResultException if there are more than one elements in the specified {@code iterable}.
     */
    public static <T> Nullable<T> getOnlyElement(Iterable<? extends T> iterable) throws DuplicatedResultException {
        if (iterable == null) {
            return Nullable.empty();
        }

        return Iterators.getOnlyElement(iterable.iterator());
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
        checkArgPositive(chunkSize, "chunkSize");

        if (isNullOrEmpty(a)) {
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
        checkArgPositive(chunkSize, "chunkSize");

        if (isNullOrEmpty(a)) {
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
        checkArgPositive(chunkSize, "chunkSize");

        if (isNullOrEmpty(a)) {
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
        checkArgPositive(chunkSize, "chunkSize");

        if (isNullOrEmpty(a)) {
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
        checkArgPositive(chunkSize, "chunkSize");

        if (isNullOrEmpty(a)) {
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
        checkArgPositive(chunkSize, "chunkSize");

        if (isNullOrEmpty(a)) {
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
        checkArgPositive(chunkSize, "chunkSize");

        if (isNullOrEmpty(a)) {
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
        checkArgPositive(chunkSize, "chunkSize");

        if (isNullOrEmpty(a)) {
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
        checkArgPositive(chunkSize, "chunkSize");

        if (isNullOrEmpty(a)) {
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
        checkArgPositive(chunkSize, "chunkSize");

        if (isNullOrEmpty(a)) {
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
        checkArgPositive(chunkSize, "chunkSize");

        if (isNullOrEmpty(a)) {
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
        checkArgPositive(chunkSize, "chunkSize");

        if (isNullOrEmpty(a)) {
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
        checkArgPositive(chunkSize, "chunkSize");

        if (isNullOrEmpty(a)) {
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
        checkArgPositive(chunkSize, "chunkSize");

        if (isNullOrEmpty(a)) {
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
        checkArgPositive(chunkSize, "chunkSize");

        if (isNullOrEmpty(a)) {
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
        checkArgPositive(chunkSize, "chunkSize");

        if (isNullOrEmpty(a)) {
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
     * @param <T>
     * @param a
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static <T> List<T[]> split(final T[] a, final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        if (isNullOrEmpty(a)) {
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
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static <T> List<T[]> split(final T[] a, final int fromIndex, final int toIndex, final int chunkSize) {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgPositive(chunkSize, "chunkSize");

        if (isNullOrEmpty(a)) {
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
     * @param <T>
     * @param c
     * @param chunkSize the desired size of each sub list (the last may be smaller).
     * @return
     */
    public static <T> List<List<T>> split(final Collection<? extends T> c, final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        if (isNullOrEmpty(c)) {
            return new ArrayList<>();
        }

        return split(c, 0, c.size(), chunkSize);
    }

    /**
     * Returns consecutive sub lists of a collection, each of the same chunkSize (the final list may be smaller).
     * or an empty List if the specified collection is null or empty. The order of elements in the original collection is kept
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param chunkSize the desired size of each sub list (the last may be smaller).
     * @return
     */
    public static <T> List<List<T>> split(final Collection<? extends T> c, final int fromIndex, final int toIndex, final int chunkSize) {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgPositive(chunkSize, "chunkSize");

        if (isNullOrEmpty(c)) {
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

                final List<T> subList = new ArrayList<>(min(chunkSize, toIndex - i));

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
        checkArgPositive(chunkSize, "chunkSize");

        if (isNullOrEmpty(str)) {
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
        checkFromToIndex(fromIndex, toIndex, len(str));
        checkArgPositive(chunkSize, "chunkSize");

        if (isNullOrEmpty(str)) {
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
        if (isNullOrEmpty(a) || isNullOrEmpty(b)) {
            return isNullOrEmpty(a) ? a : EMPTY_BOOLEAN_ARRAY;
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
        if (isNullOrEmpty(a) || isNullOrEmpty(b)) {
            return EMPTY_CHAR_ARRAY;
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
        if (isNullOrEmpty(a) || isNullOrEmpty(b)) {
            return EMPTY_BYTE_ARRAY;
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
        if (isNullOrEmpty(a) || isNullOrEmpty(b)) {
            return EMPTY_SHORT_ARRAY;
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
        if (isNullOrEmpty(a) || isNullOrEmpty(b)) {
            return EMPTY_INT_ARRAY;
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
        if (isNullOrEmpty(a) || isNullOrEmpty(b)) {
            return EMPTY_LONG_ARRAY;
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
        if (isNullOrEmpty(a) || isNullOrEmpty(b)) {
            return EMPTY_FLOAT_ARRAY;
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
        if (isNullOrEmpty(a) || isNullOrEmpty(b)) {
            return EMPTY_DOUBLE_ARRAY;
        }

        return DoubleList.of(a).intersection(DoubleList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     * @see IntList#intersection(IntList)
     */
    public static <T> List<T> intersection(final T[] a, final Object[] b) {
        if (isNullOrEmpty(a) || isNullOrEmpty(b)) {
            return new ArrayList<>();
        }

        final Multiset<?> bOccurrences = Multiset.of(b);
        final List<T> result = new ArrayList<>(min(9, a.length, b.length));

        for (T e : a) {
            if (bOccurrences.getAndRemove(e) > 0) {
                result.add(e);
            }
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     * @see IntList#intersection(IntList)
     */
    public static <T> List<T> intersection(final Collection<? extends T> a, final Collection<?> b) {
        if (isNullOrEmpty(a) || isNullOrEmpty(b)) {
            return new ArrayList<>();
        }

        final Multiset<Object> bOccurrences = Multiset.from(b);

        final List<T> result = new ArrayList<>(min(9, a.size(), b.size()));

        for (T e : a) {
            if (bOccurrences.getAndRemove(e) > 0) {
                result.add(e);
            }
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> List<T> intersection(final Collection<? extends Collection<? extends T>> c) {
        if (isNullOrEmpty(c)) {
            return new ArrayList<>();
        } else if (c.size() == 1) {
            return newArrayList(c.iterator().next());
        }

        for (Collection<? extends T> e : c) {
            if (isNullOrEmpty(e)) {
                return new ArrayList<>();
            }
        }

        final Iterator<? extends Collection<? extends T>> iter = c.iterator();
        List<T> result = intersection(iter.next(), iter.next());

        while (iter.hasNext()) {
            result = intersection(result, iter.next());

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
        if (isNullOrEmpty(a)) {
            return EMPTY_BOOLEAN_ARRAY;
        } else if (isNullOrEmpty(b)) {
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
        if (isNullOrEmpty(a)) {
            return EMPTY_CHAR_ARRAY;
        } else if (isNullOrEmpty(b)) {
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
        if (isNullOrEmpty(a)) {
            return EMPTY_BYTE_ARRAY;
        } else if (isNullOrEmpty(b)) {
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
        if (isNullOrEmpty(a)) {
            return EMPTY_SHORT_ARRAY;
        } else if (isNullOrEmpty(b)) {
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
        if (isNullOrEmpty(a)) {
            return EMPTY_INT_ARRAY;
        } else if (isNullOrEmpty(b)) {
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
        if (isNullOrEmpty(a)) {
            return EMPTY_LONG_ARRAY;
        } else if (isNullOrEmpty(b)) {
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
        if (isNullOrEmpty(a)) {
            return EMPTY_FLOAT_ARRAY;
        } else if (isNullOrEmpty(b)) {
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
        if (isNullOrEmpty(a)) {
            return EMPTY_DOUBLE_ARRAY;
        } else if (isNullOrEmpty(b)) {
            return a.clone();
        }

        return DoubleList.of(a).difference(DoubleList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     * @see IntList#difference(IntList)
     */
    public static <T> List<T> difference(final T[] a, final Object[] b) {
        if (isNullOrEmpty(a)) {
            return new ArrayList<>();
        } else if (isNullOrEmpty(b)) {
            return asList(a);
        }

        final Multiset<?> bOccurrences = Multiset.of(b);
        final List<T> result = new ArrayList<>(min(a.length, max(9, a.length - b.length)));

        for (T e : a) {
            if (bOccurrences.getAndRemove(e) < 1) {
                result.add(e);
            }
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     * @see IntList#difference(IntList)
     */
    public static <T> List<T> difference(final Collection<? extends T> a, final Collection<?> b) {
        if (isNullOrEmpty(a)) {
            return new ArrayList<>();
        } else if (isNullOrEmpty(b)) {
            return new ArrayList<>(a);
        }

        final Multiset<Object> bOccurrences = Multiset.from(b);

        final List<T> result = new ArrayList<>(min(a.size(), max(9, a.size() - b.size())));

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
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_BOOLEAN_ARRAY : b.clone();
        } else if (isNullOrEmpty(b)) {
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
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_CHAR_ARRAY : b.clone();
        } else if (isNullOrEmpty(b)) {
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
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_BYTE_ARRAY : b.clone();
        } else if (isNullOrEmpty(b)) {
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
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_SHORT_ARRAY : b.clone();
        } else if (isNullOrEmpty(b)) {
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
     * @see CommonUtil#difference(int[], int[])
     */
    public static int[] symmetricDifference(final int[] a, final int[] b) {
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_INT_ARRAY : b.clone();
        } else if (isNullOrEmpty(b)) {
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
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_LONG_ARRAY : b.clone();
        } else if (isNullOrEmpty(b)) {
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
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_FLOAT_ARRAY : b.clone();
        } else if (isNullOrEmpty(b)) {
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
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_DOUBLE_ARRAY : b.clone();
        } else if (isNullOrEmpty(b)) {
            return a.clone();
        }

        return DoubleList.of(a).symmetricDifference(DoubleList.of(b)).trimToSize().array();
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     * @see IntList#symmetricDifference(IntList)
     */
    public static <T> List<T> symmetricDifference(final T[] a, final T[] b) {
        if (isNullOrEmpty(a)) {
            return asList(b);
        } else if (isNullOrEmpty(b)) {
            return asList(a);
        }

        final Multiset<T> bOccurrences = Multiset.of(b);

        final List<T> result = new ArrayList<>(max(9, Math.abs(a.length - b.length)));

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
     * @param <T>
     * @param a
     * @param b
     * @return
     * @see IntList#symmetricDifference(IntList)
     */
    public static <T> List<T> symmetricDifference(final Collection<? extends T> a, final Collection<? extends T> b) {
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? new ArrayList<>() : new ArrayList<>(b);
        } else if (isNullOrEmpty(b)) {
            return isNullOrEmpty(a) ? new ArrayList<>() : new ArrayList<>(a);
        }

        final Multiset<T> bOccurrences = Multiset.from(b);
        final List<T> result = new ArrayList<>(max(9, Math.abs(a.size() - b.size())));

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
     * Different set.
     *
     * @param <T> the generic type
     * @param a the a
     * @param b the b
     * @return the sets the
     */
    @SuppressWarnings("rawtypes")
    public static <T> Set<T> differentSet(final Collection<? extends T> a, final Collection<?> b) {
        if (N.isNullOrEmpty(a)) {
            return N.newHashSet();
        } else if (N.isNullOrEmpty(b)) {
            return N.newHashSet(a);
        }

        final Set<T> result = N.newHashSet(a);

        N.removeAll(a, (Collection) b);

        return result;
    }

    /**
     * Symmetric different set.
     *
     * @param <T> the generic type
     * @param a the a
     * @param b the b
     * @return the sets the
     */
    public static <T> Set<T> symmetricDifferentSet(final Collection<? extends T> a, final Collection<? extends T> b) {
        if (N.isNullOrEmpty(a)) {
            return N.isNullOrEmpty(b) ? N.<T> newHashSet() : N.<T> newHashSet(b);
        } else if (N.isNullOrEmpty(b)) {
            return N.isNullOrEmpty(a) ? N.<T> newHashSet() : N.<T> newHashSet(a);
        }

        final Set<T> commonSet = commonSet(a, b);
        final Set<T> result = N.newHashSet(a);

        for (T e : a) {
            if (!commonSet.contains(e)) {
                result.add(e);
            }
        }

        for (T e : b) {
            if (!commonSet.contains(e)) {
                result.add(e);
            }
        }

        return result;
    }

    /**
     * Common set.
     *
     * @param <T> the generic type
     * @param a the a
     * @param b the b
     * @return the sets the
     */
    public static <T> Set<T> commonSet(final Collection<? extends T> a, final Collection<?> b) {
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
            return N.newHashSet();
        }

        return commonSet(Array.asList(a, (Collection<? extends T>) b));
    }

    /**
     * Common set.
     *
     * @param <T> the generic type
     * @param c the c
     * @return the sets the
     */
    public static <T> Set<T> commonSet(final Collection<? extends Collection<? extends T>> c) {
        if (N.isNullOrEmpty(c)) {
            return N.newHashSet();
        } else if (c.size() == 1) {
            return N.newHashSet(c.iterator().next());
        }

        Collection<? extends T> smallest = null;

        for (final Collection<? extends T> e : c) {
            if (N.isNullOrEmpty(e)) {
                return N.newHashSet();
            }

            if (smallest == null || e.size() < smallest.size()) {
                smallest = e;
            }
        }

        final Map<T, MutableInt> map = new HashMap<>();

        for (T e : smallest) {
            map.put(e, new MutableInt(1));
        }

        int cnt = 1;
        MutableInt val = null;

        for (final Collection<? extends T> ec : c) {
            if (ec == smallest) {
                continue;
            }

            for (T e : ec) {
                val = map.get(e);

                if (val == null) {
                    // do nothing.
                } else if (val.intValue() < cnt) {
                    // map.remove(e);
                } else if (val.intValue() == cnt) {
                    val.increment();
                }
            }

            cnt++;
        }

        final Set<T> result = N.newHashSet(map.size());

        for (Map.Entry<T, MutableInt> entry : map.entrySet()) {
            if (entry.getValue().intValue() == cnt) {
                result.add(entry.getKey());
            }
        }

        return result;
    }

    /**
     * Returns a new {@code List} with specified {@code objToExclude} excluded.
     *
     * @param <T>
     * @param c
     * @param objToExclude
     * @return a new {@code List}
     */
    public static <T> List<T> exclude(final Collection<T> c, final Object objToExclude) {
        if (N.isNullOrEmpty(c)) {
            return new ArrayList<>();
        }

        final List<T> result = new ArrayList<>(c.size() - 1);

        for (T e : c) {
            if (!N.equals(e, objToExclude)) {
                result.add(e);
            }
        }

        return result;
    }

    /**
     * Returns a new {@code Set} with specified {@code objToExclude} excluded.
     *
     * @param <T>
     * @param c
     * @param objToExclude
     * @return a new {@code Set}
     */
    public static <T> Set<T> excludeToSet(final Collection<T> c, final Object objToExclude) {
        if (N.isNullOrEmpty(c)) {
            return new HashSet<>();
        }

        final Set<T> result = new HashSet<>(c.size() - 1);

        for (T e : c) {
            if (!N.equals(e, objToExclude)) {
                result.add(e);
            }
        }

        return result;
    }

    /**
     * Returns a new {@code List} with specified {@code objsToExclude} excluded.
     *
     * @param c
     * @param objsToExclude
     * @return a new {@code List}
     */
    public static <T> List<T> excludeAll(final Collection<T> c, final Collection<?> objsToExclude) {
        if (N.isNullOrEmpty(c)) {
            return new ArrayList<>();
        } else if (N.isNullOrEmpty(objsToExclude)) {
            return new ArrayList<>(c);
        } else if (objsToExclude.size() == 1) {
            return exclude(c, N.firstOrNullIfEmpty(objsToExclude));
        }

        final Set<Object> set = objsToExclude instanceof Set ? ((Set<Object>) objsToExclude) : new HashSet<Object>(objsToExclude);
        final List<T> result = new ArrayList<>(N.max(0, c.size() - set.size()));

        for (T e : c) {
            if (!set.contains(e)) {
                result.add(e);
            }
        }

        return result;
    }

    /**
     * Returns a new {@code Set} with specified {@code objsToExclude} excluded.
     *
     * @param c
     * @param objsToExclude
     * @return a new {@code Set}
     */
    public static <T> Set<T> excludeAllToSet(final Collection<T> c, final Collection<?> objsToExclude) {
        if (N.isNullOrEmpty(c)) {
            return new HashSet<>();
        } else if (N.isNullOrEmpty(objsToExclude)) {
            return new HashSet<>(c);
        } else if (objsToExclude.size() == 1) {
            return excludeToSet(c, N.firstOrNullIfEmpty(objsToExclude));
        }

        final Set<Object> set = objsToExclude instanceof Set ? ((Set<Object>) objsToExclude) : new HashSet<Object>(objsToExclude);
        final Set<T> result = new HashSet<>(N.max(0, c.size() - set.size()));

        for (T e : c) {
            if (!set.contains(e)) {
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
     */
    public static boolean[] concat(final boolean[] a, final boolean[] b) {
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_BOOLEAN_ARRAY : b.clone();
        } else if (isNullOrEmpty(b)) {
            return isNullOrEmpty(a) ? EMPTY_BOOLEAN_ARRAY : a.clone();
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
        if (isNullOrEmpty(aa)) {
            return EMPTY_BOOLEAN_ARRAY;
        } else if (aa.length == 1) {
            return isNullOrEmpty(aa[0]) ? EMPTY_BOOLEAN_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (boolean[] a : aa) {
            if (isNullOrEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final boolean[] c = new boolean[len];
        int fromIndex = 0;

        for (boolean[] a : aa) {
            if (isNullOrEmpty(a)) {
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
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_CHAR_ARRAY : b.clone();
        } else if (isNullOrEmpty(b)) {
            return isNullOrEmpty(a) ? EMPTY_CHAR_ARRAY : a.clone();
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
        if (isNullOrEmpty(aa)) {
            return EMPTY_CHAR_ARRAY;
        } else if (aa.length == 1) {
            return isNullOrEmpty(aa[0]) ? EMPTY_CHAR_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (char[] a : aa) {
            if (isNullOrEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final char[] c = new char[len];
        int fromIndex = 0;

        for (char[] a : aa) {
            if (isNullOrEmpty(a)) {
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
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_BYTE_ARRAY : b.clone();
        } else if (isNullOrEmpty(b)) {
            return isNullOrEmpty(a) ? EMPTY_BYTE_ARRAY : a.clone();
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
        if (isNullOrEmpty(aa)) {
            return EMPTY_BYTE_ARRAY;
        } else if (aa.length == 1) {
            return isNullOrEmpty(aa[0]) ? EMPTY_BYTE_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (byte[] a : aa) {
            if (isNullOrEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final byte[] c = new byte[len];
        int fromIndex = 0;

        for (byte[] a : aa) {
            if (isNullOrEmpty(a)) {
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
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_SHORT_ARRAY : b.clone();
        } else if (isNullOrEmpty(b)) {
            return isNullOrEmpty(a) ? EMPTY_SHORT_ARRAY : a.clone();
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
        if (isNullOrEmpty(aa)) {
            return EMPTY_SHORT_ARRAY;
        } else if (aa.length == 1) {
            return isNullOrEmpty(aa[0]) ? EMPTY_SHORT_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (short[] a : aa) {
            if (isNullOrEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final short[] c = new short[len];
        int fromIndex = 0;

        for (short[] a : aa) {
            if (isNullOrEmpty(a)) {
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
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_INT_ARRAY : b.clone();
        } else if (isNullOrEmpty(b)) {
            return isNullOrEmpty(a) ? EMPTY_INT_ARRAY : a.clone();
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
        if (isNullOrEmpty(aa)) {
            return EMPTY_INT_ARRAY;
        } else if (aa.length == 1) {
            return isNullOrEmpty(aa[0]) ? EMPTY_INT_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (int[] a : aa) {
            if (isNullOrEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final int[] c = new int[len];
        int fromIndex = 0;

        for (int[] a : aa) {
            if (isNullOrEmpty(a)) {
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
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_LONG_ARRAY : b.clone();
        } else if (isNullOrEmpty(b)) {
            return isNullOrEmpty(a) ? EMPTY_LONG_ARRAY : a.clone();
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
        if (isNullOrEmpty(aa)) {
            return EMPTY_LONG_ARRAY;
        } else if (aa.length == 1) {
            return isNullOrEmpty(aa[0]) ? EMPTY_LONG_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (long[] a : aa) {
            if (isNullOrEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final long[] c = new long[len];
        int fromIndex = 0;

        for (long[] a : aa) {
            if (isNullOrEmpty(a)) {
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
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_FLOAT_ARRAY : b.clone();
        } else if (isNullOrEmpty(b)) {
            return isNullOrEmpty(a) ? EMPTY_FLOAT_ARRAY : a.clone();
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
        if (isNullOrEmpty(aa)) {
            return EMPTY_FLOAT_ARRAY;
        } else if (aa.length == 1) {
            return isNullOrEmpty(aa[0]) ? EMPTY_FLOAT_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (float[] a : aa) {
            if (isNullOrEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final float[] c = new float[len];
        int fromIndex = 0;

        for (float[] a : aa) {
            if (isNullOrEmpty(a)) {
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
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_DOUBLE_ARRAY : b.clone();
        } else if (isNullOrEmpty(b)) {
            return isNullOrEmpty(a) ? EMPTY_DOUBLE_ARRAY : a.clone();
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
        if (isNullOrEmpty(aa)) {
            return EMPTY_DOUBLE_ARRAY;
        } else if (aa.length == 1) {
            return isNullOrEmpty(aa[0]) ? EMPTY_DOUBLE_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (double[] a : aa) {
            if (isNullOrEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final double[] c = new double[len];
        int fromIndex = 0;

        for (double[] a : aa) {
            if (isNullOrEmpty(a)) {
                continue;
            }

            System.arraycopy(a, 0, c, fromIndex, a.length);

            fromIndex += a.length;
        }

        return c;
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] concat(final T[] a, final T[] b) {
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? a : b.clone();
        } else if (isNullOrEmpty(b)) {
            return a.clone();
        }

        final T[] c = (T[]) newArray(a.getClass().getComponentType(), a.length + b.length);

        copy(a, 0, c, 0, a.length);
        copy(b, 0, c, a.length, b.length);

        return c;
    }

    /**
     *
     * @param <T>
     * @param aa
     * @return
     * @throws NullPointerException if the specified <code>aa</code> is <code>null</code>.
     */
    @SafeVarargs
    public static <T> T[] concat(final T[]... aa) {
        checkArgNotNull(aa, "aa");

        if (aa.length == 1) {
            return isNullOrEmpty(aa[0]) ? aa[0] : aa[0].clone();
        }

        int len = 0;

        for (T[] a : aa) {
            if (isNullOrEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final T[] c = newArray(aa.getClass().getComponentType().getComponentType(), len);
        int fromIndex = 0;

        for (T[] a : aa) {
            if (isNullOrEmpty(a)) {
                continue;
            }

            System.arraycopy(a, 0, c, fromIndex, a.length);

            fromIndex += a.length;
        }

        return c;
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    public static <T> List<T> concat(final Collection<? extends T> a, final Collection<? extends T> b) {
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? new ArrayList<>(0) : new ArrayList<>(b);
        } else if (isNullOrEmpty(b)) {
            return new ArrayList<>(a);
        }

        final List<T> result = new ArrayList<>(a.size() + b.size());

        result.addAll(a);
        result.addAll(b);

        return result;
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> List<T> concat(final Collection<? extends T>... a) {
        if (isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        return concat(Arrays.asList(a));
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> List<T> concat(final Collection<? extends Collection<? extends T>> c) {
        return concat(c, Factory.<T> ofList());
    }

    /**
     *
     * @param <T>
     * @param <C>
     * @param c
     * @param supplier
     * @return
     */
    public static <T, C extends Collection<T>> C concat(final Collection<? extends Collection<? extends T>> c, final IntFunction<? extends C> supplier) {
        if (isNullOrEmpty(c)) {
            return supplier.apply(0);
        }

        int count = 0;

        for (Collection<? extends T> e : c) {
            if (notNullOrEmpty(e)) {
                count += e.size();
            }
        }

        final C result = supplier.apply(count);

        for (Collection<? extends T> e : c) {
            if (notNullOrEmpty(e)) {
                result.addAll(e);
            }
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    public static <T> ObjIterator<T> concat(final Iterator<? extends T> a, final Iterator<? extends T> b) {
        return Iterators.concat(a, b);
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> ObjIterator<T> concat(final Iterator<? extends T>... a) {
        return Iterators.concat(a);
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> ObjIterator<T> concatt(final Collection<? extends Iterator<? extends T>> c) {
        return Iterators.concat(c);
    }

    /**
     *
     * @param a
     * @param oldVal
     * @param newVal
     * @return
     */
    public static int replaceAll(final boolean[] a, final boolean oldVal, final boolean newVal) {
        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param a
     * @param oldVal
     * @param newVal
     * @return
     */
    public static int replaceAll(final char[] a, final char oldVal, final char newVal) {
        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param a
     * @param oldVal
     * @param newVal
     * @return
     */
    public static int replaceAll(final byte[] a, final byte oldVal, final byte newVal) {
        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param a
     * @param oldVal
     * @param newVal
     * @return
     */
    public static int replaceAll(final short[] a, final short oldVal, final short newVal) {
        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param a
     * @param oldVal
     * @param newVal
     * @return
     */
    public static int replaceAll(final int[] a, final int oldVal, final int newVal) {
        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param a
     * @param oldVal
     * @param newVal
     * @return
     */
    public static int replaceAll(final long[] a, final long oldVal, final long newVal) {
        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param a
     * @param oldVal
     * @param newVal
     * @return
     */
    public static int replaceAll(final float[] a, final float oldVal, final float newVal) {
        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param a
     * @param oldVal
     * @param newVal
     * @return
     */
    public static int replaceAll(final double[] a, final double oldVal, final double newVal) {
        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param <T>
     * @param a
     * @param oldVal
     * @param newVal
     * @return
     */
    public static <T> int replaceAll(final T[] a, final Object oldVal, final T newVal) {
        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param <T>
     * @param list
     * @param oldVal
     * @param newVal
     * @return
     */
    public static <T> int replaceAll(final List<T> list, final Object oldVal, final T newVal) {
        if (isNullOrEmpty(list)) {
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

    public static <E extends Exception> int replaceIf(final boolean[] a, final Throwables.BooleanPredicate<E> predicate, final boolean newValue) throws E {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int result = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            if (predicate.test(a[i])) {
                a[i] = newValue;
                result++;
            }
        }

        return result;
    }

    public static <E extends Exception> int replaceIf(final char[] a, final Throwables.CharPredicate<E> predicate, final char newValue) throws E {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int result = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            if (predicate.test(a[i])) {
                a[i] = newValue;
                result++;
            }
        }

        return result;
    }

    public static <E extends Exception> int replaceIf(final byte[] a, final Throwables.BytePredicate<E> predicate, final byte newValue) throws E {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int result = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            if (predicate.test(a[i])) {
                a[i] = newValue;
                result++;
            }
        }

        return result;
    }

    public static <E extends Exception> int replaceIf(final short[] a, final Throwables.ShortPredicate<E> predicate, final short newValue) throws E {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int result = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            if (predicate.test(a[i])) {
                a[i] = newValue;
                result++;
            }
        }

        return result;
    }

    public static <E extends Exception> int replaceIf(final int[] a, final Throwables.IntPredicate<E> predicate, final int newValue) throws E {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int result = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            if (predicate.test(a[i])) {
                a[i] = newValue;
                result++;
            }
        }

        return result;
    }

    public static <E extends Exception> int replaceIf(final long[] a, final Throwables.LongPredicate<E> predicate, final long newValue) throws E {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int result = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            if (predicate.test(a[i])) {
                a[i] = newValue;
                result++;
            }
        }

        return result;
    }

    public static <E extends Exception> int replaceIf(final float[] a, final Throwables.FloatPredicate<E> predicate, final float newValue) throws E {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int result = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            if (predicate.test(a[i])) {
                a[i] = newValue;
                result++;
            }
        }

        return result;
    }

    public static <E extends Exception> int replaceIf(final double[] a, final Throwables.DoublePredicate<E> predicate, final double newValue) throws E {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int result = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            if (predicate.test(a[i])) {
                a[i] = newValue;
                result++;
            }
        }

        return result;
    }

    public static <T, E extends Exception> int replaceIf(final T[] a, final Throwables.Predicate<? super T, E> predicate, final T newValue) throws E {
        if (N.isNullOrEmpty(a)) {
            return 0;
        }

        int result = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            if (predicate.test(a[i])) {
                a[i] = newValue;
                result++;
            }
        }

        return result;
    }

    public static <T, E extends Exception> int replaceIf(final List<T> c, final Throwables.Predicate<? super T, E> predicate, final T newValue) throws E {
        if (N.isNullOrEmpty(c)) {
            return 0;
        }

        int result = 0;

        for (int i = 0, n = c.size(); i < n; i++) {
            if (predicate.test(c.get(i))) {
                c.set(i, newValue);
                result++;
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
        if (isNullOrEmpty(a)) {
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
        if (isNullOrEmpty(a)) {
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
        if (isNullOrEmpty(a)) {
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
        if (isNullOrEmpty(a)) {
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
        if (isNullOrEmpty(a)) {
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
        if (isNullOrEmpty(a)) {
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
        if (isNullOrEmpty(a)) {
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
        if (isNullOrEmpty(a)) {
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
        if (isNullOrEmpty(a)) {
            return asArray(element);
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
     * @param <T>
     * @param a
     * @param element
     * @return A new array containing the existing elements plus the new element
     * @throws NullPointerException if the specified <code>a</code> is <code>null</code>.
     */
    public static <T> T[] add(final T[] a, final T element) {
        checkArgNotNull(a, "a");

        if (isNullOrEmpty(a)) {
            return asArray(element);
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
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_BOOLEAN_ARRAY : b.clone();
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
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_CHAR_ARRAY : b.clone();
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
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_BYTE_ARRAY : b.clone();
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
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_SHORT_ARRAY : b.clone();
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
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_INT_ARRAY : b.clone();
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
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_LONG_ARRAY : b.clone();
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
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_FLOAT_ARRAY : b.clone();
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
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_DOUBLE_ARRAY : b.clone();
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
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? EMPTY_STRING_ARRAY : b.clone();
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
     * @param <T>
     * @param a the first array whose elements are added to the new array.
     * @param b the second array whose elements are added to the new array.
     * @return A new array containing the elements from a and b
     * @throws NullPointerException if the specified <code>a</code> is <code>null</code>.
     */
    @SafeVarargs
    public static <T> T[] addAll(final T[] a, final T... b) {
        checkArgNotNull(a, "a");

        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? b : b.clone();
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
     * @param index the position of the new object
     * @param element the object to add
     * @return A new array containing the existing elements and the new element
     */
    public static boolean[] insert(final boolean[] a, final int index, final boolean element) {
        if (isNullOrEmpty(a) && index == 0) {
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
     * @param index the position of the new object
     * @param element the object to add
     * @return A new array containing the existing elements and the new element
     */
    public static char[] insert(final char[] a, final int index, final char element) {
        if (isNullOrEmpty(a) && index == 0) {
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
     * @param index the position of the new object
     * @param element the object to add
     * @return A new array containing the existing elements and the new element
     */
    public static byte[] insert(final byte[] a, final int index, final byte element) {
        if (isNullOrEmpty(a) && index == 0) {
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
     * @param index the position of the new object
     * @param element the object to add
     * @return A new array containing the existing elements and the new element
     */
    public static short[] insert(final short[] a, final int index, final short element) {
        if (isNullOrEmpty(a) && index == 0) {
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
     * @param index the position of the new object
     * @param element the object to add
     * @return A new array containing the existing elements and the new element
     */
    public static int[] insert(final int[] a, final int index, final int element) {
        if (isNullOrEmpty(a) && index == 0) {
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
     * @param index the position of the new object
     * @param element the object to add
     * @return A new array containing the existing elements and the new element
     */
    public static long[] insert(final long[] a, final int index, final long element) {
        if (isNullOrEmpty(a) && index == 0) {
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
     * @param index the position of the new object
     * @param element the object to add
     * @return A new array containing the existing elements and the new element
     */
    public static float[] insert(final float[] a, final int index, final float element) {
        if (isNullOrEmpty(a) && index == 0) {
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
     * @param index the position of the new object
     * @param element the object to add
     * @return A new array containing the existing elements and the new element
     */
    public static double[] insert(final double[] a, final int index, final double element) {
        if (isNullOrEmpty(a) && index == 0) {
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

    /**
     *
     * @param a
     * @param index
     * @param element
     * @return
     */
    public static String[] insert(final String[] a, final int index, final String element) {
        if (isNullOrEmpty(a) && index == 0) {
            return asArray(element);
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
     * @param <T>
     * @param a
     * @param index the position of the new object
     * @param element the object to add
     * @return A new array containing the existing elements and the new element
     * @throws NullPointerException if the specified <code>a</code> is <code>null</code>.
     */
    public static <T> T[] insert(final T[] a, final int index, final T element) {
        checkArgNotNull(a, "a");

        final T[] newArray = newArray(a.getClass().getComponentType(), a.length + 1);

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
        if (isNullOrEmpty(a) && index == 0) {
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
        if (isNullOrEmpty(a) && index == 0) {
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
        if (isNullOrEmpty(a) && index == 0) {
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
        if (isNullOrEmpty(a) && index == 0) {
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
        if (isNullOrEmpty(a) && index == 0) {
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
        if (isNullOrEmpty(a) && index == 0) {
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
        if (isNullOrEmpty(a) && index == 0) {
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
        if (isNullOrEmpty(a) && index == 0) {
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

    /**
     *
     * @param a
     * @param index
     * @param b
     * @return
     */
    @SafeVarargs
    public static String[] insertAll(final String[] a, final int index, final String... b) {
        if (isNullOrEmpty(a) && index == 0) {
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
     * @param <T>
     * @param a the first array whose elements are added to the new array.
     * @param index the position of the new elements start from
     * @param b the second array whose elements are added to the new array.
     * @return A new array containing the elements from a and b
     * @throws NullPointerException if the specified <code>a</code> is <code>null</code>.
     */
    @SafeVarargs
    public static <T> T[] insertAll(final T[] a, final int index, final T... b) {
        checkArgNotNull(a, "a");

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
     * @param a
     * @param index the position of the element to be removed
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
     * @param a
     * @param index the position of the element to be removed
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
     * @param a
     * @param index the position of the element to be removed
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
     * @param a
     * @param index the position of the element to be removed
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
     * @param a
     * @param index the position of the element to be removed
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
     * @param a
     * @param index the position of the element to be removed
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
     * @param a
     * @param index the position of the element to be removed
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
     * @param a
     * @param index the position of the element to be removed
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
     * @param <T> the component type of the array
     * @param a
     * @param index the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     */
    public static <T> T[] delete(final T[] a, final int index) {
        final T[] result = newArray(a.getClass().getComponentType(), a.length - 1);

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
        if (isNullOrEmpty(indices)) {
            return a.clone();
        } else if (indices.length == 1) {
            return delete(a, indices[0]);
        }

        indices = indices.clone();
        sort(indices);
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
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those at the
     *         specified positions.
     */
    @SafeVarargs
    public static char[] deleteAll(final char[] a, int... indices) {
        if (isNullOrEmpty(indices)) {
            return a.clone();
        } else if (indices.length == 1) {
            return delete(a, indices[0]);
        }

        indices = indices.clone();
        sort(indices);
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
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those at the
     *         specified positions.
     */
    @SafeVarargs
    public static byte[] deleteAll(final byte[] a, int... indices) {
        if (isNullOrEmpty(indices)) {
            return a.clone();
        } else if (indices.length == 1) {
            return delete(a, indices[0]);
        }

        indices = indices.clone();
        sort(indices);
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
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those at the
     *         specified positions.
     */
    @SafeVarargs
    public static short[] deleteAll(final short[] a, int... indices) {
        if (isNullOrEmpty(indices)) {
            return a.clone();
        } else if (indices.length == 1) {
            return delete(a, indices[0]);
        }

        indices = indices.clone();
        sort(indices);
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
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those at the
     *         specified positions.
     * @throws IndexOutOfBoundsException             if any index is out of range (index &lt; 0 || index &gt;=
     *             array.length), or if the array is {@code null}.
     */
    @SafeVarargs
    public static int[] deleteAll(final int[] a, int... indices) {
        if (isNullOrEmpty(indices)) {
            return a.clone();
        } else if (indices.length == 1) {
            return delete(a, indices[0]);
        }

        indices = indices.clone();
        sort(indices);
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
        if (isNullOrEmpty(indices)) {
            return a.clone();
        } else if (indices.length == 1) {
            return delete(a, indices[0]);
        }

        indices = indices.clone();
        sort(indices);
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
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those at the
     *         specified positions.
     */
    @SafeVarargs
    public static float[] deleteAll(final float[] a, int... indices) {
        if (isNullOrEmpty(indices)) {
            return a.clone();
        } else if (indices.length == 1) {
            return delete(a, indices[0]);
        }

        indices = indices.clone();
        sort(indices);
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
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those at the
     *         specified positions.
     */
    @SafeVarargs
    public static double[] deleteAll(final double[] a, int... indices) {
        if (isNullOrEmpty(indices)) {
            return a.clone();
        } else if (indices.length == 1) {
            return delete(a, indices[0]);
        }

        indices = indices.clone();
        sort(indices);
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
     * @param <T> the component type of the array
     * @param a
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those at the
     *         specified positions.
     * @throws NullPointerException if the specified <code>a</code> is <code>null</code>.
     */
    @SafeVarargs
    public static <T> T[] deleteAll(final T[] a, int... indices) {
        checkArgNotNull(a, "a");

        if (isNullOrEmpty(indices)) {
            return a.clone();
        } else if (indices.length == 1) {
            return delete(a, indices[0]);
        }

        indices = indices.clone();
        sort(indices);
        return deleteAllBySortedIndices(a, indices);
    }

    /**
     * Delete all by sorted indices.
     *
     * @param <T>
     * @param a
     * @param indices
     * @return
     */
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

        final T[] result = newArray(a.getClass().getComponentType(), a.length - diff);
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
     * Removes the elements at the specified positions from the specified List.
     *
     * @param list
     * @param indices
     * @return true, if successful
     */
    @SuppressWarnings("rawtypes")
    @SafeVarargs
    public static boolean deleteAll(final List<?> list, int... indices) {
        checkArgNotNull(list);

        if (isNullOrEmpty(indices)) {
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
        checkFromToIndex(fromIndex, toIndex, a.length);

        if (fromIndex == toIndex) {
            return a.clone();
        }

        final boolean[] b = new boolean[a.length - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < a.length) {
            copy(a, toIndex, b, fromIndex, a.length - toIndex);
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
        checkFromToIndex(fromIndex, toIndex, a.length);

        if (fromIndex == toIndex) {
            return a.clone();
        }

        final char[] b = new char[a.length - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < a.length) {
            copy(a, toIndex, b, fromIndex, a.length - toIndex);
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
        checkFromToIndex(fromIndex, toIndex, a.length);

        if (fromIndex == toIndex) {
            return a.clone();
        }

        final byte[] b = new byte[a.length - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < a.length) {
            copy(a, toIndex, b, fromIndex, a.length - toIndex);
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
        checkFromToIndex(fromIndex, toIndex, a.length);

        if (fromIndex == toIndex) {
            return a.clone();
        }

        final short[] b = new short[a.length - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < a.length) {
            copy(a, toIndex, b, fromIndex, a.length - toIndex);
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
        checkFromToIndex(fromIndex, toIndex, a.length);

        if (fromIndex == toIndex) {
            return a.clone();
        }

        final int[] b = new int[a.length - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < a.length) {
            copy(a, toIndex, b, fromIndex, a.length - toIndex);
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
        checkFromToIndex(fromIndex, toIndex, a.length);

        if (fromIndex == toIndex) {
            return a.clone();
        }

        final long[] b = new long[a.length - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < a.length) {
            copy(a, toIndex, b, fromIndex, a.length - toIndex);
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
        checkFromToIndex(fromIndex, toIndex, a.length);

        if (fromIndex == toIndex) {
            return a.clone();
        }

        final float[] b = new float[a.length - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < a.length) {
            copy(a, toIndex, b, fromIndex, a.length - toIndex);
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
        checkFromToIndex(fromIndex, toIndex, a.length);

        if (fromIndex == toIndex) {
            return a.clone();
        }

        final double[] b = new double[a.length - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < a.length) {
            copy(a, toIndex, b, fromIndex, a.length - toIndex);
        }

        return b;
    }

    /**
     * Deletes the values from {@code fromIndex} to {@code toIndex}.
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return a new array
     */
    public static <T> T[] deleteRange(final T[] a, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, a.length);

        if (fromIndex == toIndex) {
            return a.clone();
        }

        final T[] b = Array.newInstance(a.getClass().getComponentType(), a.length - (toIndex - fromIndex));

        if (fromIndex > 0) {
            copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < a.length) {
            copy(a, toIndex, b, fromIndex, a.length - toIndex);
        }

        return b;
    }

    /**
     * Returns {@code true} if the {@code List} is updated when {@code fromIndex < toIndex}, otherwise {@code false} is returned when {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return true, if successful
     */
    @SuppressWarnings("unchecked")
    public static <T> boolean deleteRange(final List<T> c, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, c.size());

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
                    copy(array, toIndex, array, fromIndex, c.size() - toIndex);
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
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static boolean[] remove(final boolean[] a, final boolean element) {
        if (isNullOrEmpty(a)) {
            return EMPTY_BOOLEAN_ARRAY;
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
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static char[] remove(final char[] a, final char element) {
        if (isNullOrEmpty(a)) {
            return EMPTY_CHAR_ARRAY;
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
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static byte[] remove(final byte[] a, final byte element) {
        if (isNullOrEmpty(a)) {
            return EMPTY_BYTE_ARRAY;
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
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static short[] remove(final short[] a, final short element) {
        if (isNullOrEmpty(a)) {
            return EMPTY_SHORT_ARRAY;
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
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static int[] remove(final int[] a, final int element) {
        if (isNullOrEmpty(a)) {
            return EMPTY_INT_ARRAY;
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
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static long[] remove(final long[] a, final long element) {
        if (isNullOrEmpty(a)) {
            return EMPTY_LONG_ARRAY;
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
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static float[] remove(final float[] a, final float element) {
        if (isNullOrEmpty(a)) {
            return EMPTY_FLOAT_ARRAY;
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
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static double[] remove(final double[] a, final double element) {
        if (isNullOrEmpty(a)) {
            return EMPTY_DOUBLE_ARRAY;
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
     * @param <T>
     * @param a
     * @param element the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     */
    public static <T> T[] remove(final T[] a, final T element) {
        if (isNullOrEmpty(a)) {
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
    public static <T> boolean remove(final Collection<T> c, final T element) {
        if (isNullOrEmpty(c)) {
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
        if (isNullOrEmpty(a)) {
            return EMPTY_BOOLEAN_ARRAY;
        }

        final boolean[] copy = a.clone();
        int idx = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == element) {
                continue;
            }

            copy[idx++] = a[i];
        }

        return idx == copy.length ? copy : copyOfRange(copy, 0, idx);
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
        if (isNullOrEmpty(a)) {
            return EMPTY_CHAR_ARRAY;
        }

        final char[] copy = a.clone();
        int idx = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == element) {
                continue;
            }

            copy[idx++] = a[i];
        }

        return idx == copy.length ? copy : copyOfRange(copy, 0, idx);
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
        if (isNullOrEmpty(a)) {
            return EMPTY_BYTE_ARRAY;
        }

        final byte[] copy = a.clone();
        int idx = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == element) {
                continue;
            }

            copy[idx++] = a[i];
        }

        return idx == copy.length ? copy : copyOfRange(copy, 0, idx);
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
        if (isNullOrEmpty(a)) {
            return EMPTY_SHORT_ARRAY;
        }

        final short[] copy = a.clone();
        int idx = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == element) {
                continue;
            }

            copy[idx++] = a[i];
        }

        return idx == copy.length ? copy : copyOfRange(copy, 0, idx);
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
        if (isNullOrEmpty(a)) {
            return EMPTY_INT_ARRAY;
        }

        final int[] copy = a.clone();
        int idx = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == element) {
                continue;
            }

            copy[idx++] = a[i];
        }

        return idx == copy.length ? copy : copyOfRange(copy, 0, idx);
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
        if (isNullOrEmpty(a)) {
            return EMPTY_LONG_ARRAY;
        }

        final long[] copy = a.clone();
        int idx = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (a[i] == element) {
                continue;
            }

            copy[idx++] = a[i];
        }

        return idx == copy.length ? copy : copyOfRange(copy, 0, idx);
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
        if (isNullOrEmpty(a)) {
            return EMPTY_FLOAT_ARRAY;
        }

        final float[] copy = a.clone();
        int idx = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (equals(a[i], element)) {
                continue;
            }

            copy[idx++] = a[i];
        }

        return idx == copy.length ? copy : copyOfRange(copy, 0, idx);
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
        if (isNullOrEmpty(a)) {
            return EMPTY_DOUBLE_ARRAY;
        }

        final double[] copy = a.clone();
        int idx = 0;

        for (int i = 0, len = a.length; i < len; i++) {
            if (equals(a[i], element)) {
                continue;
            }

            copy[idx++] = a[i];
        }

        return idx == copy.length ? copy : copyOfRange(copy, 0, idx);
    }

    /**
     * Removes all the occurrences of the specified element from the specified
     * array. All subsequent elements are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     *
     * @param <T>
     * @param a
     * @param element
     * @return A new array containing the existing elements except the
     *         occurrences of the specified element.
     */
    public static <T> T[] removeAllOccurrences(final T[] a, final T element) {
        if (isNullOrEmpty(a)) {
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

        return idx == copy.length ? copy : copyOfRange(copy, 0, idx);
    }

    /**
     * Removes the all occurrences.
     *
     * @param c
     * @param element
     * @return true, if successful
     */
    public static <T> boolean removeAllOccurrences(final Collection<T> c, final T element) {
        if (isNullOrEmpty(c)) {
            return false;
        }

        return removeAll(c, asSet(element));
    }

    /**
     * Returns a new array with removes all the occurrences of specified elements from <code>a</code>.
     *
     * @param a
     * @param elements
     * @return
     * @see Collection#removeAll(Collection)
     */
    @SafeVarargs
    public static boolean[] removeAll(final boolean[] a, final boolean... elements) {
        if (isNullOrEmpty(a)) {
            return EMPTY_BOOLEAN_ARRAY;
        } else if (isNullOrEmpty(elements)) {
            return a.clone();
        } else if (elements.length == 1) {
            return removeAllOccurrences(a, elements[0]);
        }

        final BooleanList list = BooleanList.of(a.clone());
        list.removeAll(BooleanList.of(elements));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elements from <code>a</code>.
     *
     * @param a
     * @param elements
     * @return
     * @see Collection#removeAll(Collection)
     */
    @SafeVarargs
    public static char[] removeAll(final char[] a, final char... elements) {
        if (isNullOrEmpty(a)) {
            return EMPTY_CHAR_ARRAY;
        } else if (isNullOrEmpty(elements)) {
            return a.clone();
        } else if (elements.length == 1) {
            return removeAllOccurrences(a, elements[0]);
        }

        final CharList list = CharList.of(a.clone());
        list.removeAll(CharList.of(elements));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elements from <code>a</code>.
     *
     * @param a
     * @param elements
     * @return
     * @see Collection#removeAll(Collection)
     */
    @SafeVarargs
    public static byte[] removeAll(final byte[] a, final byte... elements) {
        if (isNullOrEmpty(a)) {
            return EMPTY_BYTE_ARRAY;
        } else if (isNullOrEmpty(elements)) {
            return a.clone();
        } else if (elements.length == 1) {
            return removeAllOccurrences(a, elements[0]);
        }

        final ByteList list = ByteList.of(a.clone());
        list.removeAll(ByteList.of(elements));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elements from <code>a</code>.
     *
     * @param a
     * @param elements
     * @return
     * @see Collection#removeAll(Collection)
     */
    @SafeVarargs
    public static short[] removeAll(final short[] a, final short... elements) {
        if (isNullOrEmpty(a)) {
            return EMPTY_SHORT_ARRAY;
        } else if (isNullOrEmpty(elements)) {
            return a.clone();
        } else if (elements.length == 1) {
            return removeAllOccurrences(a, elements[0]);
        }

        final ShortList list = ShortList.of(a.clone());
        list.removeAll(ShortList.of(elements));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elements from <code>a</code>.
     *
     * @param a
     * @param elements
     * @return
     * @see Collection#removeAll(Collection)
     */
    @SafeVarargs
    public static int[] removeAll(final int[] a, final int... elements) {
        if (isNullOrEmpty(a)) {
            return EMPTY_INT_ARRAY;
        } else if (isNullOrEmpty(elements)) {
            return a.clone();
        } else if (elements.length == 1) {
            return removeAllOccurrences(a, elements[0]);
        }

        final IntList list = IntList.of(a.clone());
        list.removeAll(IntList.of(elements));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elements from <code>a</code>.
     *
     * @param a
     * @param elements
     * @return
     * @see Collection#removeAll(Collection)
     */
    @SafeVarargs
    public static long[] removeAll(final long[] a, final long... elements) {
        if (isNullOrEmpty(a)) {
            return EMPTY_LONG_ARRAY;
        } else if (isNullOrEmpty(elements)) {
            return a.clone();
        } else if (elements.length == 1) {
            return removeAllOccurrences(a, elements[0]);
        }

        final LongList list = LongList.of(a.clone());
        list.removeAll(LongList.of(elements));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elements from <code>a</code>.
     *
     * @param a
     * @param elements
     * @return
     * @see Collection#removeAll(Collection)
     */
    @SafeVarargs
    public static float[] removeAll(final float[] a, final float... elements) {
        if (isNullOrEmpty(a)) {
            return EMPTY_FLOAT_ARRAY;
        } else if (isNullOrEmpty(elements)) {
            return a.clone();
        } else if (elements.length == 1) {
            return removeAllOccurrences(a, elements[0]);
        }

        final FloatList list = FloatList.of(a.clone());
        list.removeAll(FloatList.of(elements));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elements from <code>a</code>.
     *
     * @param a
     * @param elements
     * @return
     * @see Collection#removeAll(Collection)
     */
    @SafeVarargs
    public static double[] removeAll(final double[] a, final double... elements) {
        if (isNullOrEmpty(a)) {
            return EMPTY_DOUBLE_ARRAY;
        } else if (isNullOrEmpty(elements)) {
            return a.clone();
        } else if (elements.length == 1) {
            return removeAllOccurrences(a, elements[0]);
        }

        final DoubleList list = DoubleList.of(a.clone());
        list.removeAll(DoubleList.of(elements));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elements from <code>a</code>.
     *
     * @param <T>
     * @param a
     * @param elements
     * @return
     * @see Collection#removeAll(Collection)
     */
    @SafeVarargs
    public static <T> T[] removeAll(final T[] a, final T... elements) {
        if (isNullOrEmpty(a)) {
            return a;
        } else if (isNullOrEmpty(elements)) {
            return a.clone();
        } else if (elements.length == 1) {
            return removeAllOccurrences(a, elements[0]);
        }

        final Set<Object> set = asSet(elements);
        final List<T> result = new ArrayList<>();

        for (T e : a) {
            if (!set.contains(e)) {
                result.add(e);
            }
        }

        return result.toArray((T[]) newArray(a.getClass().getComponentType(), result.size()));
    }

    /**
     * Removes the all.
     *
     * @param c
     * @param elements
     * @return true, if successful
     */
    @SafeVarargs
    public static <T> boolean removeAll(final Collection<T> c, final T... elements) {
        if (isNullOrEmpty(c) || isNullOrEmpty(elements)) {
            return false;
        } else {
            return removeAll(c, asSet(elements));
        }
    }

    /**
     * Removes the all.
     *
     * @param c
     * @param objsToRemove
     * @return true, if successful
     */
    public static <T> boolean removeAll(final Collection<T> c, final Collection<?> objsToRemove) {
        if (N.isNullOrEmpty(c) || N.isNullOrEmpty(objsToRemove)) {
            return false;
        }

        if (c instanceof HashSet && !(objsToRemove instanceof Set)) {
            boolean result = false;

            for (Object e : objsToRemove) {
                result |= c.remove(e);

                if (c.size() == 0) {
                    break;
                }
            }

            return result;
        } else {
            return c.removeAll(objsToRemove);
        }
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @return
     */
    public static boolean[] removeDuplicates(final boolean[] a) {
        if (isNullOrEmpty(a)) {
            return EMPTY_BOOLEAN_ARRAY;
        }

        return removeDuplicates(a, false);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param isSorted
     * @return
     */
    public static boolean[] removeDuplicates(final boolean[] a, final boolean isSorted) {
        if (isNullOrEmpty(a)) {
            return EMPTY_BOOLEAN_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param from
     * @param to
     * @param isSorted
     * @return
     */
    public static boolean[] removeDuplicates(final boolean[] a, final int from, final int to, final boolean isSorted) {
        checkFromToIndex(from, to, len(a));

        if (isNullOrEmpty(a) && from == 0 && to == 0) {
            return EMPTY_BOOLEAN_ARRAY;
        } else if (to - from <= 1) {
            return copyOfRange(a, from, to);
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

    /**
     * Removes the duplicates.
     *
     * @param a
     * @return
     */
    public static char[] removeDuplicates(final char[] a) {
        if (isNullOrEmpty(a)) {
            return EMPTY_CHAR_ARRAY;
        }

        return removeDuplicates(a, false);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param isSorted
     * @return
     */
    public static char[] removeDuplicates(final char[] a, final boolean isSorted) {
        if (isNullOrEmpty(a)) {
            return EMPTY_CHAR_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param from
     * @param to
     * @param isSorted
     * @return
     */
    public static char[] removeDuplicates(final char[] a, final int from, final int to, final boolean isSorted) {
        checkFromToIndex(from, to, len(a));

        if (isNullOrEmpty(a) && from == 0 && to == 0) {
            return EMPTY_CHAR_ARRAY;
        } else if (to - from <= 1) {
            return copyOfRange(a, from, to);
        }

        if (isSorted) {
            final char[] b = (from == 0 && to == a.length) ? a.clone() : copyOfRange(a, from, to);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (b[i] == b[i - 1]) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : copyOfRange(b, 0, idx);
        } else {
            final Set<Character> set = newLinkedHashSet(initHashCapacity(a.length));

            for (int i = from; i < to; i++) {
                set.add(a[i]);
            }

            if (set.size() == to - from) {
                return (from == 0 && to == a.length) ? a.clone() : copyOfRange(a, from, to);
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

    /**
     * Removes the duplicates.
     *
     * @param a
     * @return
     */
    public static byte[] removeDuplicates(final byte[] a) {
        if (isNullOrEmpty(a)) {
            return EMPTY_BYTE_ARRAY;
        }

        return removeDuplicates(a, false);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param isSorted
     * @return
     */
    public static byte[] removeDuplicates(final byte[] a, final boolean isSorted) {
        if (isNullOrEmpty(a)) {
            return EMPTY_BYTE_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param from
     * @param to
     * @param isSorted
     * @return
     */
    public static byte[] removeDuplicates(final byte[] a, final int from, final int to, final boolean isSorted) {
        checkFromToIndex(from, to, len(a));

        if (isNullOrEmpty(a) && from == 0 && to == 0) {
            return EMPTY_BYTE_ARRAY;
        } else if (to - from <= 1) {
            return copyOfRange(a, from, to);
        }

        if (isSorted) {
            final byte[] b = (from == 0 && to == a.length) ? a.clone() : copyOfRange(a, from, to);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (b[i] == b[i - 1]) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : copyOfRange(b, 0, idx);
        } else {
            final Set<Byte> set = newLinkedHashSet(initHashCapacity(a.length));

            for (int i = from; i < to; i++) {
                set.add(a[i]);
            }

            if (set.size() == to - from) {
                return (from == 0 && to == a.length) ? a.clone() : copyOfRange(a, from, to);
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

    /**
     * Removes the duplicates.
     *
     * @param a
     * @return
     */
    public static short[] removeDuplicates(final short[] a) {
        if (isNullOrEmpty(a)) {
            return EMPTY_SHORT_ARRAY;
        }

        return removeDuplicates(a, false);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param isSorted
     * @return
     */
    public static short[] removeDuplicates(final short[] a, final boolean isSorted) {
        if (isNullOrEmpty(a)) {
            return EMPTY_SHORT_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param from
     * @param to
     * @param isSorted
     * @return
     */
    public static short[] removeDuplicates(final short[] a, final int from, final int to, final boolean isSorted) {
        checkFromToIndex(from, to, len(a));

        if (isNullOrEmpty(a) && from == 0 && to == 0) {
            return EMPTY_SHORT_ARRAY;
        } else if (to - from <= 1) {
            return copyOfRange(a, from, to);
        }

        if (isSorted) {
            final short[] b = (from == 0 && to == a.length) ? a.clone() : copyOfRange(a, from, to);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (b[i] == b[i - 1]) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : copyOfRange(b, 0, idx);
        } else {
            final Set<Short> set = newLinkedHashSet(initHashCapacity(a.length));

            for (int i = from; i < to; i++) {
                set.add(a[i]);
            }

            if (set.size() == to - from) {
                return (from == 0 && to == a.length) ? a.clone() : copyOfRange(a, from, to);
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

    /**
     * Removes the duplicates.
     *
     * @param a
     * @return
     */
    public static int[] removeDuplicates(final int[] a) {
        if (isNullOrEmpty(a)) {
            return EMPTY_INT_ARRAY;
        }

        return removeDuplicates(a, false);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param isSorted
     * @return
     */
    public static int[] removeDuplicates(final int[] a, final boolean isSorted) {
        if (isNullOrEmpty(a)) {
            return EMPTY_INT_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param from
     * @param to
     * @param isSorted
     * @return
     */
    public static int[] removeDuplicates(final int[] a, final int from, final int to, final boolean isSorted) {
        checkFromToIndex(from, to, len(a));

        if (isNullOrEmpty(a) && from == 0 && to == 0) {
            return EMPTY_INT_ARRAY;
        } else if (to - from <= 1) {
            return copyOfRange(a, from, to);
        }

        if (isSorted) {
            final int[] b = (from == 0 && to == a.length) ? a.clone() : copyOfRange(a, from, to);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (b[i] == b[i - 1]) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : copyOfRange(b, 0, idx);
        } else {

            final Set<Integer> set = newLinkedHashSet(initHashCapacity(a.length));

            for (int i = from; i < to; i++) {
                set.add(a[i]);
            }

            if (set.size() == to - from) {
                return (from == 0 && to == a.length) ? a.clone() : copyOfRange(a, from, to);
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

    /**
     * Removes the duplicates.
     *
     * @param a
     * @return
     */
    public static long[] removeDuplicates(final long[] a) {
        if (isNullOrEmpty(a)) {
            return EMPTY_LONG_ARRAY;
        }

        return removeDuplicates(a, false);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param isSorted
     * @return
     */
    public static long[] removeDuplicates(final long[] a, final boolean isSorted) {
        if (isNullOrEmpty(a)) {
            return EMPTY_LONG_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param from
     * @param to
     * @param isSorted
     * @return
     */
    public static long[] removeDuplicates(final long[] a, final int from, final int to, final boolean isSorted) {
        checkFromToIndex(from, to, len(a));

        if (isNullOrEmpty(a) && from == 0 && to == 0) {
            return EMPTY_LONG_ARRAY;
        } else if (to - from <= 1) {
            return copyOfRange(a, from, to);
        }

        if (isSorted) {
            final long[] b = (from == 0 && to == a.length) ? a.clone() : copyOfRange(a, from, to);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (b[i] == b[i - 1]) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : copyOfRange(b, 0, idx);
        } else {
            final Set<Long> set = newLinkedHashSet(initHashCapacity(a.length));

            for (int i = from; i < to; i++) {
                set.add(a[i]);
            }

            if (set.size() == to - from) {
                return (from == 0 && to == a.length) ? a.clone() : copyOfRange(a, from, to);
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

    /**
     * Removes the duplicates.
     *
     * @param a
     * @return
     */
    public static float[] removeDuplicates(final float[] a) {
        if (isNullOrEmpty(a)) {
            return EMPTY_FLOAT_ARRAY;
        }

        return removeDuplicates(a, false);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param isSorted
     * @return
     */
    public static float[] removeDuplicates(final float[] a, final boolean isSorted) {
        if (isNullOrEmpty(a)) {
            return EMPTY_FLOAT_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param from
     * @param to
     * @param isSorted
     * @return
     */
    public static float[] removeDuplicates(final float[] a, final int from, final int to, final boolean isSorted) {
        checkFromToIndex(from, to, len(a));

        if (isNullOrEmpty(a) && from == 0 && to == 0) {
            return EMPTY_FLOAT_ARRAY;
        } else if (to - from <= 1) {
            return copyOfRange(a, from, to);
        }

        if (isSorted) {
            final float[] b = (from == 0 && to == a.length) ? a.clone() : copyOfRange(a, from, to);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (equals(b[i], b[i - 1])) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : copyOfRange(b, 0, idx);
        } else {

            final Set<Float> set = newLinkedHashSet(initHashCapacity(a.length));

            for (int i = from; i < to; i++) {
                set.add(a[i]);
            }

            if (set.size() == to - from) {
                return (from == 0 && to == a.length) ? a.clone() : copyOfRange(a, from, to);
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

    /**
     * Removes the duplicates.
     *
     * @param a
     * @return
     */
    public static double[] removeDuplicates(final double[] a) {
        if (isNullOrEmpty(a)) {
            return EMPTY_DOUBLE_ARRAY;
        }

        return removeDuplicates(a, false);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param isSorted
     * @return
     */
    public static double[] removeDuplicates(final double[] a, final boolean isSorted) {
        if (isNullOrEmpty(a)) {
            return EMPTY_DOUBLE_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param from
     * @param to
     * @param isSorted
     * @return
     */
    public static double[] removeDuplicates(final double[] a, final int from, final int to, final boolean isSorted) {
        checkFromToIndex(from, to, len(a));

        if (isNullOrEmpty(a) && from == 0 && to == 0) {
            return EMPTY_DOUBLE_ARRAY;
        } else if (to - from <= 1) {
            return copyOfRange(a, from, to);
        }

        if (isSorted) {
            final double[] b = (from == 0 && to == a.length) ? a.clone() : copyOfRange(a, from, to);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (equals(b[i], b[i - 1])) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : copyOfRange(b, 0, idx);
        } else {
            final Set<Double> set = newLinkedHashSet(initHashCapacity(a.length));

            for (int i = from; i < to; i++) {
                set.add(a[i]);
            }

            if (set.size() == to - from) {
                return (from == 0 && to == a.length) ? a.clone() : copyOfRange(a, from, to);
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
     * @param <T> the component type of the array
     * @param a
     * @return A new array containing the existing elements except the duplicates
     * @throws NullPointerException if the specified array <code>a</code> is null.
     */
    public static <T> T[] removeDuplicates(final T[] a) {
        if (isNullOrEmpty(a)) {
            return a;
        }

        return removeDuplicates(a, false);
    }

    /**
     * Removes the duplicates.
     *
     * @param <T>
     * @param a
     * @param isSorted
     * @return
     */
    public static <T> T[] removeDuplicates(final T[] a, final boolean isSorted) {
        if (isNullOrEmpty(a)) {
            return a;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Removes the duplicates.
     *
     * @param <T>
     * @param a
     * @param from
     * @param to
     * @param isSorted
     * @return
     */
    public static <T> T[] removeDuplicates(final T[] a, final int from, final int to, final boolean isSorted) {
        checkFromToIndex(from, to, len(a));

        if (isNullOrEmpty(a) && from == 0 && to == 0) {
            return a;
        } else if (to - from <= 1) {
            return copyOfRange(a, from, to);
        }

        if (isSorted) {
            final T[] b = (from == 0 && to == a.length) ? a.clone() : copyOfRange(a, from, to);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (equals(b[i], b[i - 1])) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : copyOfRange(b, 0, idx);
        } else {
            final List<T> list = distinct(a, from, to);
            return list.toArray((T[]) newArray(a.getClass().getComponentType(), list.size()));
        }
    }

    /**
     * Removes the duplicates.
     *
     * @param c
     * @return <code>true</code> if there is one or more duplicated elements are removed. otherwise <code>false</code> is returned.
     */
    public static boolean removeDuplicates(final Collection<?> c) {
        if (isNullOrEmpty(c) || c.size() == 1) {
            return false;
        }

        return removeDuplicates(c, false);
    }

    /**
     * Removes the duplicates.
     *
     * @param c
     * @param isSorted
     * @return <code>true</code> if there is one or more duplicated elements are removed. otherwise <code>false</code> is returned.
     */
    @SuppressWarnings("rawtypes")
    public static boolean removeDuplicates(final Collection<?> c, final boolean isSorted) {
        if (isNullOrEmpty(c) || c.size() == 1) {
            return false;
        }

        if (isSorted) {
            boolean hasDuplicates = false;
            final Iterator<?> it = c.iterator();
            Object pre = it.next();
            Object next = null;
            while (it.hasNext()) {
                next = it.next();
                if (equals(next, pre)) {
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

    /**
     * Checks for duplicates.
     *
     * @param a
     * @return true, if successful
     */
    public static boolean hasDuplicates(final char[] a) {
        return hasDuplicates(a, false);
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @param isSorted
     * @return true, if successful
     */
    public static boolean hasDuplicates(final char[] a, final boolean isSorted) {
        if (isNullOrEmpty(a)) {
            return false;
        }

        return hasDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param isSorted
     * @return true, if successful
     */
    static boolean hasDuplicates(final char[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, a.length);

        if (isNullOrEmpty(a) || toIndex - fromIndex < 2) {
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
            final Set<Character> set = newHashSet(initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                if (set.add(a[i]) == false) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @return true, if successful
     */
    public static boolean hasDuplicates(final byte[] a) {
        return hasDuplicates(a, false);
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @param isSorted
     * @return true, if successful
     */
    public static boolean hasDuplicates(final byte[] a, final boolean isSorted) {
        if (isNullOrEmpty(a)) {
            return false;
        }

        return hasDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param isSorted
     * @return true, if successful
     */
    static boolean hasDuplicates(final byte[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, a.length);

        if (isNullOrEmpty(a) || toIndex - fromIndex < 2) {
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
            final Set<Byte> set = newHashSet(initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                if (set.add(a[i]) == false) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @return true, if successful
     */
    public static boolean hasDuplicates(final short[] a) {
        return hasDuplicates(a, false);
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @param isSorted
     * @return true, if successful
     */
    public static boolean hasDuplicates(final short[] a, final boolean isSorted) {
        if (isNullOrEmpty(a)) {
            return false;
        }

        return hasDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param isSorted
     * @return true, if successful
     */
    static boolean hasDuplicates(final short[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, a.length);

        if (isNullOrEmpty(a) || toIndex - fromIndex < 2) {
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
            final Set<Short> set = newHashSet(initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                if (set.add(a[i]) == false) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @return true, if successful
     */
    public static boolean hasDuplicates(final int[] a) {
        return hasDuplicates(a, false);
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @param isSorted
     * @return true, if successful
     */
    public static boolean hasDuplicates(final int[] a, final boolean isSorted) {
        if (isNullOrEmpty(a)) {
            return false;
        }

        return hasDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param isSorted
     * @return true, if successful
     */
    static boolean hasDuplicates(final int[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, a.length);

        if (isNullOrEmpty(a) || toIndex - fromIndex < 2) {
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
            final Set<Integer> set = newHashSet(initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                if (set.add(a[i]) == false) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @return true, if successful
     */
    public static boolean hasDuplicates(final long[] a) {
        return hasDuplicates(a, false);
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @param isSorted
     * @return true, if successful
     */
    public static boolean hasDuplicates(final long[] a, final boolean isSorted) {
        if (isNullOrEmpty(a)) {
            return false;
        }

        return hasDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param isSorted
     * @return true, if successful
     */
    static boolean hasDuplicates(final long[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, a.length);

        if (isNullOrEmpty(a) || toIndex - fromIndex < 2) {
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
            final Set<Long> set = newHashSet(initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                if (set.add(a[i]) == false) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @return true, if successful
     */
    public static boolean hasDuplicates(final float[] a) {
        return hasDuplicates(a, false);
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @param isSorted
     * @return true, if successful
     */
    public static boolean hasDuplicates(final float[] a, final boolean isSorted) {
        if (isNullOrEmpty(a)) {
            return false;
        }

        return hasDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param isSorted
     * @return true, if successful
     */
    static boolean hasDuplicates(final float[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, a.length);

        if (isNullOrEmpty(a) || toIndex - fromIndex < 2) {
            return false;
        } else if (toIndex - fromIndex == 2) {
            return equals(a[fromIndex], a[fromIndex + 1]);
        } else if (toIndex - fromIndex == 3) {
            return equals(a[fromIndex], a[fromIndex + 1]) || equals(a[fromIndex], a[fromIndex + 2]) || equals(a[fromIndex + 1], a[fromIndex + 2]);
        }

        if (isSorted) {
            for (int i = fromIndex + 1; i < toIndex; i++) {
                if (equals(a[i], a[i - 1])) {
                    return true;
                }
            }

            return false;
        } else {
            final Set<Float> set = newHashSet(initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                if (set.add(a[i]) == false) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @return true, if successful
     */
    public static boolean hasDuplicates(final double[] a) {
        return hasDuplicates(a, false);
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @param isSorted
     * @return true, if successful
     */
    public static boolean hasDuplicates(final double[] a, final boolean isSorted) {
        if (isNullOrEmpty(a)) {
            return false;
        }

        return hasDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param isSorted
     * @return true, if successful
     */
    static boolean hasDuplicates(final double[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, a.length);

        if (isNullOrEmpty(a) || toIndex - fromIndex < 2) {
            return false;
        } else if (toIndex - fromIndex == 2) {
            return equals(a[fromIndex], a[fromIndex + 1]);
        } else if (toIndex - fromIndex == 3) {
            return equals(a[fromIndex], a[fromIndex + 1]) || equals(a[fromIndex], a[fromIndex + 2]) || equals(a[fromIndex + 1], a[fromIndex + 2]);
        }

        if (isSorted) {
            for (int i = fromIndex + 1; i < toIndex; i++) {
                if (equals(a[i], a[i - 1])) {
                    return true;
                }
            }

            return false;
        } else {
            final Set<Double> set = newHashSet(initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                if (set.add(a[i]) == false) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Checks for duplicates.
     *
     * @param <T>
     * @param a
     * @return true, if successful
     */
    public static <T> boolean hasDuplicates(final T[] a) {
        return hasDuplicates(a, false);
    }

    /**
     * Checks for duplicates.
     *
     * @param <T>
     * @param a
     * @param isSorted
     * @return true, if successful
     */
    public static <T> boolean hasDuplicates(final T[] a, final boolean isSorted) {
        if (isNullOrEmpty(a)) {
            return false;
        }

        return hasDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Checks for duplicates.
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param isSorted
     * @return true, if successful
     */
    static <T> boolean hasDuplicates(final T[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, a.length);

        if (isNullOrEmpty(a) || toIndex - fromIndex < 2) {
            return false;
        } else if (toIndex - fromIndex == 2) {
            return equals(a[fromIndex], a[fromIndex + 1]);
        } else if (toIndex - fromIndex == 3) {
            return equals(a[fromIndex], a[fromIndex + 1]) || equals(a[fromIndex], a[fromIndex + 2]) || equals(a[fromIndex + 1], a[fromIndex + 2]);
        }

        if (isSorted) {
            for (int i = fromIndex + 1; i < toIndex; i++) {
                if (equals(a[i], a[i - 1])) {
                    return true;
                }
            }

            return false;
        } else {
            final Set<Object> set = newHashSet(initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                if (set.add(hashKey(a[i])) == false) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Checks for duplicates.
     *
     * @param c
     * @return true, if successful
     */
    public static boolean hasDuplicates(final Collection<?> c) {
        return hasDuplicates(c, false);
    }

    /**
     * Checks for duplicates.
     *
     * @param c
     * @param isSorted
     * @return true, if successful
     */
    public static boolean hasDuplicates(final Collection<?> c, final boolean isSorted) {
        if (isNullOrEmpty(c) || c.size() == 1) {
            return false;
        }

        if (isSorted) {
            final Iterator<?> it = c.iterator();
            Object pre = it.next();
            Object next = null;
            while (it.hasNext()) {
                next = it.next();

                if (equals(next, pre)) {
                    return true;
                }

                pre = next;
            }

            return false;
        } else {
            final Set<Object> set = newHashSet(initHashCapacity(c.size()));

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
     * @param c
     * @param objsToKeep
     * @return true, if successful
     */
    public static <T> boolean retainAll(final Collection<T> c, final Collection<? extends T> objsToKeep) {
        if (N.isNullOrEmpty(c)) {
            return false;
        } else if (N.isNullOrEmpty(objsToKeep)) {
            c.clear();
            return true;
        }

        if (c instanceof HashSet && !(objsToKeep instanceof Set) && (c.size() > 9 || objsToKeep.size() > 9)) {
            return c.retainAll(N.newHashSet(objsToKeep));
        } else {
            return c.retainAll(objsToKeep);
        }
    }

    /**
     *
     * @param obj
     * @return
     */
    static Object hashKey(Object obj) {
        return obj == null || obj.getClass().isArray() == false ? obj : Wrapper.of(obj);
    }

    /**
     *
     * @param a
     * @return a long number
     */
    @SafeVarargs
    public static int sum(final char... a) {
        if (isNullOrEmpty(a)) {
            return 0;
        }

        return sum(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static int sum(final char[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (isNullOrEmpty(a)) {
            if (to > 0) {
                throw new IndexOutOfBoundsException();
            }

            return 0;
        }

        int sum = 0;

        for (int i = from; i < to; i++) {
            sum += a[i];
        }

        return sum;
    }

    /**
     *
     * @param a
     * @return a long number
     */
    @SafeVarargs
    public static int sum(final byte... a) {
        if (isNullOrEmpty(a)) {
            return 0;
        }

        return sum(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static int sum(final byte[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (isNullOrEmpty(a)) {
            if (to > 0) {
                throw new IndexOutOfBoundsException();
            }

            return 0;
        }

        int sum = 0;

        for (int i = from; i < to; i++) {
            sum += a[i];
        }

        return sum;
    }

    /**
     *
     * @param a
     * @return a long number
     */
    @SafeVarargs
    public static int sum(final short... a) {
        if (isNullOrEmpty(a)) {
            return 0;
        }

        return sum(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static int sum(final short[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (isNullOrEmpty(a)) {
            if (to > 0) {
                throw new IndexOutOfBoundsException();
            }

            return 0;
        }

        int sum = 0;

        for (int i = from; i < to; i++) {
            sum += a[i];
        }

        return sum;
    }

    /**
     *
     * @param a
     * @return a long number
     */
    @SafeVarargs
    public static int sum(final int... a) {
        if (isNullOrEmpty(a)) {
            return 0;
        }

        return sum(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static int sum(final int[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (isNullOrEmpty(a)) {
            if (to > 0) {
                throw new IndexOutOfBoundsException();
            }

            return 0;
        }

        int sum = 0;

        for (int i = from; i < to; i++) {
            sum += a[i];
        }

        return sum;
    }

    /**
     *
     * @param a
     * @return a long number
     */
    @SafeVarargs
    public static long sum(final long... a) {
        if (isNullOrEmpty(a)) {
            return 0L;
        }

        return sum(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static long sum(final long[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (isNullOrEmpty(a)) {
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
        if (isNullOrEmpty(a)) {
            return 0f;
        }

        return sum(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static float sum(final float[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (isNullOrEmpty(a)) {
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
        if (isNullOrEmpty(a)) {
            return 0d;
        }

        return sum(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static double sum(final double[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (isNullOrEmpty(a)) {
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
        if (isNullOrEmpty(a)) {
            return 0d;
        }

        return average(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static double average(final char[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (isNullOrEmpty(a)) {
            if (to > 0) {
                throw new IndexOutOfBoundsException();
            }

            return 0d;
        } else if (from == to) {
            return 0d;
        }

        return ((double) sum(a, from, to)) / (to - from);
    }

    /**
     *
     * @param a
     * @return a double number
     */
    @SafeVarargs
    public static double average(final byte... a) {
        if (isNullOrEmpty(a)) {
            return 0d;
        }

        return average(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static double average(final byte[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (isNullOrEmpty(a)) {
            if (to > 0) {
                throw new IndexOutOfBoundsException();
            }

            return 0d;
        } else if (from == to) {
            return 0d;
        }

        return ((double) sum(a, from, to)) / (to - from);
    }

    /**
     *
     * @param a
     * @return a double number
     */
    @SafeVarargs
    public static double average(final short... a) {
        if (isNullOrEmpty(a)) {
            return 0d;
        }

        return average(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static double average(final short[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (isNullOrEmpty(a)) {
            if (to > 0) {
                throw new IndexOutOfBoundsException();
            }

            return 0d;
        } else if (from == to) {
            return 0d;
        }

        return ((double) sum(a, from, to)) / (to - from);
    }

    /**
     *
     * @param a
     * @return a double number
     */
    @SafeVarargs
    public static double average(final int... a) {
        if (isNullOrEmpty(a)) {
            return 0d;
        }

        return average(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static double average(final int[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (isNullOrEmpty(a)) {
            if (to > 0) {
                throw new IndexOutOfBoundsException();
            }

            return 0d;
        } else if (from == to) {
            return 0d;
        }

        return ((double) sum(a, from, to)) / (to - from);
    }

    /**
     *
     * @param a
     * @return a double number
     */
    @SafeVarargs
    public static double average(final long... a) {
        if (isNullOrEmpty(a)) {
            return 0d;
        }

        return average(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static double average(final long[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (isNullOrEmpty(a)) {
            if (to > 0) {
                throw new IndexOutOfBoundsException();
            }

            return 0d;
        } else if (from == to) {
            return 0d;
        }

        return ((double) sum(a, from, to)) / (to - from);
    }

    /**
     *
     * @param a
     * @return a double number
     */
    @SafeVarargs
    public static double average(final float... a) {
        if (isNullOrEmpty(a)) {
            return 0d;
        }

        return average(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static double average(final float[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (isNullOrEmpty(a)) {
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
        if (isNullOrEmpty(a)) {
            return 0d;
        }

        return average(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static double average(final double[] a, final int from, final int to) {
        checkFromToIndex(from, to, len(a));

        if (isNullOrEmpty(a)) {
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
     * @return
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
     * @return
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
     * @return
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
     * @return
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
     * @return
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
     * @return
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
     * @return
     */
    public static double min(final double a, final double b) {
        return Math.min(a, b);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    public static <T extends Comparable<? super T>> T min(final T a, final T b) {
        return (T) min(a, b, NULL_MAX_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @param cmp
     * @return
     */
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

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static <T extends Comparable<? super T>> T min(final T a, final T b, final T c) {
        return (T) min(a, b, c, NULL_MAX_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @param c
     * @param cmp
     * @return
     */
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
     * @return
     */
    @SafeVarargs
    public static char min(final char... a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        if (isNullOrEmpty(a)) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        return min(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static char min(final char[] a, final int from, final int to) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

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
     * @return
     */
    @SafeVarargs
    public static byte min(final byte... a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return min(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static byte min(final byte[] a, final int from, final int to) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

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
     * @return
     */
    @SafeVarargs
    public static short min(final short... a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return min(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static short min(final short[] a, final int from, final int to) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

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
     * @return
     */
    @SafeVarargs
    public static int min(final int... a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return min(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static int min(final int[] a, final int from, final int to) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

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
     * @return
     */
    @SafeVarargs
    public static long min(final long... a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return min(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static long min(final long[] a, final int from, final int to) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

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
     * @return
     * @see IEEE754rUtil#min(float[]) IEEE754rUtils for a version of this method
     *      that handles NaN differently
     */
    @SafeVarargs
    public static float min(final float... a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return min(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static float min(final float[] a, final int from, final int to) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

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
     * @return
     * @see IEEE754rUtil#min(double[]) IEEE754rUtils for a version of this
     *      method that handles NaN differently
     */
    @SafeVarargs
    public static double min(final double... a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return min(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static double min(final double[] a, final int from, final int to) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

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
     * @param <T>
     * @param a an array, must not be null or empty
     * @return
     */
    public static <T extends Comparable<? super T>> T min(final T[] a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return min(a, 0, a.length);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static <T extends Comparable<? super T>> T min(final T[] a, final int from, final int to) {
        return (T) min(a, from, to, NULL_MAX_COMPARATOR);
    }

    /**
     * Returns the minimum element in the array.
     *
     * @param <T>
     * @param a an array, must not be null or empty
     * @param cmp
     * @return
     */
    public static <T> T min(final T[] a, final Comparator<? super T> cmp) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return min(a, 0, a.length, cmp);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param from
     * @param to
     * @param cmp
     * @return
     */
    public static <T> T min(final T[] a, final int from, final int to, Comparator<? super T> cmp) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
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

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Comparable<? super T>> T min(final Collection<? extends T> c) {
        checkArgNotNullOrEmpty(c, "The spcified collection can not be null or empty");

        return min(c, 0, c.size());
    }

    /**
     *
     * @param <T>
     * @param c
     * @param from
     * @param to
     * @return
     */
    public static <T extends Comparable<? super T>> T min(final Collection<? extends T> c, final int from, final int to) {
        checkArgNotNullOrEmpty(c, "The spcified collection can not be null or empty");

        return (T) min(c, from, to, NULL_MAX_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param cmp
     * @return
     */
    public static <T> T min(final Collection<? extends T> c, Comparator<? super T> cmp) {
        checkArgNotNullOrEmpty(c, "The spcified collection can not be null or empty");

        return min(c, 0, c.size(), cmp);
    }

    /**
     * Returns the minimum element in the collection.
     *
     * @param <T>
     * @param c
     * @param from
     * @param to
     * @param cmp
     * @return
     */
    public static <T> T min(final Collection<? extends T> c, final int from, final int to, Comparator<? super T> cmp) {
        checkFromToIndex(from, to, size(c));

        if (isNullOrEmpty(c) || to - from < 1 || from >= c.size()) {
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

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T extends Comparable<? super T>> List<T> minAll(final T[] a) {
        return minAll(a, NULL_MAX_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param cmp
     * @return
     */
    public static <T> List<T> minAll(final T[] a, Comparator<? super T> cmp) {
        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Comparable<? super T>> List<T> minAll(final Collection<T> c) {
        return minAll(c, NULL_MAX_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param cmp
     * @return
     */
    public static <T> List<T> minAll(final Collection<T> c, Comparator<? super T> cmp) {
        if (isNullOrEmpty(c)) {
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
     * @return
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
     * @return
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
     * @return
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
     * @return
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
     * @return
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
     * @return
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
     * @return
     */
    public static double max(final double a, final double b) {
        return Math.max(a, b);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    public static <T extends Comparable<? super T>> T max(final T a, final T b) {
        return (T) max(a, b, NULL_MIN_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @param cmp
     * @return
     */
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

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static <T extends Comparable<? super T>> T max(final T a, final T b, final T c) {
        return (T) max(a, b, c, NULL_MIN_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param b
     * @param c
     * @param cmp
     * @return
     */
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
     * @return
     */
    @SafeVarargs
    public static char max(final char... a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return max(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static char max(final char[] a, final int from, final int to) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

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
     * @return
     */
    @SafeVarargs
    public static byte max(final byte... a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return max(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static byte max(final byte[] a, final int from, final int to) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

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
     * @return
     */
    @SafeVarargs
    public static short max(final short... a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return max(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static short max(final short[] a, final int from, final int to) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

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
     * @return
     */
    @SafeVarargs
    public static int max(final int... a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return max(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static int max(final int[] a, final int from, final int to) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

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
     * @return
     */
    @SafeVarargs
    public static long max(final long... a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return max(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static long max(final long[] a, final int from, final int to) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

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
     * @return
     * @see IEEE754rUtil#max(float[]) IEEE754rUtils for a version of this method
     *      that handles NaN differently
     */
    @SafeVarargs
    public static float max(final float... a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return max(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static float max(final float[] a, final int from, final int to) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

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
     * @return
     * @see IEEE754rUtil#max(double[]) IEEE754rUtils for a version of this
     *      method that handles NaN differently
     */
    @SafeVarargs
    public static double max(final double... a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return max(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static double max(final double[] a, final int from, final int to) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

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
     * @param <T>
     * @param a an array, must not be null or empty
     * @return
     * @throws IllegalArgumentException             if <code>a</code> is <code>null</code> or empty.
     */
    public static <T extends Comparable<? super T>> T max(final T[] a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return max(a, 0, a.length);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static <T extends Comparable<? super T>> T max(final T[] a, final int from, final int to) {
        return (T) max(a, from, to, NULL_MIN_COMPARATOR);
    }

    /**
     * Returns the maximum element in the array.
     *
     * @param <T>
     * @param a an array, must not be null or empty
     * @param cmp
     * @return
     */
    public static <T> T max(final T[] a, final Comparator<? super T> cmp) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return max(a, 0, a.length, cmp);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param from
     * @param to
     * @param cmp
     * @return
     */
    public static <T> T max(final T[] a, final int from, final int to, Comparator<? super T> cmp) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
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

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Comparable<? super T>> T max(final Collection<? extends T> c) {
        checkArgNotNullOrEmpty(c, "The spcified collection can not be null or empty");

        return max(c, 0, c.size());
    }

    /**
     *
     * @param <T>
     * @param c
     * @param from
     * @param to
     * @return
     */
    public static <T extends Comparable<? super T>> T max(final Collection<? extends T> c, final int from, final int to) {
        checkArgNotNullOrEmpty(c, "The spcified collection can not be null or empty");

        return (T) max(c, from, to, NULL_MIN_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param cmp
     * @return
     */
    public static <T> T max(final Collection<? extends T> c, Comparator<? super T> cmp) {
        checkArgNotNullOrEmpty(c, "The spcified collection can not be null or empty");

        return max(c, 0, c.size(), cmp);
    }

    /**
     * Returns the maximum element in the collection.
     *
     * @param <T>
     * @param c
     * @param from
     * @param to
     * @param cmp
     * @return
     */
    public static <T> T max(final Collection<? extends T> c, final int from, final int to, Comparator<? super T> cmp) {
        checkFromToIndex(from, to, size(c));

        if (isNullOrEmpty(c) || to - from < 1 || from >= c.size()) {
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

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T extends Comparable<? super T>> List<T> maxAll(final T[] a) {
        return maxAll(a, NULL_MIN_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param cmp
     * @return
     */
    public static <T> List<T> maxAll(final T[] a, Comparator<? super T> cmp) {
        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Comparable<? super T>> List<T> maxAll(final Collection<T> c) {
        return maxAll(c, NULL_MIN_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param cmp
     * @return
     */
    public static <T> List<T> maxAll(final Collection<T> c, Comparator<? super T> cmp) {
        if (isNullOrEmpty(c)) {
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
     * @return
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
     * @return
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
     * @return
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
     * @return
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
     * @return
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
     * @return
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
     * @return
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
     * @param <T>
     * @param a
     * @param b
     * @param c
     * @return
     * @see #median(int...)
     */
    public static <T extends Comparable<? super T>> T median(final T a, final T b, final T c) {
        return (T) median(a, b, c, NATURAL_ORDER);
    }

    /**
     * Gets the median of three values.
     *
     * @param <T>
     * @param a
     * @param b
     * @param c
     * @param cmp
     * @return
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
     * @return
     * @see #median(int...)
     */
    @SafeVarargs
    public static char median(final char... a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return median(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static char median(final char[] a, final int from, final int to) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
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
     * @return
     * @see #median(int...)
     */
    @SafeVarargs
    public static byte median(final byte... a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return median(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static byte median(final byte[] a, final int from, final int to) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
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
     * @return
     * @see #median(int...)
     */
    @SafeVarargs
    public static short median(final short... a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return median(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static short median(final short[] a, final int from, final int to) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
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
     * @return
     * @see #median(int...)
     */
    @SafeVarargs
    public static int median(final int... a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return median(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static int median(final int[] a, final int from, final int to) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
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
     * @return
     * @see #median(int...)
     */
    @SafeVarargs
    public static long median(final long... a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return median(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static long median(final long[] a, final int from, final int to) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
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
     * @return
     * @see #median(int...)
     */
    @SafeVarargs
    public static float median(final float... a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return median(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static float median(final float[] a, final int from, final int to) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
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
     * @return
     * @see #median(int...)
     */
    @SafeVarargs
    public static double median(final double... a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return median(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static double median(final double[] a, final int from, final int to) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
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
     * @param <T>
     * @param a an array, must not be null or empty
     * @return
     * @see #median(int...)
     */
    public static <T extends Comparable<? super T>> T median(final T[] a) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return median(a, 0, a.length);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param from
     * @param to
     * @return
     */
    public static <T extends Comparable<? super T>> T median(final T[] a, final int from, final int to) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        return (T) median(a, from, to, NATURAL_ORDER);
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param <T>
     * @param a an array, must not be null or empty
     * @param cmp
     * @return
     * @see #median(int...)
     */
    public static <T> T median(final T[] a, Comparator<? super T> cmp) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return median(a, 0, a.length, cmp);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param from
     * @param to
     * @param cmp
     * @return
     */
    public static <T> T median(final T[] a, final int from, final int to, Comparator<? super T> cmp) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        checkFromToIndex(from, to, a.length);

        cmp = cmp == null ? NATURAL_ORDER : cmp;

        final int len = to - from;

        return kthLargest(a, from, to, len / 2 + 1, cmp);
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param <T>
     * @param c
     * @return
     * @see #median(int...)
     */
    public static <T extends Comparable<? super T>> T median(final Collection<? extends T> c) {
        checkArgNotNullOrEmpty(c, "The spcified collection can not be null or empty");

        return median(c, 0, c.size());
    }

    /**
     *
     * @param <T>
     * @param c
     * @param from
     * @param to
     * @return
     */
    public static <T extends Comparable<? super T>> T median(final Collection<? extends T> c, final int from, final int to) {
        return (T) median(c, from, to, NATURAL_ORDER);
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param <T>
     * @param c
     * @param cmp
     * @return
     * @see #median(int...)
     */
    public static <T> T median(final Collection<? extends T> c, Comparator<? super T> cmp) {
        checkArgNotNullOrEmpty(c, "The spcified collection can not be null or empty");

        return median(c, 0, c.size(), cmp);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param from
     * @param to
     * @param cmp
     * @return
     */
    public static <T> T median(final Collection<? extends T> c, final int from, final int to, Comparator<? super T> cmp) {
        if (isNullOrEmpty(c) || to - from < 1) {
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
     * @return
     * @throws IllegalArgumentException if the length of the specified array is less than <code>k</code>.
     */
    public static char kthLargest(final char[] a, final int k) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return Array.kthLargest(a, k);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @param k
     * @return
     * @throws IllegalArgumentException if <code>to - from</code> is less than <code>k</code>.
     */
    public static char kthLargest(final char[] a, final int from, final int to, final int k) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        return Array.kthLargest(a, from, to, k);
    }

    /**
     *
     * @param a
     * @param k
     * @return
     * @throws IllegalArgumentException if the length of the specified array is less than <code>k</code>.
     */
    public static byte kthLargest(final byte[] a, final int k) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return Array.kthLargest(a, k);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @param k
     * @return
     * @throws IllegalArgumentException if <code>to - from</code> is less than <code>k</code>.
     */
    public static byte kthLargest(final byte[] a, final int from, final int to, final int k) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        return Array.kthLargest(a, from, to, k);
    }

    /**
     *
     * @param a
     * @param k
     * @return
     * @throws IllegalArgumentException if the length of the specified array is less than <code>k</code>.
     */
    public static short kthLargest(final short[] a, final int k) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return Array.kthLargest(a, k);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @param k
     * @return
     * @throws IllegalArgumentException if <code>to - from</code> is less than <code>k</code>.
     */
    public static short kthLargest(final short[] a, final int from, final int to, final int k) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        return Array.kthLargest(a, from, to, k);
    }

    /**
     *
     * @param a
     * @param k
     * @return
     * @throws IllegalArgumentException if the length of the specified array is less than <code>k</code>.
     */
    public static int kthLargest(final int[] a, final int k) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return Array.kthLargest(a, k);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @param k
     * @return
     * @throws IllegalArgumentException if <code>to - from</code> is less than <code>k</code>.
     */
    public static int kthLargest(final int[] a, final int from, final int to, final int k) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        return Array.kthLargest(a, from, to, k);
    }

    /**
     *
     * @param a
     * @param k
     * @return
     * @throws IllegalArgumentException if the length of the specified array is less than <code>k</code>.
     */
    public static long kthLargest(final long[] a, final int k) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return Array.kthLargest(a, k);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @param k
     * @return
     * @throws IllegalArgumentException if <code>to - from</code> is less than <code>k</code>.
     */
    public static long kthLargest(final long[] a, final int from, final int to, final int k) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        return Array.kthLargest(a, from, to, k);
    }

    /**
     *
     * @param a
     * @param k
     * @return
     * @throws IllegalArgumentException if the length of the specified array is less than <code>k</code>.
     */
    public static float kthLargest(final float[] a, final int k) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return Array.kthLargest(a, k);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @param k
     * @return
     * @throws IllegalArgumentException if <code>to - from</code> is less than <code>k</code>.
     */
    public static float kthLargest(final float[] a, final int from, final int to, final int k) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        return Array.kthLargest(a, from, to, k);
    }

    /**
     *
     * @param a
     * @param k
     * @return
     * @throws IllegalArgumentException if the length of the specified array is less than <code>k</code>.
     */
    public static double kthLargest(final double[] a, final int k) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return Array.kthLargest(a, k);
    }

    /**
     *
     * @param a
     * @param from
     * @param to
     * @param k
     * @return
     * @throws IllegalArgumentException if <code>to - from</code> is less than <code>k</code>.
     */
    public static double kthLargest(final double[] a, final int from, final int to, final int k) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        return Array.kthLargest(a, from, to, k);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param k
     * @return
     * @throws IllegalArgumentException if the length of the specified array is less than <code>k</code>.
     */
    public static <T extends Comparable<? super T>> T kthLargest(final T[] a, final int k) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return Array.kthLargest(a, k);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param from
     * @param to
     * @param k
     * @return
     * @throws IllegalArgumentException if <code>to - from</code> is less than <code>k</code>.
     */
    public static <T extends Comparable<? super T>> T kthLargest(final T[] a, final int from, final int to, final int k) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        return Array.kthLargest(a, from, to, k);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param k
     * @param cmp
     * @return
     * @throws IllegalArgumentException if the length of the specified array is less than <code>k</code>.
     */
    public static <T> T kthLargest(final T[] a, final int k, final Comparator<? super T> cmp) {
        checkArgNotNullOrEmpty(a, "The spcified array can not be null or empty");

        return Array.kthLargest(a, k, cmp);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param from
     * @param to
     * @param k
     * @param cmp
     * @return
     * @throws IllegalArgumentException if <code>to - from</code> is less than <code>k</code>.
     */
    public static <T> T kthLargest(final T[] a, final int from, final int to, final int k, final Comparator<? super T> cmp) {
        if (isNullOrEmpty(a) || to - from < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        return Array.kthLargest(a, from, to, k, cmp);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param k
     * @return
     * @throws IllegalArgumentException if the length of the specified array is less than <code>k</code>.
     */
    public static <T extends Comparable<? super T>> T kthLargest(final Collection<? extends T> c, final int k) {
        checkArgNotNullOrEmpty(c, "The spcified collection can not be null or empty");

        return Array.kthLargest(c, k);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param from
     * @param to
     * @param k
     * @return
     * @throws IllegalArgumentException if <code>to - from</code> is less than <code>k</code>.
     */
    public static <T extends Comparable<? super T>> T kthLargest(final Collection<? extends T> c, final int from, final int to, final int k) {
        if (isNullOrEmpty(c) || to - from < 1) {
            throw new IllegalArgumentException("The length of collection can not be null or empty");
        }

        return Array.kthLargest(c, from, to, k);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param k
     * @param cmp
     * @return
     * @throws IllegalArgumentException if the length of the specified array is less than <code>k</code>.
     */
    public static <T> T kthLargest(final Collection<? extends T> c, final int k, final Comparator<? super T> cmp) {
        checkArgNotNullOrEmpty(c, "The spcified collection can not be null or empty");

        return Array.kthLargest(c, k, cmp);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param from
     * @param to
     * @param k
     * @param cmp
     * @return
     * @throws IllegalArgumentException if <code>to - from</code> is less than <code>k</code>.
     */
    public static <T> T kthLargest(final Collection<? extends T> c, final int from, final int to, final int k, final Comparator<? super T> cmp) {
        if (isNullOrEmpty(c) || to - from < 1) {
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
        checkArgNotNullOrEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty");

        final int len = sortedArray.length;
        final Map<Percentage, Character> m = new LinkedHashMap<>(initHashCapacity(Percentage.values().length));

        for (Percentage p : Percentage.values()) {
            m.put(p, sortedArray[(int) (len * p.doubleValue())]);
        }

        return m;
    }

    /**
     * Returns the elements at: <code>Percentage</code> * length of the specified array.
     *
     * @param sortedArray
     * @return
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is null or empty.
     */
    public static Map<Percentage, Byte> percentiles(final byte[] sortedArray) {
        checkArgNotNullOrEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty");

        final int len = sortedArray.length;
        final Map<Percentage, Byte> m = new LinkedHashMap<>(initHashCapacity(Percentage.values().length));

        for (Percentage p : Percentage.values()) {
            m.put(p, sortedArray[(int) (len * p.doubleValue())]);
        }

        return m;
    }

    /**
     * Returns the elements at: <code>Percentage</code> * length of the specified array.
     *
     * @param sortedArray
     * @return
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is null or empty.
     */
    public static Map<Percentage, Short> percentiles(final short[] sortedArray) {
        checkArgNotNullOrEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty");

        final int len = sortedArray.length;
        final Map<Percentage, Short> m = new LinkedHashMap<>(initHashCapacity(Percentage.values().length));

        for (Percentage p : Percentage.values()) {
            m.put(p, sortedArray[(int) (len * p.doubleValue())]);
        }

        return m;
    }

    /**
     * Returns the elements at: <code>Percentage</code> * length of the specified array.
     *
     * @param sortedArray
     * @return
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is null or empty.
     */
    public static Map<Percentage, Integer> percentiles(final int[] sortedArray) {
        checkArgNotNullOrEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty");

        final int len = sortedArray.length;
        final Map<Percentage, Integer> m = new LinkedHashMap<>(initHashCapacity(Percentage.values().length));

        for (Percentage p : Percentage.values()) {
            m.put(p, sortedArray[(int) (len * p.doubleValue())]);
        }

        return m;
    }

    /**
     * Returns the elements at: <code>Percentage</code> * length of the specified array.
     *
     * @param sortedArray
     * @return
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is null or empty.
     */
    public static Map<Percentage, Long> percentiles(final long[] sortedArray) {
        checkArgNotNullOrEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty");

        final int len = sortedArray.length;
        final Map<Percentage, Long> m = new LinkedHashMap<>(initHashCapacity(Percentage.values().length));

        for (Percentage p : Percentage.values()) {
            m.put(p, sortedArray[(int) (len * p.doubleValue())]);
        }

        return m;
    }

    /**
     * Returns the elements at: <code>Percentage</code> * length of the specified array.
     *
     * @param sortedArray
     * @return
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is null or empty.
     */
    public static Map<Percentage, Float> percentiles(final float[] sortedArray) {
        checkArgNotNullOrEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty");

        final int len = sortedArray.length;
        final Map<Percentage, Float> m = new LinkedHashMap<>(initHashCapacity(Percentage.values().length));

        for (Percentage p : Percentage.values()) {
            m.put(p, sortedArray[(int) (len * p.doubleValue())]);
        }

        return m;
    }

    /**
     * Returns the elements at: <code>Percentage</code> * length of the specified array.
     *
     * @param sortedArray
     * @return
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is null or empty.
     */
    public static Map<Percentage, Double> percentiles(final double[] sortedArray) {
        checkArgNotNullOrEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty");

        final int len = sortedArray.length;
        final Map<Percentage, Double> m = new LinkedHashMap<>(initHashCapacity(Percentage.values().length));

        for (Percentage p : Percentage.values()) {
            m.put(p, sortedArray[(int) (len * p.doubleValue())]);
        }

        return m;
    }

    /**
     * Returns the elements at: <code>Percentage</code> * length of the specified array.
     *
     * @param <T>
     * @param sortedArray
     * @return
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is null or empty.
     */
    public static <T> Map<Percentage, T> percentiles(final T[] sortedArray) {
        checkArgNotNullOrEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty");

        final int len = sortedArray.length;
        final Map<Percentage, T> m = new LinkedHashMap<>(initHashCapacity(Percentage.values().length));

        for (Percentage p : Percentage.values()) {
            m.put(p, sortedArray[(int) (len * p.doubleValue())]);
        }

        return m;
    }

    /**
     * Returns the elements at: <code>Percentage</code> * length of the specified array.
     *
     * @param <T>
     * @param sortedList
     * @return
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is null or empty.
     */
    public static <T> Map<Percentage, T> percentiles(final List<T> sortedList) {
        checkArgNotNullOrEmpty(sortedList, "The spcified 'sortedList' can not be null or empty");

        final int size = sortedList.size();
        final Map<Percentage, T> m = new LinkedHashMap<>(initHashCapacity(Percentage.values().length));

        for (Percentage p : Percentage.values()) {
            m.put(p, sortedList.get((int) (size * p.doubleValue())));
        }

        return m;
    }

    /**
     *
     * @param <E>
     * @param startInclusive
     * @param endExclusive
     * @param action
     * @throws E the e
     */
    public static <E extends Exception> void forEach(final int startInclusive, final int endExclusive, Throwables.Runnable<E> action) throws E {
        forEach(startInclusive, endExclusive, 1, action);
    }

    /**
     *
     * @param <E>
     * @param startInclusive
     * @param endExclusive
     * @param step
     * @param action
     * @throws E the e
     */
    public static <E extends Exception> void forEach(final int startInclusive, final int endExclusive, final int step, Throwables.Runnable<E> action) throws E {
        checkArgument(step != 0, "The input parameter 'step' can not be zero");

        if (endExclusive == startInclusive || endExclusive > startInclusive != step > 0) {
            return;
        }

        long len = (endExclusive * 1L - startInclusive) / step + ((endExclusive * 1L - startInclusive) % step == 0 ? 0 : 1);
        while (len-- > 0) {
            action.run();
        }
    }

    /**
     *
     * @param <E>
     * @param startInclusive
     * @param endExclusive
     * @param action
     * @throws E the e
     */
    public static <E extends Exception> void forEach(final int startInclusive, final int endExclusive, Throwables.IntConsumer<E> action) throws E {
        forEach(startInclusive, endExclusive, 1, action);
    }

    /**
     *
     * @param <E>
     * @param startInclusive
     * @param endExclusive
     * @param step
     * @param action
     * @throws E the e
     */
    public static <E extends Exception> void forEach(final int startInclusive, final int endExclusive, final int step, Throwables.IntConsumer<E> action)
            throws E {
        checkArgument(step != 0, "The input parameter 'step' can not be zero");

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

    /**
     *
     * @param <T>
     * @param <E>
     * @param startInclusive
     * @param endExclusive
     * @param a
     * @param action
     * @throws E the e
     * @deprecated use traditional for-loop
     */
    @Deprecated
    public static <T, E extends Exception> void forEach(final int startInclusive, final int endExclusive, final T a,
            Throwables.ObjIntConsumer<? super T, E> action) throws E {
        forEach(startInclusive, endExclusive, 1, a, action);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param startInclusive
     * @param endExclusive
     * @param step
     * @param a
     * @param action
     * @throws E the e
     * @deprecated use traditional for-loop
     */
    @Deprecated
    public static <T, E extends Exception> void forEach(final int startInclusive, final int endExclusive, final int step, final T a,
            Throwables.ObjIntConsumer<? super T, E> action) throws E {
        checkArgument(step != 0, "The input parameter 'step' can not be zero");

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

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEach(final T[] a, final Throwables.Consumer<? super T, E> action) throws E {
        checkArgNotNull(action);

        if (isNullOrEmpty(a)) {
            return;
        }

        for (T e : a) {
            action.accept(e);
        }
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEach(final T[] a, final int fromIndex, final int toIndex, final Throwables.Consumer<? super T, E> action)
            throws E {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex, len(a));
        checkArgNotNull(action);

        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEach(final Collection<? extends T> c, final Throwables.Consumer<? super T, E> action) throws E {
        checkArgNotNull(action);

        if (isNullOrEmpty(c)) {
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
     * @param <T>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEach(final Collection<? extends T> c, int fromIndex, final int toIndex,
            final Throwables.Consumer<? super T, E> action) throws E {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex, size(c));
        checkArgNotNull(action);

        if (isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return;
        }

        fromIndex = min(c.size() - 1, fromIndex);

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

    /**
     *
     * @param <K>
     * @param <V>
     * @param <E>
     * @param map
     * @param action
     * @throws E the e
     */
    public static <K, V, E extends Exception> void forEach(final Map<K, V> map, final Throwables.Consumer<? super Map.Entry<K, V>, E> action) throws E {
        checkArgNotNull(action);

        if (isNullOrEmpty(map)) {
            return;
        }

        forEach(map.entrySet(), action);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachIndexed(final T[] a, final Throwables.IndexedConsumer<? super T, E> action) throws E {
        checkArgNotNull(action);

        if (isNullOrEmpty(a)) {
            return;
        }

        forEachIndexed(a, 0, a.length, action);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachIndexed(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.IndexedConsumer<? super T, E> action) throws E {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex, len(a));
        checkArgNotNull(action);

        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachIndexed(final Collection<? extends T> c, final Throwables.IndexedConsumer<? super T, E> action)
            throws E {
        checkArgNotNull(action);

        if (isNullOrEmpty(c)) {
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
     * @param <T>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachIndexed(final Collection<? extends T> c, int fromIndex, final int toIndex,
            final Throwables.IndexedConsumer<? super T, E> action) throws E {
        checkFromToIndex(fromIndex < toIndex ? fromIndex : (toIndex == -1 ? 0 : toIndex), fromIndex < toIndex ? toIndex : fromIndex, size(c));
        checkArgNotNull(action);

        if (isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return;
        }

        fromIndex = min(c.size() - 1, fromIndex);

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

    /**
     *
     * @param <K>
     * @param <V>
     * @param <E>
     * @param map
     * @param action
     * @throws E the e
     */
    public static <K, V, E extends Exception> void forEachIndexed(final Map<K, V> map, final Throwables.IndexedConsumer<? super Map.Entry<K, V>, E> action)
            throws E {
        checkArgNotNull(action);

        if (isNullOrEmpty(map)) {
            return;
        }

        forEachIndexed(map.entrySet(), action);
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param <E>
     * @param <E2>
     * @param a
     * @param flatMapper
     * @param action
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, U, E extends Exception, E2 extends Exception> void forEach(final T[] a,
            final Throwables.Function<? super T, ? extends Collection<U>, E> flatMapper, final Throwables.BiConsumer<? super T, ? super U, E2> action)
            throws E, E2 {
        checkArgNotNull(flatMapper);
        checkArgNotNull(action);

        if (isNullOrEmpty(a)) {
            return;
        }

        for (T e : a) {
            final Collection<U> c2 = flatMapper.apply(e);

            if (notNullOrEmpty(c2)) {
                for (U u : c2) {
                    action.accept(e, u);
                }
            }
        }
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param <E>
     * @param <E2>
     * @param c
     * @param flatMapper
     * @param action
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, U, E extends Exception, E2 extends Exception> void forEach(final Collection<T> c,
            final Throwables.Function<? super T, ? extends Collection<U>, E> flatMapper, final Throwables.BiConsumer<? super T, ? super U, E2> action)
            throws E, E2 {
        checkArgNotNull(flatMapper);
        checkArgNotNull(action);

        if (isNullOrEmpty(c)) {
            return;
        }

        for (T e : c) {
            final Collection<U> c2 = flatMapper.apply(e);

            if (notNullOrEmpty(c2)) {
                for (U u : c2) {
                    action.accept(e, u);
                }
            }
        }
    }

    /**
     *
     * @param <T>
     * @param <T2>
     * @param <T3>
     * @param <E>
     * @param <E2>
     * @param <E3>
     * @param a
     * @param flatMapper
     * @param flatMapper2
     * @param action
     * @throws E the e
     * @throws E2 the e2
     * @throws E3 the e3
     */
    public static <T, T2, T3, E extends Exception, E2 extends Exception, E3 extends Exception> void forEach(final T[] a,
            final Throwables.Function<? super T, ? extends Collection<T2>, E> flatMapper,
            final Throwables.Function<? super T2, ? extends Collection<T3>, E2> flatMapper2,
            final Throwables.TriConsumer<? super T, ? super T2, ? super T3, E3> action) throws E, E2, E3 {
        checkArgNotNull(flatMapper);
        checkArgNotNull(flatMapper2);
        checkArgNotNull(action);

        if (isNullOrEmpty(a)) {
            return;
        }

        for (T e : a) {
            final Collection<T2> c2 = flatMapper.apply(e);

            if (notNullOrEmpty(c2)) {
                for (T2 t2 : c2) {
                    final Collection<T3> c3 = flatMapper2.apply(t2);

                    if (notNullOrEmpty(c3)) {
                        for (T3 t3 : c3) {
                            action.accept(e, t2, t3);
                        }
                    }
                }
            }
        }
    }

    /**
     *
     * @param <T>
     * @param <T2>
     * @param <T3>
     * @param <E>
     * @param <E2>
     * @param <E3>
     * @param c
     * @param flatMapper
     * @param flatMapper2
     * @param action
     * @throws E the e
     * @throws E2 the e2
     * @throws E3 the e3
     */
    public static <T, T2, T3, E extends Exception, E2 extends Exception, E3 extends Exception> void forEach(final Collection<T> c,
            final Throwables.Function<? super T, ? extends Collection<T2>, E> flatMapper,
            final Throwables.Function<? super T2, ? extends Collection<T3>, E2> flatMapper2,
            final Throwables.TriConsumer<? super T, ? super T2, ? super T3, E3> action) throws E, E2, E3 {
        checkArgNotNull(flatMapper);
        checkArgNotNull(flatMapper2);
        checkArgNotNull(action);

        if (isNullOrEmpty(c)) {
            return;
        }

        for (T e : c) {
            final Collection<T2> c2 = flatMapper.apply(e);

            if (notNullOrEmpty(c2)) {
                for (T2 t2 : c2) {
                    final Collection<T3> c3 = flatMapper2.apply(t2);

                    if (notNullOrEmpty(c3)) {
                        for (T3 t3 : c3) {
                            action.accept(e, t2, t3);
                        }
                    }
                }
            }
        }
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <E>
     * @param a
     * @param b
     * @param action
     * @throws E the e
     */
    public static <A, B, E extends Exception> void forEach(final A[] a, final B[] b, final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
        checkArgNotNull(action);

        if (isNullOrEmpty(a) || isNullOrEmpty(b)) {
            return;
        }

        for (int i = 0, minLen = min(a.length, b.length); i < minLen; i++) {
            action.accept(a[i], b[i]);
        }
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <E>
     * @param a
     * @param b
     * @param action
     * @throws E the e
     */
    public static <A, B, E extends Exception> void forEach(final Collection<A> a, final Collection<B> b,
            final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
        checkArgNotNull(action);

        if (isNullOrEmpty(a) || isNullOrEmpty(b)) {
            return;
        }

        final Iterator<A> iterA = a.iterator();
        final Iterator<B> iterB = b.iterator();

        for (int i = 0, minLen = min(a.size(), b.size()); i < minLen; i++) {
            action.accept(iterA.next(), iterB.next());
        }
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <E>
     * @param a
     * @param b
     * @param c
     * @param action
     * @throws E the e
     */
    public static <A, B, C, E extends Exception> void forEach(final A[] a, final B[] b, final C[] c,
            final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
        checkArgNotNull(action);

        if (isNullOrEmpty(a) || isNullOrEmpty(b) || isNullOrEmpty(c)) {
            return;
        }

        for (int i = 0, minLen = min(a.length, b.length, c.length); i < minLen; i++) {
            action.accept(a[i], b[i], c[i]);
        }
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <E>
     * @param a
     * @param b
     * @param c
     * @param action
     * @throws E the e
     */
    public static <A, B, C, E extends Exception> void forEach(final Collection<A> a, final Collection<B> b, final Collection<C> c,
            final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
        checkArgNotNull(action);

        if (isNullOrEmpty(a) || isNullOrEmpty(b) || isNullOrEmpty(c)) {
            return;
        }

        final Iterator<A> iterA = a.iterator();
        final Iterator<B> iterB = b.iterator();
        final Iterator<C> iterC = c.iterator();

        for (int i = 0, minLen = min(a.size(), b.size(), c.size()); i < minLen; i++) {
            action.accept(iterA.next(), iterB.next(), iterC.next());
        }
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <E>
     * @param a
     * @param b
     * @param valueForNoneA
     * @param valueForNoneB
     * @param action
     * @throws E the e
     */
    public static <A, B, E extends Exception> void forEach(final A[] a, final B[] b, final A valueForNoneA, final B valueForNoneB,
            final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
        checkArgNotNull(action);

        final int lenA = len(a);
        final int lenB = len(b);

        for (int i = 0, maxLen = max(lenA, lenB); i < maxLen; i++) {
            action.accept(i < lenA ? a[i] : valueForNoneA, i < lenB ? b[i] : valueForNoneB);
        }
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <E>
     * @param a
     * @param b
     * @param valueForNoneA
     * @param valueForNoneB
     * @param action
     * @throws E the e
     */
    public static <A, B, E extends Exception> void forEach(final Collection<A> a, final Collection<B> b, final A valueForNoneA, final B valueForNoneB,
            final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
        checkArgNotNull(action);

        final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a.iterator();
        final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b.iterator();
        final int lenA = size(a);
        final int lenB = size(b);

        for (int i = 0, maxLen = max(lenA, lenB); i < maxLen; i++) {
            action.accept(i < lenA ? iterA.next() : valueForNoneA, i < lenB ? iterB.next() : valueForNoneB);
        }
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <E>
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA
     * @param valueForNoneB
     * @param valueForNoneC
     * @param action
     * @throws E the e
     */
    public static <A, B, C, E extends Exception> void forEach(final A[] a, final B[] b, final C[] c, final A valueForNoneA, final B valueForNoneB,
            final C valueForNoneC, final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
        checkArgNotNull(action);

        final int lenA = len(a);
        final int lenB = len(b);
        final int lenC = len(c);

        for (int i = 0, maxLen = max(lenA, lenB, lenC); i < maxLen; i++) {
            action.accept(i < lenA ? a[i] : valueForNoneA, i < lenB ? b[i] : valueForNoneB, i < lenC ? c[i] : valueForNoneC);
        }
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <E>
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA
     * @param valueForNoneB
     * @param valueForNoneC
     * @param action
     * @throws E the e
     */
    public static <A, B, C, E extends Exception> void forEach(final Collection<A> a, final Collection<B> b, final Collection<C> c, final A valueForNoneA,
            final B valueForNoneB, final C valueForNoneC, final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
        checkArgNotNull(action);

        final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a.iterator();
        final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b.iterator();
        final Iterator<C> iterC = c == null ? ObjIterator.<C> empty() : c.iterator();
        final int lenA = size(a);
        final int lenB = size(b);
        final int lenC = size(c);

        for (int i = 0, maxLen = max(lenA, lenB, lenC); i < maxLen; i++) {
            action.accept(i < lenA ? iterA.next() : valueForNoneA, i < lenB ? iterB.next() : valueForNoneB, i < lenC ? iterC.next() : valueForNoneC);
        }
    }

    /**
     * For each non null.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachNonNull(final T[] a, final Throwables.Consumer<? super T, E> action) throws E {
        checkArgNotNull(action);

        if (isNullOrEmpty(a)) {
            return;
        }

        for (T e : a) {
            if (e != null) {
                action.accept(e);
            }
        }
    }

    /**
     * For each non null.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachNonNull(final Collection<T> c, final Throwables.Consumer<? super T, E> action) throws E {
        checkArgNotNull(action);

        if (isNullOrEmpty(c)) {
            return;
        }

        for (T e : c) {
            if (e != null) {
                action.accept(e);
            }
        }
    }

    /**
     * For each non null.
     *
     * @param <T>
     * @param <U>
     * @param <E>
     * @param <E2>
     * @param a
     * @param flatMapper
     * @param action
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, U, E extends Exception, E2 extends Exception> void forEachNonNull(final T[] a,
            final Throwables.Function<? super T, ? extends Collection<U>, E> flatMapper, final Throwables.BiConsumer<? super T, ? super U, E2> action)
            throws E, E2 {
        checkArgNotNull(flatMapper);
        checkArgNotNull(action);

        if (isNullOrEmpty(a)) {
            return;
        }

        for (T e : a) {
            if (e != null) {
                final Collection<U> c2 = flatMapper.apply(e);

                if (notNullOrEmpty(c2)) {
                    for (U u : c2) {
                        if (u != null) {
                            action.accept(e, u);
                        }
                    }
                }
            }
        }
    }

    /**
     * For each non null.
     *
     * @param <T>
     * @param <U>
     * @param <E>
     * @param <E2>
     * @param c
     * @param flatMapper
     * @param action
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, U, E extends Exception, E2 extends Exception> void forEachNonNull(final Collection<T> c,
            final Throwables.Function<? super T, ? extends Collection<U>, E> flatMapper, final Throwables.BiConsumer<? super T, ? super U, E2> action)
            throws E, E2 {
        checkArgNotNull(flatMapper);
        checkArgNotNull(action);

        if (isNullOrEmpty(c)) {
            return;
        }

        for (T e : c) {
            if (e != null) {
                final Collection<U> c2 = flatMapper.apply(e);

                if (notNullOrEmpty(c2)) {
                    for (U u : c2) {
                        if (u != null) {
                            action.accept(e, u);
                        }
                    }
                }
            }
        }
    }

    /**
     * For each non null.
     *
     * @param <T>
     * @param <T2>
     * @param <T3>
     * @param <E>
     * @param <E2>
     * @param <E3>
     * @param a
     * @param flatMapper
     * @param flatMapper2
     * @param action
     * @throws E the e
     * @throws E2 the e2
     * @throws E3 the e3
     */
    public static <T, T2, T3, E extends Exception, E2 extends Exception, E3 extends Exception> void forEachNonNull(final T[] a,
            final Throwables.Function<? super T, ? extends Collection<T2>, E> flatMapper,
            final Throwables.Function<? super T2, ? extends Collection<T3>, E2> flatMapper2,
            final Throwables.TriConsumer<? super T, ? super T2, ? super T3, E3> action) throws E, E2, E3 {
        checkArgNotNull(flatMapper);
        checkArgNotNull(flatMapper2);
        checkArgNotNull(action);

        if (isNullOrEmpty(a)) {
            return;
        }

        for (T e : a) {
            if (e != null) {
                final Collection<T2> c2 = flatMapper.apply(e);

                if (notNullOrEmpty(c2)) {
                    for (T2 t2 : c2) {
                        if (t2 != null) {
                            final Collection<T3> c3 = flatMapper2.apply(t2);

                            if (notNullOrEmpty(c3)) {
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

    /**
     * For each non null.
     *
     * @param <T>
     * @param <T2>
     * @param <T3>
     * @param <E>
     * @param <E2>
     * @param <E3>
     * @param c
     * @param flatMapper
     * @param flatMapper2
     * @param action
     * @throws E the e
     * @throws E2 the e2
     * @throws E3 the e3
     */
    public static <T, T2, T3, E extends Exception, E2 extends Exception, E3 extends Exception> void forEachNonNull(final Collection<T> c,
            final Throwables.Function<? super T, ? extends Collection<T2>, E> flatMapper,
            final Throwables.Function<? super T2, ? extends Collection<T3>, E2> flatMapper2,
            final Throwables.TriConsumer<? super T, ? super T2, ? super T3, E3> action) throws E, E2, E3 {
        checkArgNotNull(flatMapper);
        checkArgNotNull(flatMapper2);
        checkArgNotNull(action);

        if (isNullOrEmpty(c)) {
            return;
        }

        for (T e : c) {
            if (e != null) {
                final Collection<T2> c2 = flatMapper.apply(e);

                if (notNullOrEmpty(c2)) {
                    for (T2 t2 : c2) {
                        if (t2 != null) {
                            final Collection<T3> c3 = flatMapper2.apply(t2);

                            if (notNullOrEmpty(c3)) {
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

    /**
     *
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> BooleanList filter(final boolean[] a, final Throwables.BooleanPredicate<E> filter) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return new BooleanList();
        }

        return filter(a, 0, a.length, filter);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    public static <E extends Exception> BooleanList filter(final boolean[] a, final Throwables.BooleanPredicate<E> filter, final int max) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return new BooleanList();
        }

        return filter(a, 0, a.length, filter, max);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> BooleanList filter(final boolean[] a, final int fromIndex, final int toIndex,
            final Throwables.BooleanPredicate<E> filter) throws E {
        return filter(a, fromIndex, toIndex, filter, Integer.MAX_VALUE);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max maximum return result.
     * @return
     * @throws E the e
     */
    public static <E extends Exception> BooleanList filter(final boolean[] a, final int fromIndex, final int toIndex,
            final Throwables.BooleanPredicate<E> filter, final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> CharList filter(final char[] a, final Throwables.CharPredicate<E> filter) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return new CharList();
        }

        return filter(a, 0, a.length, filter);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    public static <E extends Exception> CharList filter(final char[] a, final Throwables.CharPredicate<E> filter, final int max) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return new CharList();
        }

        return filter(a, 0, a.length, filter, max);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> CharList filter(final char[] a, final int fromIndex, final int toIndex, final Throwables.CharPredicate<E> filter)
            throws E {
        return filter(a, fromIndex, toIndex, filter, Integer.MAX_VALUE);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max maximum return result.
     * @return
     * @throws E the e
     */
    public static <E extends Exception> CharList filter(final char[] a, final int fromIndex, final int toIndex, final Throwables.CharPredicate<E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> ByteList filter(final byte[] a, final Throwables.BytePredicate<E> filter) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return new ByteList();
        }

        return filter(a, 0, a.length, filter);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    public static <E extends Exception> ByteList filter(final byte[] a, final Throwables.BytePredicate<E> filter, final int max) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return new ByteList();
        }

        return filter(a, 0, a.length, filter, max);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> ByteList filter(final byte[] a, final int fromIndex, final int toIndex, final Throwables.BytePredicate<E> filter)
            throws E {
        return filter(a, fromIndex, toIndex, filter, Integer.MAX_VALUE);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max maximum return result.
     * @return
     * @throws E the e
     */
    public static <E extends Exception> ByteList filter(final byte[] a, final int fromIndex, final int toIndex, final Throwables.BytePredicate<E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> ShortList filter(final short[] a, final Throwables.ShortPredicate<E> filter) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return new ShortList();
        }

        return filter(a, 0, a.length, filter);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    public static <E extends Exception> ShortList filter(final short[] a, final Throwables.ShortPredicate<E> filter, final int max) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return new ShortList();
        }

        return filter(a, 0, a.length, filter, max);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> ShortList filter(final short[] a, final int fromIndex, final int toIndex, final Throwables.ShortPredicate<E> filter)
            throws E {
        return filter(a, fromIndex, toIndex, filter, Integer.MAX_VALUE);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max maximum return result.
     * @return
     * @throws E the e
     */
    public static <E extends Exception> ShortList filter(final short[] a, final int fromIndex, final int toIndex, final Throwables.ShortPredicate<E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> IntList filter(final int[] a, final Throwables.IntPredicate<E> filter) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return new IntList();
        }

        return filter(a, 0, a.length, filter);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    public static <E extends Exception> IntList filter(final int[] a, final Throwables.IntPredicate<E> filter, final int max) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return new IntList();
        }

        return filter(a, 0, a.length, filter, max);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> IntList filter(final int[] a, final int fromIndex, final int toIndex, final Throwables.IntPredicate<E> filter)
            throws E {
        return filter(a, fromIndex, toIndex, filter, Integer.MAX_VALUE);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max maximum return result.
     * @return
     * @throws E the e
     */
    public static <E extends Exception> IntList filter(final int[] a, final int fromIndex, final int toIndex, final Throwables.IntPredicate<E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> LongList filter(final long[] a, final Throwables.LongPredicate<E> filter) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return new LongList();
        }

        return filter(a, 0, a.length, filter);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    public static <E extends Exception> LongList filter(final long[] a, final Throwables.LongPredicate<E> filter, final int max) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return new LongList();
        }

        return filter(a, 0, a.length, filter, max);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> LongList filter(final long[] a, final int fromIndex, final int toIndex, final Throwables.LongPredicate<E> filter)
            throws E {
        return filter(a, fromIndex, toIndex, filter, Integer.MAX_VALUE);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max maximum return result.
     * @return
     * @throws E the e
     */
    public static <E extends Exception> LongList filter(final long[] a, final int fromIndex, final int toIndex, final Throwables.LongPredicate<E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> FloatList filter(final float[] a, final Throwables.FloatPredicate<E> filter) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return new FloatList();
        }

        return filter(a, 0, a.length, filter);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    public static <E extends Exception> FloatList filter(final float[] a, final Throwables.FloatPredicate<E> filter, final int max) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return new FloatList();
        }

        return filter(a, 0, a.length, filter, max);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> FloatList filter(final float[] a, final int fromIndex, final int toIndex, final Throwables.FloatPredicate<E> filter)
            throws E {
        return filter(a, fromIndex, toIndex, filter, Integer.MAX_VALUE);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max maximum return result.
     * @return
     * @throws E the e
     */
    public static <E extends Exception> FloatList filter(final float[] a, final int fromIndex, final int toIndex, final Throwables.FloatPredicate<E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> DoubleList filter(final double[] a, final Throwables.DoublePredicate<E> filter) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return new DoubleList();
        }

        return filter(a, 0, a.length, filter);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    public static <E extends Exception> DoubleList filter(final double[] a, final Throwables.DoublePredicate<E> filter, final int max) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return new DoubleList();
        }

        return filter(a, 0, a.length, filter, max);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> DoubleList filter(final double[] a, final int fromIndex, final int toIndex, final Throwables.DoublePredicate<E> filter)
            throws E {
        return filter(a, fromIndex, toIndex, filter, Integer.MAX_VALUE);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max maximum return result.
     * @return
     * @throws E the e
     */
    public static <E extends Exception> DoubleList filter(final double[] a, final int fromIndex, final int toIndex, final Throwables.DoublePredicate<E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> filter(final T[] a, final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        return filter(a, filter, Integer.MAX_VALUE);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> filter(final T[] a, final Throwables.Predicate<? super T, E> filter, final int max) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        return filter(a, 0, a.length, filter, max);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> filter(final T[] a, final int fromIndex, final int toIndex, final Throwables.Predicate<? super T, E> filter)
            throws E {
        return filter(a, fromIndex, toIndex, filter, Integer.MAX_VALUE);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> filter(final T[] a, final int fromIndex, final int toIndex, final Throwables.Predicate<? super T, E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
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

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param filter
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> filter(final Collection<? extends T> c, final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(c)) {
            return new ArrayList<>();
        }

        return filter(c, filter, Integer.MAX_VALUE);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> filter(final Collection<? extends T> c, final Throwables.Predicate<? super T, E> filter, final int max)
            throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(c)) {
            return new ArrayList<>();
        }

        return filter(c, 0, c.size(), filter, max);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> filter(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.Predicate<? super T, E> filter) throws E {
        return filter(c, fromIndex, toIndex, filter, Integer.MAX_VALUE);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> filter(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.Predicate<? super T, E> filter, final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(filter);

        if (isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
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

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param a
     * @param filter
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R extends Collection<T>, E extends Exception> R filter(final T[] a, final Throwables.Predicate<? super T, E> filter,
            final IntFunction<R> supplier) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return supplier.apply(0);
        }

        return filter(a, filter, Integer.MAX_VALUE, supplier);
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param a
     * @param filter
     * @param max
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R extends Collection<T>, E extends Exception> R filter(final T[] a, final Throwables.Predicate<? super T, E> filter, final int max,
            final IntFunction<R> supplier) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return supplier.apply(0);
        }

        return filter(a, 0, a.length, filter, max, supplier);
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R extends Collection<T>, E extends Exception> R filter(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.Predicate<? super T, E> filter, final IntFunction<R> supplier) throws E {
        return filter(a, fromIndex, toIndex, filter, Integer.MAX_VALUE, supplier);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R extends Collection<T>, E extends Exception> R filter(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.Predicate<? super T, E> filter, final int max, final IntFunction<R> supplier) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return supplier.apply(0);
        }

        final R result = supplier.apply(min(9, max, (toIndex - fromIndex)));

        for (int i = fromIndex, cnt = 0; i < toIndex && cnt < max; i++) {
            if (filter.test(a[i])) {
                result.add(a[i]);
                cnt++;
            }
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param c
     * @param filter
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R extends Collection<T>, E extends Exception> R filter(final Collection<? extends T> c, final Throwables.Predicate<? super T, E> filter,
            final IntFunction<R> supplier) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(c)) {
            return supplier.apply(0);
        }

        return filter(c, filter, Integer.MAX_VALUE, supplier);
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param c
     * @param filter
     * @param max
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R extends Collection<T>, E extends Exception> R filter(final Collection<? extends T> c, final Throwables.Predicate<? super T, E> filter,
            final int max, final IntFunction<R> supplier) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(c)) {
            return supplier.apply(0);
        }

        return filter(c, 0, c.size(), filter, max, supplier);
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R extends Collection<T>, E extends Exception> R filter(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.Predicate<? super T, E> filter, final IntFunction<R> supplier) throws E {
        return filter(c, fromIndex, toIndex, filter, Integer.MAX_VALUE, supplier);
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @param max
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R extends Collection<T>, E extends Exception> R filter(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.Predicate<? super T, E> filter, final int max, final IntFunction<R> supplier) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(filter);

        if (isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return supplier.apply(0);
        }

        final R result = supplier.apply(min(9, max, (toIndex - fromIndex)));

        if ((isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) || (fromIndex == toIndex && fromIndex < c.size())) {
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

    /**
     * Map to boolean.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> BooleanList mapToBoolean(final T[] a, final Throwables.ToBooleanFunction<? super T, E> func) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return new BooleanList();
        }

        return mapToBoolean(a, 0, a.length, func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> BooleanList mapToBoolean(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.ToBooleanFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return new BooleanList();
        }

        final BooleanList result = new BooleanList(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(func.applyAsBoolean(a[i]));
        }

        return result;
    }

    /**
     * Map to boolean.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> BooleanList mapToBoolean(final Collection<? extends T> c, final Throwables.ToBooleanFunction<? super T, E> func)
            throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(c)) {
            return new BooleanList();
        }

        return mapToBoolean(c, 0, c.size(), func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> BooleanList mapToBoolean(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToBooleanFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(func);

        if (isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
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

    /**
     * Map to char.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> CharList mapToChar(final T[] a, final Throwables.ToCharFunction<? super T, E> func) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return new CharList();
        }

        return mapToChar(a, 0, a.length, func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> CharList mapToChar(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.ToCharFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return new CharList();
        }

        final CharList result = new CharList(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(func.applyAsChar(a[i]));
        }

        return result;
    }

    /**
     * Map to char.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> CharList mapToChar(final Collection<? extends T> c, final Throwables.ToCharFunction<? super T, E> func) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(c)) {
            return new CharList();
        }

        return mapToChar(c, 0, c.size(), func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> CharList mapToChar(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToCharFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(func);

        if (isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
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

    /**
     * Map to byte.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> ByteList mapToByte(final T[] a, final Throwables.ToByteFunction<? super T, E> func) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return new ByteList();
        }

        return mapToByte(a, 0, a.length, func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> ByteList mapToByte(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.ToByteFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return new ByteList();
        }

        final ByteList result = new ByteList(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(func.applyAsByte(a[i]));
        }

        return result;
    }

    /**
     * Map to byte.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> ByteList mapToByte(final Collection<? extends T> c, final Throwables.ToByteFunction<? super T, E> func) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(c)) {
            return new ByteList();
        }

        return mapToByte(c, 0, c.size(), func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> ByteList mapToByte(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToByteFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(func);

        if (isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
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

    /**
     * Map to short.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> ShortList mapToShort(final T[] a, final Throwables.ToShortFunction<? super T, E> func) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return new ShortList();
        }

        return mapToShort(a, 0, a.length, func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> ShortList mapToShort(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.ToShortFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return new ShortList();
        }

        final ShortList result = new ShortList(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(func.applyAsShort(a[i]));
        }

        return result;
    }

    /**
     * Map to short.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> ShortList mapToShort(final Collection<? extends T> c, final Throwables.ToShortFunction<? super T, E> func) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(c)) {
            return new ShortList();
        }

        return mapToShort(c, 0, c.size(), func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> ShortList mapToShort(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToShortFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(func);

        if (isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
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

    /**
     * Map to int.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> IntList mapToInt(final T[] a, final Throwables.ToIntFunction<? super T, E> func) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return new IntList();
        }

        return mapToInt(a, 0, a.length, func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> IntList mapToInt(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.ToIntFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return new IntList();
        }

        final IntList result = new IntList(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(func.applyAsInt(a[i]));
        }

        return result;
    }

    /**
     * Map to int.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> IntList mapToInt(final Collection<? extends T> c, final Throwables.ToIntFunction<? super T, E> func) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(c)) {
            return new IntList();
        }

        return mapToInt(c, 0, c.size(), func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> IntList mapToInt(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToIntFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(func);

        if (isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
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

    /**
     * Map to long.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> LongList mapToLong(final T[] a, final Throwables.ToLongFunction<? super T, E> func) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return new LongList();
        }

        return mapToLong(a, 0, a.length, func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> LongList mapToLong(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.ToLongFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return new LongList();
        }

        final LongList result = new LongList(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(func.applyAsLong(a[i]));
        }

        return result;
    }

    /**
     * Map to long.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> LongList mapToLong(final Collection<? extends T> c, final Throwables.ToLongFunction<? super T, E> func) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(c)) {
            return new LongList();
        }

        return mapToLong(c, 0, c.size(), func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> LongList mapToLong(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToLongFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(func);

        if (isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
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

    /**
     * Map to float.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> FloatList mapToFloat(final T[] a, final Throwables.ToFloatFunction<? super T, E> func) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return new FloatList();
        }

        return mapToFloat(a, 0, a.length, func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> FloatList mapToFloat(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.ToFloatFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return new FloatList();
        }

        final FloatList result = new FloatList(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(func.applyAsFloat(a[i]));
        }

        return result;
    }

    /**
     * Map to float.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> FloatList mapToFloat(final Collection<? extends T> c, final Throwables.ToFloatFunction<? super T, E> func) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(c)) {
            return new FloatList();
        }

        return mapToFloat(c, 0, c.size(), func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> FloatList mapToFloat(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToFloatFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(func);

        if (isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
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

    /**
     * Map to double.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> DoubleList mapToDouble(final T[] a, final Throwables.ToDoubleFunction<? super T, E> func) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return new DoubleList();
        }

        return mapToDouble(a, 0, a.length, func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> DoubleList mapToDouble(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.ToDoubleFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return new DoubleList();
        }

        final DoubleList result = new DoubleList(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(func.applyAsDouble(a[i]));
        }

        return result;
    }

    /**
     * Map to double.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> DoubleList mapToDouble(final Collection<? extends T> c, final Throwables.ToDoubleFunction<? super T, E> func)
            throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(c)) {
            return new DoubleList();
        }

        return mapToDouble(c, 0, c.size(), func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> DoubleList mapToDouble(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToDoubleFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(func);

        if (isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
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

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param a
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> map(final T[] a, final Throwables.Function<? super T, ? extends R, E> func) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        return map(a, 0, a.length, func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> map(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ? extends R, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final List<R> result = new ArrayList<>(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(func.apply(a[i]));
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param c
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> map(final Collection<? extends T> c, final Throwables.Function<? super T, ? extends R, E> func) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(c)) {
            return new ArrayList<>();
        }

        return map(c, 0, c.size(), func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> map(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ? extends R, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(func);

        if (isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
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

    /**
     *
     * @param <T>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param a
     * @param func
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C map(final T[] a, final Throwables.Function<? super T, ? extends R, E> func,
            final IntFunction<? extends C> supplier) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return supplier.apply(0);
        }

        return map(a, 0, a.length, func, supplier);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C map(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ? extends R, E> func, final IntFunction<? extends C> supplier) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(func.apply(a[i]));
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param c
     * @param func
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C map(final Collection<? extends T> c,
            final Throwables.Function<? super T, ? extends R, E> func, final IntFunction<? extends C> supplier) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(c)) {
            return supplier.apply(0);
        }

        return map(c, 0, c.size(), func, supplier);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C map(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ? extends R, E> func, final IntFunction<? extends C> supplier) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(func);

        if (isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
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

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param a
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> flatMap(final T[] a, final Throwables.Function<? super T, ? extends Collection<? extends R>, E> func)
            throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        return flatMap(a, 0, a.length, func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> flatMap(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ? extends Collection<? extends R>, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = (toIndex - fromIndex) > N.MAX_ARRAY_SIZE / LOAD_FACTOR_FOR_FLAT_MAP ? N.MAX_ARRAY_SIZE
                : (int) ((toIndex - fromIndex) * LOAD_FACTOR_FOR_FLAT_MAP);
        final List<R> result = new ArrayList<>(len);
        Collection<? extends R> mr = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (notNullOrEmpty(mr = func.apply(a[i]))) {
                result.addAll(mr);
            }
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param c
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> flatMap(final Collection<? extends T> c,
            final Throwables.Function<? super T, ? extends Collection<? extends R>, E> func) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(c)) {
            return new ArrayList<>();
        }

        return flatMap(c, 0, c.size(), func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> flatMap(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ? extends Collection<? extends R>, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(func);

        if (isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return new ArrayList<>();
        }

        final int len = (toIndex - fromIndex) > N.MAX_ARRAY_SIZE / LOAD_FACTOR_FOR_FLAT_MAP ? N.MAX_ARRAY_SIZE
                : (int) ((toIndex - fromIndex) * LOAD_FACTOR_FOR_FLAT_MAP);
        final List<R> result = new ArrayList<>(len);
        Collection<? extends R> mr = null;

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                if (notNullOrEmpty(mr = func.apply(list.get(i)))) {
                    result.addAll(mr);
                }
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }
                if (notNullOrEmpty(mr = func.apply(e))) {
                    result.addAll(mr);
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
     * @param <T>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param a
     * @param func
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C flatMap(final T[] a,
            final Throwables.Function<? super T, ? extends Collection<? extends R>, E> func, final IntFunction<? extends C> supplier) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return supplier.apply(0);
        }

        return flatMap(a, 0, a.length, func, supplier);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C flatMap(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ? extends Collection<? extends R>, E> func, final IntFunction<? extends C> supplier) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return supplier.apply(0);
        }

        final int len = (toIndex - fromIndex) > N.MAX_ARRAY_SIZE / LOAD_FACTOR_FOR_FLAT_MAP ? N.MAX_ARRAY_SIZE
                : (int) ((toIndex - fromIndex) * LOAD_FACTOR_FOR_FLAT_MAP);
        final C result = supplier.apply(len);
        Collection<? extends R> mr = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (notNullOrEmpty(mr = func.apply(a[i]))) {
                result.addAll(mr);
            }
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param c
     * @param func
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C flatMap(final Collection<? extends T> c,
            final Throwables.Function<? super T, ? extends Collection<? extends R>, E> func, final IntFunction<? extends C> supplier) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(c)) {
            return supplier.apply(0);
        }

        return flatMap(c, 0, c.size(), func, supplier);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C flatMap(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ? extends Collection<? extends R>, E> func, final IntFunction<? extends C> supplier) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(func);

        if (isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return supplier.apply(0);
        }

        final int len = (toIndex - fromIndex) > N.MAX_ARRAY_SIZE / LOAD_FACTOR_FOR_FLAT_MAP ? N.MAX_ARRAY_SIZE
                : (int) ((toIndex - fromIndex) * LOAD_FACTOR_FOR_FLAT_MAP);
        final C result = supplier.apply(len);
        Collection<? extends R> mr = null;

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                if (notNullOrEmpty(mr = func.apply(list.get(i)))) {
                    result.addAll(mr);
                }
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                if (notNullOrEmpty(mr = func.apply(e))) {
                    result.addAll(mr);
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
     * @param <T>
     * @param <T2>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param <E2>
     * @param a
     * @param func
     * @param func2
     * @return
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, T2, R, C extends Collection<R>, E extends Exception, E2 extends Exception> List<R> flatMap(final T[] a,
            final Throwables.Function<? super T, ? extends Collection<? extends T2>, E> func,
            final Throwables.Function<? super T2, ? extends Collection<? extends R>, E2> func2) throws E, E2 {

        return flatMap(a, func, func2, Factory.<R> ofList());
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <T2>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param <E2>
     * @param a
     * @param func
     * @param func2
     * @param supplier
     * @return
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, T2, R, C extends Collection<R>, E extends Exception, E2 extends Exception> C flatMap(final T[] a,
            final Throwables.Function<? super T, ? extends Collection<? extends T2>, E> func,
            final Throwables.Function<? super T2, ? extends Collection<? extends R>, E2> func2, final IntFunction<? extends C> supplier) throws E, E2 {
        checkArgNotNull(func);
        checkArgNotNull(func2);

        if (isNullOrEmpty(a)) {
            return supplier.apply(0);
        }

        final int len = a.length > N.MAX_ARRAY_SIZE / LOAD_FACTOR_FOR_TWO_FLAT_MAP ? N.MAX_ARRAY_SIZE : a.length * LOAD_FACTOR_FOR_TWO_FLAT_MAP;
        final C result = supplier.apply(len);

        for (T e : a) {
            final Collection<? extends T2> c1 = func.apply(e);

            if (notNullOrEmpty(c1)) {
                for (T2 e2 : c1) {
                    final Collection<? extends R> c2 = func2.apply(e2);

                    if (notNullOrEmpty(c2)) {
                        result.addAll(c2);
                    }
                }
            }
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <T2>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param <E2>
     * @param c
     * @param func
     * @param func2
     * @return
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, T2, R, C extends Collection<R>, E extends Exception, E2 extends Exception> List<R> flatMap(final Collection<? extends T> c,
            final Throwables.Function<? super T, ? extends Collection<? extends T2>, E> func,
            final Throwables.Function<? super T2, ? extends Collection<? extends R>, E2> func2) throws E, E2 {

        return flatMap(c, func, func2, Factory.<R> ofList());
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <T2>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param <E2>
     * @param c
     * @param func
     * @param func2
     * @param supplier
     * @return
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, T2, R, C extends Collection<R>, E extends Exception, E2 extends Exception> C flatMap(final Collection<? extends T> c,
            final Throwables.Function<? super T, ? extends Collection<? extends T2>, E> func,
            final Throwables.Function<? super T2, ? extends Collection<? extends R>, E2> func2, final IntFunction<? extends C> supplier) throws E, E2 {
        checkArgNotNull(func);
        checkArgNotNull(func2);

        if (isNullOrEmpty(c)) {
            return supplier.apply(0);
        }

        final int len = c.size() > N.MAX_ARRAY_SIZE / LOAD_FACTOR_FOR_TWO_FLAT_MAP ? N.MAX_ARRAY_SIZE : c.size() * LOAD_FACTOR_FOR_TWO_FLAT_MAP;
        final C result = supplier.apply(len);

        for (T e : c) {
            final Collection<? extends T2> c1 = func.apply(e);

            if (notNullOrEmpty(c1)) {
                for (T2 e2 : c1) {
                    final Collection<? extends R> c2 = func2.apply(e2);

                    if (notNullOrEmpty(c2)) {
                        result.addAll(c2);
                    }
                }
            }
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param a
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> flattMap(final T[] a, final Throwables.Function<? super T, ? extends R[], E> func) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        return flattMap(a, 0, a.length, func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> flattMap(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ? extends R[], E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = (toIndex - fromIndex) > N.MAX_ARRAY_SIZE / LOAD_FACTOR_FOR_FLAT_MAP ? N.MAX_ARRAY_SIZE
                : (int) ((toIndex - fromIndex) * LOAD_FACTOR_FOR_FLAT_MAP);
        final List<R> result = new ArrayList<>(len);
        R[] mr = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (notNullOrEmpty(mr = func.apply(a[i]))) {
                result.addAll(Arrays.asList(mr));
            }
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param c
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> flattMap(final Collection<? extends T> c, final Throwables.Function<? super T, ? extends R[], E> func)
            throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(c)) {
            return new ArrayList<>();
        }

        return flattMap(c, 0, c.size(), func);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> flattMap(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ? extends R[], E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(func);

        if (isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return new ArrayList<>();
        }

        final int len = (toIndex - fromIndex) > N.MAX_ARRAY_SIZE / LOAD_FACTOR_FOR_FLAT_MAP ? N.MAX_ARRAY_SIZE
                : (int) ((toIndex - fromIndex) * LOAD_FACTOR_FOR_FLAT_MAP);
        final List<R> result = new ArrayList<>(len);
        R[] mr = null;

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                if (notNullOrEmpty(mr = func.apply(list.get(i)))) {
                    result.addAll(Arrays.asList(mr));
                }
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }
                if (notNullOrEmpty(mr = func.apply(e))) {
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
     * @param <T>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param a
     * @param func
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C flattMap(final T[] a, final Throwables.Function<? super T, ? extends R[], E> func,
            final IntFunction<? extends C> supplier) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return supplier.apply(0);
        }

        return flattMap(a, 0, a.length, func, supplier);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C flattMap(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ? extends R[], E> func, final IntFunction<? extends C> supplier) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(func);

        if (isNullOrEmpty(a)) {
            return supplier.apply(0);
        }

        final int len = (toIndex - fromIndex) > N.MAX_ARRAY_SIZE / LOAD_FACTOR_FOR_FLAT_MAP ? N.MAX_ARRAY_SIZE
                : (int) ((toIndex - fromIndex) * LOAD_FACTOR_FOR_FLAT_MAP);
        final C result = supplier.apply(len);
        R[] mr = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (notNullOrEmpty(mr = func.apply(a[i]))) {
                result.addAll(Arrays.asList(mr));
            }
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param c
     * @param func
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C flattMap(final Collection<? extends T> c,
            final Throwables.Function<? super T, ? extends R[], E> func, final IntFunction<? extends C> supplier) throws E {
        checkArgNotNull(func);

        if (isNullOrEmpty(c)) {
            return supplier.apply(0);
        }

        return flattMap(c, 0, c.size(), func, supplier);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C flattMap(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ? extends R[], E> func, final IntFunction<? extends C> supplier) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(func);

        if (isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return supplier.apply(0);
        }

        final int len = (toIndex - fromIndex) > N.MAX_ARRAY_SIZE / LOAD_FACTOR_FOR_FLAT_MAP ? N.MAX_ARRAY_SIZE
                : (int) ((toIndex - fromIndex) * LOAD_FACTOR_FOR_FLAT_MAP);
        final C result = supplier.apply(len);
        R[] mr = null;

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                if (notNullOrEmpty(mr = func.apply(list.get(i)))) {
                    result.addAll(Arrays.asList(mr));
                }
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                if (notNullOrEmpty(mr = func.apply(e))) {
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
     * @param <T>
     * @param a
     * @return
     */
    public static <T extends Number> int sumInt(final T[] a) {
        return sumInt(a, Fn.numToInt());
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> int sumInt(final T[] a, final int fromIndex, final int toIndex) {
        return sumInt(a, fromIndex, toIndex, Fn.numToInt());
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> int sumInt(final T[] a, final Throwables.ToIntFunction<? super T, E> func) throws E {
        if (isNullOrEmpty(a)) {
            return 0;
        }

        return sumInt(a, 0, a.length, func);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> int sumInt(final T[] a, final int fromIndex, final int toIndex, final Throwables.ToIntFunction<? super T, E> func)
            throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return 0;
        }

        int sum = 0;

        for (int i = fromIndex; i < toIndex; i++) {
            sum += func.applyAsInt(a[i]);
        }

        return sum;
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Number> int sumInt(final Collection<? extends T> c) {
        return sumInt(c, Fn.numToInt());
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> int sumInt(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        return sumInt(c, fromIndex, toIndex, Fn.numToInt());
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> int sumInt(final Collection<? extends T> c, final Throwables.ToIntFunction<? super T, E> func) throws E {
        if (isNullOrEmpty(c)) {
            return 0;
        }

        int sum = 0;

        for (T e : c) {
            sum += func.applyAsInt(e);
        }

        return sum;
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> int sumInt(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToIntFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (fromIndex == toIndex) {
            return 0;
        }

        int sum = 0;

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

        return sum;
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T extends Number> long sumLong(final T[] a) {
        return sumLong(a, Fn.numToLong());
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> long sumLong(final T[] a, final int fromIndex, final int toIndex) {
        return sumLong(a, fromIndex, toIndex, Fn.numToLong());
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> long sumLong(final T[] a, final Throwables.ToLongFunction<? super T, E> func) throws E {
        if (isNullOrEmpty(a)) {
            return 0L;
        }

        return sumLong(a, 0, a.length, func);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> long sumLong(final T[] a, final int fromIndex, final int toIndex, final Throwables.ToLongFunction<? super T, E> func)
            throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return 0L;
        }

        long sum = 0;

        for (int i = fromIndex; i < toIndex; i++) {
            sum += func.applyAsLong(a[i]);
        }

        return sum;
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Number> long sumLong(final Collection<? extends T> c) {
        return sumLong(c, Fn.numToLong());
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> long sumLong(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        return sumLong(c, fromIndex, toIndex, Fn.numToLong());
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> long sumLong(final Collection<? extends T> c, final Throwables.ToLongFunction<? super T, E> func) throws E {
        if (isNullOrEmpty(c)) {
            return 0L;
        }

        long sum = 0;

        for (T e : c) {
            sum += func.applyAsLong(e);
        }

        return sum;
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> long sumLong(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToLongFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (fromIndex == toIndex) {
            return 0L;
        }

        long sum = 0;

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                sum += func.applyAsLong(list.get(i));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                sum += func.applyAsLong(e);

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return sum;
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T extends Number> double sumDouble(final T[] a) {
        return sumDouble(a, Fn.numToDouble());
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> double sumDouble(final T[] a, final int fromIndex, final int toIndex) {
        return sumDouble(a, fromIndex, toIndex, Fn.numToDouble());
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> double sumDouble(final T[] a, final Throwables.ToDoubleFunction<? super T, E> func) throws E {
        if (isNullOrEmpty(a)) {
            return 0D;
        }

        return sumDouble(a, 0, a.length, func);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> double sumDouble(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.ToDoubleFunction<? super T, E> func) throws E {
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
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Number> double sumDouble(final Collection<? extends T> c) {
        return sumDouble(c, Fn.numToDouble());
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> double sumDouble(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        return sumDouble(c, fromIndex, toIndex, Fn.numToDouble());
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> double sumDouble(final Collection<? extends T> c, final Throwables.ToDoubleFunction<? super T, E> func) throws E {
        if (isNullOrEmpty(c)) {
            return 0D;
        }

        final KahanSummation summation = new KahanSummation();

        for (T e : c) {
            summation.add(func.applyAsDouble(e));
        }

        return summation.sum();
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> double sumDouble(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToDoubleFunction<? super T, E> func) throws E {
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
     * @param <T>
     * @param a
     * @return
     */
    public static <T extends Number> OptionalDouble averageInt(final T[] a) {
        return averageInt(a, Fn.numToInt());
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> OptionalDouble averageInt(final T[] a, final int fromIndex, final int toIndex) {
        return averageInt(a, fromIndex, toIndex, Fn.numToInt());
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> OptionalDouble averageInt(final T[] a, final Throwables.ToIntFunction<? super T, E> func) throws E {
        if (isNullOrEmpty(a)) {
            return OptionalDouble.empty();
        }

        return averageInt(a, 0, a.length, func);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> OptionalDouble averageInt(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.ToIntFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(((double) sumInt(a, fromIndex, toIndex, func)) / (toIndex - fromIndex));
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Number> OptionalDouble averageInt(final Collection<? extends T> c) {
        return averageInt(c, Fn.numToInt());
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> OptionalDouble averageInt(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        return averageInt(c, fromIndex, toIndex, Fn.numToInt());
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> OptionalDouble averageInt(final Collection<? extends T> c, final Throwables.ToIntFunction<? super T, E> func)
            throws E {
        if (isNullOrEmpty(c)) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(((double) sumInt(c, func)) / c.size());
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> OptionalDouble averageInt(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToIntFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (fromIndex == toIndex) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(((double) sumInt(c, fromIndex, toIndex, func)) / (toIndex - fromIndex));
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T extends Number> OptionalDouble averageLong(final T[] a) {
        return averageLong(a, Fn.numToLong());
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> OptionalDouble averageLong(final T[] a, final int fromIndex, final int toIndex) {
        return averageLong(a, fromIndex, toIndex, Fn.numToLong());
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> OptionalDouble averageLong(final T[] a, final Throwables.ToLongFunction<? super T, E> func) throws E {
        if (isNullOrEmpty(a)) {
            return OptionalDouble.empty();
        }

        return averageLong(a, 0, a.length, func);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> OptionalDouble averageLong(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.ToLongFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(((double) sumLong(a, fromIndex, toIndex, func)) / (toIndex - fromIndex));
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Number> OptionalDouble averageLong(final Collection<? extends T> c) {
        return averageLong(c, Fn.numToLong());
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> OptionalDouble averageLong(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        return averageLong(c, fromIndex, toIndex, Fn.numToLong());
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> OptionalDouble averageLong(final Collection<? extends T> c, final Throwables.ToLongFunction<? super T, E> func)
            throws E {
        if (isNullOrEmpty(c)) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(((double) sumLong(c, func)) / c.size());
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> OptionalDouble averageLong(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToLongFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (fromIndex == toIndex) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(((double) sumLong(c, fromIndex, toIndex, func)) / (toIndex - fromIndex));
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T extends Number> OptionalDouble averageDouble(final T[] a) {
        return averageDouble(a, Fn.numToDouble());
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> OptionalDouble averageDouble(final T[] a, final int fromIndex, final int toIndex) {
        return averageDouble(a, fromIndex, toIndex, Fn.numToDouble());
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> OptionalDouble averageDouble(final T[] a, final Throwables.ToDoubleFunction<? super T, E> func) throws E {
        if (isNullOrEmpty(a)) {
            return OptionalDouble.empty();
        }

        return averageDouble(a, 0, a.length, func);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> OptionalDouble averageDouble(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.ToDoubleFunction<? super T, E> func) throws E {
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
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Number> OptionalDouble averageDouble(final Collection<? extends T> c) {
        return averageDouble(c, Fn.numToDouble());
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> OptionalDouble averageDouble(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        return averageDouble(c, fromIndex, toIndex, Fn.numToDouble());
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> OptionalDouble averageDouble(final Collection<? extends T> c, final Throwables.ToDoubleFunction<? super T, E> func)
            throws E {
        if (isNullOrEmpty(c)) {
            return OptionalDouble.empty();
        }

        final KahanSummation summation = new KahanSummation();

        for (T e : c) {
            summation.add(func.applyAsDouble(e));
        }

        return summation.average();
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> OptionalDouble averageDouble(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToDoubleFunction<? super T, E> func) throws E {
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
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> int count(final boolean[] a, final Throwables.BooleanPredicate<E> filter) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return 0;
        }

        return count(a, 0, a.length, filter);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> int count(final boolean[] a, final int fromIndex, final int toIndex, final Throwables.BooleanPredicate<E> filter)
            throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
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
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> int count(final char[] a, final Throwables.CharPredicate<E> filter) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return 0;
        }

        return count(a, 0, a.length, filter);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> int count(final char[] a, final int fromIndex, final int toIndex, final Throwables.CharPredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
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
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> int count(final byte[] a, final Throwables.BytePredicate<E> filter) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return 0;
        }

        return count(a, 0, a.length, filter);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> int count(final byte[] a, final int fromIndex, final int toIndex, final Throwables.BytePredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
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
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> int count(final short[] a, final Throwables.ShortPredicate<E> filter) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return 0;
        }

        return count(a, 0, a.length, filter);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> int count(final short[] a, final int fromIndex, final int toIndex, final Throwables.ShortPredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
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
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> int count(final int[] a, final Throwables.IntPredicate<E> filter) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return 0;
        }

        return count(a, 0, a.length, filter);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> int count(final int[] a, final int fromIndex, final int toIndex, final Throwables.IntPredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
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
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> int count(final long[] a, final Throwables.LongPredicate<E> filter) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return 0;
        }

        return count(a, 0, a.length, filter);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> int count(final long[] a, final int fromIndex, final int toIndex, final Throwables.LongPredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
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
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> int count(final float[] a, final Throwables.FloatPredicate<E> filter) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return 0;
        }

        return count(a, 0, a.length, filter);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> int count(final float[] a, final int fromIndex, final int toIndex, final Throwables.FloatPredicate<E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
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
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> int count(final double[] a, final Throwables.DoublePredicate<E> filter) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return 0;
        }

        return count(a, 0, a.length, filter);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> int count(final double[] a, final int fromIndex, final int toIndex, final Throwables.DoublePredicate<E> filter)
            throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
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
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> int count(final T[] a, final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
            return 0;
        }

        return count(a, 0, a.length, filter);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> int count(final T[] a, final int fromIndex, final int toIndex, final Throwables.Predicate<? super T, E> filter)
            throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter);

        if (isNullOrEmpty(a)) {
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
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param filter
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> int count(final Collection<? extends T> c, final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNull(filter);

        if (isNullOrEmpty(c)) {
            return 0;
        }

        return count(c, 0, c.size(), filter);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> int count(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.Predicate<? super T, E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(filter);

        if ((isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) || (fromIndex == toIndex && fromIndex < c.size())) {
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

    /**
     *
     * @param a
     * @param n
     * @return
     */
    public static short[] top(final short[] a, final int n) {
        return top(a, n, null);
    }

    /**
     *
     * @param a
     * @param n
     * @param cmp
     * @return
     */
    public static short[] top(final short[] a, final int n, final Comparator<? super Short> cmp) {
        return top(a, 0, len(a), n, cmp);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param n
     * @return
     */
    public static short[] top(final short[] a, final int fromIndex, final int toIndex, final int n) {
        return top(a, fromIndex, toIndex, n, null);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param n
     * @param cmp
     * @return
     */
    public static short[] top(final short[] a, final int fromIndex, final int toIndex, final int n, final Comparator<? super Short> cmp) {
        checkArgNotNegative(n, "n");

        if (n == 0) {
            return EMPTY_SHORT_ARRAY;
        } else if (n >= toIndex - fromIndex) {
            return copyOfRange(a, fromIndex, toIndex);
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

    /**
     *
     * @param a
     * @param n
     * @return
     */
    public static int[] top(final int[] a, final int n) {
        return top(a, n, null);
    }

    /**
     *
     * @param a
     * @param n
     * @param cmp
     * @return
     */
    public static int[] top(final int[] a, final int n, final Comparator<? super Integer> cmp) {
        return top(a, 0, len(a), n, cmp);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param n
     * @return
     */
    public static int[] top(final int[] a, final int fromIndex, final int toIndex, final int n) {
        return top(a, fromIndex, toIndex, n, null);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param n
     * @param cmp
     * @return
     */
    public static int[] top(final int[] a, final int fromIndex, final int toIndex, final int n, final Comparator<? super Integer> cmp) {
        checkArgNotNegative(n, "n");

        if (n == 0) {
            return EMPTY_INT_ARRAY;
        } else if (n >= toIndex - fromIndex) {
            return copyOfRange(a, fromIndex, toIndex);
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

    /**
     *
     * @param a
     * @param n
     * @return
     */
    public static long[] top(final long[] a, final int n) {
        return top(a, n, null);
    }

    /**
     *
     * @param a
     * @param n
     * @param cmp
     * @return
     */
    public static long[] top(final long[] a, final int n, final Comparator<? super Long> cmp) {
        return top(a, 0, len(a), n, cmp);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param n
     * @return
     */
    public static long[] top(final long[] a, final int fromIndex, final int toIndex, final int n) {
        return top(a, fromIndex, toIndex, n, null);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param n
     * @param cmp
     * @return
     */
    public static long[] top(final long[] a, final int fromIndex, final int toIndex, final int n, final Comparator<? super Long> cmp) {
        checkArgNotNegative(n, "n");

        if (n == 0) {
            return EMPTY_LONG_ARRAY;
        } else if (n >= toIndex - fromIndex) {
            return copyOfRange(a, fromIndex, toIndex);
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

    /**
     *
     * @param a
     * @param n
     * @return
     */
    public static float[] top(final float[] a, final int n) {
        return top(a, n, null);
    }

    /**
     *
     * @param a
     * @param n
     * @param cmp
     * @return
     */
    public static float[] top(final float[] a, final int n, final Comparator<? super Float> cmp) {
        return top(a, 0, len(a), n, cmp);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param n
     * @return
     */
    public static float[] top(final float[] a, final int fromIndex, final int toIndex, final int n) {
        return top(a, fromIndex, toIndex, n, null);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param n
     * @param cmp
     * @return
     */
    public static float[] top(final float[] a, final int fromIndex, final int toIndex, final int n, final Comparator<? super Float> cmp) {
        checkArgNotNegative(n, "n");

        if (n == 0) {
            return EMPTY_FLOAT_ARRAY;
        } else if (n >= toIndex - fromIndex) {
            return copyOfRange(a, fromIndex, toIndex);
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

    /**
     *
     * @param a
     * @param n
     * @return
     */
    public static double[] top(final double[] a, final int n) {
        return top(a, n, null);
    }

    /**
     *
     * @param a
     * @param n
     * @param cmp
     * @return
     */
    public static double[] top(final double[] a, final int n, final Comparator<? super Double> cmp) {
        return top(a, 0, len(a), n, cmp);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param n
     * @return
     */
    public static double[] top(final double[] a, final int fromIndex, final int toIndex, final int n) {
        return top(a, fromIndex, toIndex, n, null);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param n
     * @param cmp
     * @return
     */
    public static double[] top(final double[] a, final int fromIndex, final int toIndex, final int n, final Comparator<? super Double> cmp) {
        checkArgNotNegative(n, "n");

        if (n == 0) {
            return EMPTY_DOUBLE_ARRAY;
        } else if (n >= toIndex - fromIndex) {
            return copyOfRange(a, fromIndex, toIndex);
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

    /**
     *
     * @param <T>
     * @param a
     * @param n
     * @return
     */
    public static <T extends Comparable<? super T>> List<T> top(final T[] a, final int n) {
        return top(a, n, NATURAL_ORDER);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param n
     * @param cmp
     * @return
     */
    public static <T> List<T> top(final T[] a, final int n, final Comparator<? super T> cmp) {
        return top(a, 0, len(a), n, cmp);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param n
     * @return
     */
    public static <T extends Comparable<? super T>> List<T> top(final T[] a, final int fromIndex, final int toIndex, final int n) {
        return top(a, fromIndex, toIndex, n, NATURAL_ORDER);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param n
     * @param cmp
     * @return
     */
    public static <T> List<T> top(final T[] a, final int fromIndex, final int toIndex, final int n, final Comparator<? super T> cmp) {
        checkArgNotNegative(n, "n");

        if (n == 0) {
            return new ArrayList<>();
        } else if (n >= toIndex - fromIndex) {
            return toList(a, fromIndex, toIndex);
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

        return createList((T[]) heap.toArray(EMPTY_OBJECT_ARRAY));
    }

    /**
     *
     * @param <T>
     * @param c
     * @param n
     * @return
     */
    public static <T extends Comparable<? super T>> List<T> top(final Collection<? extends T> c, final int n) {
        return top(c, n, null);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param n
     * @param cmp
     * @return
     */
    public static <T> List<T> top(final Collection<? extends T> c, final int n, final Comparator<? super T> cmp) {
        return top(c, 0, size(c), n, cmp);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param n
     * @return
     */
    public static <T extends Comparable<? super T>> List<T> top(final Collection<? extends T> c, final int fromIndex, final int toIndex, final int n) {
        return top(c, fromIndex, toIndex, n, null);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param n
     * @param cmp
     * @return
     */
    public static <T> List<T> top(final Collection<? extends T> c, final int fromIndex, final int toIndex, final int n, final Comparator<? super T> cmp) {
        checkArgNotNegative(n, "n");

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

        return createList((T[]) heap.toArray(EMPTY_OBJECT_ARRAY));
    }

    /**
     *
     * @param <T>
     * @param a
     * @param n
     * @param keepEncounterOrder
     * @return
     */
    public static <T extends Comparable<? super T>> List<T> top(final T[] a, final int n, final boolean keepEncounterOrder) {
        return top(a, n, NATURAL_ORDER, keepEncounterOrder);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param n
     * @param cmp
     * @param keepEncounterOrder
     * @return
     */
    public static <T> List<T> top(final T[] a, final int n, final Comparator<? super T> cmp, final boolean keepEncounterOrder) {
        return top(a, 0, len(a), n, cmp, keepEncounterOrder);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param n
     * @param keepEncounterOrder
     * @return
     */
    public static <T extends Comparable<? super T>> List<T> top(final T[] a, final int fromIndex, final int toIndex, final int n,
            final boolean keepEncounterOrder) {
        return top(a, fromIndex, toIndex, n, NATURAL_ORDER, keepEncounterOrder);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param n
     * @param cmp
     * @param keepEncounterOrder
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> List<T> top(final T[] a, final int fromIndex, final int toIndex, final int n, final Comparator<? super T> cmp,
            final boolean keepEncounterOrder) {
        checkArgNotNegative(n, "n");

        if (!keepEncounterOrder) {
            return top(a, fromIndex, toIndex, n, cmp);
        }

        if (n == 0) {
            return new ArrayList<>();
        } else if (n >= toIndex - fromIndex) {
            return toList(a, fromIndex, toIndex);
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

        sort(arrayOfIndexed, new Comparator<Indexed<T>>() {
            @Override
            public int compare(final Indexed<T> o1, final Indexed<T> o2) {
                return o1.index() - o2.index();
            }
        });

        final List<T> res = new ArrayList<>(arrayOfIndexed.length);

        for (Indexed<T> element : arrayOfIndexed) {
            res.add(element.value());
        }

        return res;
    }

    /**
     *
     * @param <T>
     * @param c
     * @param n
     * @param keepEncounterOrder
     * @return
     */
    public static <T extends Comparable<? super T>> List<T> top(final Collection<? extends T> c, final int n, final boolean keepEncounterOrder) {
        return top(c, n, NATURAL_ORDER, keepEncounterOrder);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param n
     * @param cmp
     * @param keepEncounterOrder
     * @return
     */
    public static <T> List<T> top(final Collection<? extends T> c, final int n, final Comparator<? super T> cmp, final boolean keepEncounterOrder) {
        return top(c, 0, size(c), n, cmp, keepEncounterOrder);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param n
     * @return
     */
    public static <T extends Comparable<? super T>> List<T> top(final Collection<? extends T> c, final int fromIndex, final int toIndex, final int n,
            final boolean keepEncounterOrder) {
        return top(c, fromIndex, toIndex, n, NATURAL_ORDER, keepEncounterOrder);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param n
     * @param cmp
     * @param keepEncounterOrder
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> List<T> top(final Collection<? extends T> c, final int fromIndex, final int toIndex, final int n, final Comparator<? super T> cmp,
            final boolean keepEncounterOrder) {
        checkArgNotNegative(n, "n");

        if (!keepEncounterOrder) {
            return top(c, fromIndex, toIndex, n, cmp);
        }

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

        sort(arrayOfIndexed, new Comparator<Indexed<T>>() {
            @Override
            public int compare(final Indexed<T> o1, final Indexed<T> o2) {
                return o1.index() - o2.index();
            }
        });

        final List<T> res = new ArrayList<>(arrayOfIndexed.length);

        for (Indexed<T> element : arrayOfIndexed) {
            res.add(element.value());
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
        return removeDuplicates(a, fromIndex, toIndex, false);
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
        return removeDuplicates(a, fromIndex, toIndex, false);
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
        return removeDuplicates(a, fromIndex, toIndex, false);
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
        return removeDuplicates(a, fromIndex, toIndex, false);
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
        return removeDuplicates(a, fromIndex, toIndex, false);
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
        return removeDuplicates(a, fromIndex, toIndex, false);
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
        return removeDuplicates(a, fromIndex, toIndex, false);
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
        return removeDuplicates(a, fromIndex, toIndex, false);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T> List<T> distinct(final T[] a) {
        if (isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        return distinct(a, 0, a.length);
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T> List<T> distinct(final T[] a, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final List<T> result = new ArrayList<>();
        final Set<Object> set = newHashSet();

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
     * @param <T>
     * @param c
     * @return
     */
    public static <T> List<T> distinct(final Collection<? extends T> c) {
        if (isNullOrEmpty(c)) {
            return new ArrayList<>();
        }

        return distinct(c, 0, c.size());
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T> List<T> distinct(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return new ArrayList<>();
        }

        final List<T> result = new ArrayList<>();
        final Set<Object> set = newHashSet();
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
     * @param <T>
     * @param <E>
     * @param a
     * @param keyMapper don't change value of the input parameter.
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> distinctBy(final T[] a, final Throwables.Function<? super T, ?, E> keyMapper) throws E {
        if (isNullOrEmpty(a)) {
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
     * @param <T>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param keyMapper don't change value of the input parameter.
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> distinctBy(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ?, E> keyMapper) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        final List<T> result = new ArrayList<>();
        final Set<Object> set = newHashSet();

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
     * @param <T>
     * @param <E>
     * @param c
     * @param keyMapper don't change value of the input parameter.
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> distinctBy(final Collection<? extends T> c, final Throwables.Function<? super T, ?, E> keyMapper) throws E {
        if (isNullOrEmpty(c)) {
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
     * @param <T>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param keyMapper don't change value of the input parameter.
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> distinctBy(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ?, E> keyMapper) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (isNullOrEmpty(c) && fromIndex == 0 && toIndex == 0) {
            return new ArrayList<>();
        }

        final List<T> result = new ArrayList<>();
        final Set<Object> set = newHashSet();
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

    /**
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param filter the filter
     * @return true, if successful
     * @throws E the e
     */
    @SuppressWarnings("deprecation")
    public static <T, E extends Exception> boolean allMatch(final Collection<? extends T> c, final Throwables.Predicate<? super T, E> filter) throws E {
        return Iterables.allMatch(c, filter);
    }

    /**
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param filter the filter
     * @return true, if successful
     * @throws E the e
     */
    @SuppressWarnings("deprecation")
    public static <T, E extends Exception> boolean allMatch(final T[] a, final Throwables.Predicate<? super T, E> filter) throws E {
        return Iterables.allMatch(a, filter);
    }

    /**
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param filter the filter
     * @return true, if successful
     * @throws E the e
     */
    @SuppressWarnings("deprecation")
    public static <T, E extends Exception> boolean anyMatch(final Collection<? extends T> c, final Throwables.Predicate<? super T, E> filter) throws E {
        return Iterables.anyMatch(c, filter);
    }

    /**
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param filter the filter
     * @return true, if successful
     * @throws E the e
     */
    @SuppressWarnings("deprecation")
    public static <T, E extends Exception> boolean anyMatch(final T[] a, final Throwables.Predicate<? super T, E> filter) throws E {
        return Iterables.anyMatch(a, filter);
    }

    /**
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param filter the filter
     * @return true, if successful
     * @throws E the e
     */
    @SuppressWarnings("deprecation")
    public static <T, E extends Exception> boolean noneMatch(final Collection<? extends T> c, final Throwables.Predicate<? super T, E> filter) throws E {
        return Iterables.noneMatch(c, filter);
    }

    /**
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param filter the filter
     * @return true, if successful
     * @throws E the e
     */
    @SuppressWarnings("deprecation")
    public static <T, E extends Exception> boolean noneMatch(final T[] a, final Throwables.Predicate<? super T, E> filter) throws E {
        return Iterables.noneMatch(a, filter);
    }

    /**
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param c the c
     * @param atLeast the at least
     * @param atMost the at most
     * @param filter the filter
     * @return true, if successful
     * @throws E the e
     */
    @SuppressWarnings("deprecation")
    public static <T, E extends Exception> boolean nMatch(final Collection<? extends T> c, final int atLeast, final int atMost,
            final Throwables.Predicate<? super T, E> filter) throws E {
        return Iterables.nMatch(c, atLeast, atMost, filter);
    }

    /**
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param atLeast the at least
     * @param atMost the at most
     * @param filter the filter
     * @return true, if successful
     * @throws E the e
     */
    @SuppressWarnings("deprecation")
    public static <T, E extends Exception> boolean nMatch(final T[] a, final int atLeast, final int atMost, final Throwables.Predicate<? super T, E> filter)
            throws E {
        return Iterables.nMatch(a, atLeast, atMost, filter);
    }

    /**
     *
     * @param obj
     * @return
     */
    public static String toJSON(final Object obj) {
        return Utils.jsonParser.serialize(obj, Utils.jsc);
    }

    /**
     *
     * @param obj
     * @param prettyFormat
     * @return
     */
    public static String toJSON(final Object obj, final boolean prettyFormat) {
        return Utils.jsonParser.serialize(obj, prettyFormat ? Utils.jscPrettyFormat : Utils.jsc);
    }

    /**
     *
     * @param obj
     * @param config
     * @return
     */
    public static String toJSON(final Object obj, final JSONSerializationConfig config) {
        return Utils.jsonParser.serialize(obj, config);
    }

    /**
     *
     * @param file
     * @param obj
     */
    public static void toJSON(final File file, final Object obj) {
        Utils.jsonParser.serialize(file, obj);
    }

    /**
     *
     * @param file
     * @param obj
     * @param config
     */
    public static void toJSON(final File file, final Object obj, final JSONSerializationConfig config) {
        Utils.jsonParser.serialize(file, obj, config);
    }

    /**
     *
     * @param os
     * @param obj
     */
    public static void toJSON(final OutputStream os, final Object obj) {
        Utils.jsonParser.serialize(os, obj);
    }

    /**
     *
     * @param os
     * @param obj
     * @param config
     */
    public static void toJSON(final OutputStream os, final Object obj, final JSONSerializationConfig config) {
        Utils.jsonParser.serialize(os, obj, config);
    }

    /**
     *
     * @param writer
     * @param obj
     */
    public static void toJSON(final Writer writer, final Object obj) {
        Utils.jsonParser.serialize(writer, obj);
    }

    /**
     *
     * @param writer
     * @param obj
     * @param config
     */
    public static void toJSON(final Writer writer, final Object obj, final JSONSerializationConfig config) {
        Utils.jsonParser.serialize(writer, obj, config);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param json
     * @return
     */
    public static <T> T fromJSON(final Class<? extends T> targetClass, final String json) {
        return Utils.jsonParser.deserialize(targetClass, json);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param json
     * @param config
     * @return
     */
    public static <T> T fromJSON(final Class<? extends T> targetClass, final String json, final JSONDeserializationConfig config) {
        return Utils.jsonParser.deserialize(targetClass, json, config);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param json
     * @return
     */
    public static <T> T fromJSON(final Class<? extends T> targetClass, final File json) {
        return Utils.jsonParser.deserialize(targetClass, json);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param json
     * @param config
     * @return
     */
    public static <T> T fromJSON(final Class<? extends T> targetClass, final File json, final JSONDeserializationConfig config) {
        return Utils.jsonParser.deserialize(targetClass, json, config);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param json
     * @return
     */
    public static <T> T fromJSON(final Class<? extends T> targetClass, final InputStream json) {
        return Utils.jsonParser.deserialize(targetClass, json);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param json
     * @param config
     * @return
     */
    public static <T> T fromJSON(final Class<? extends T> targetClass, final InputStream json, final JSONDeserializationConfig config) {
        return Utils.jsonParser.deserialize(targetClass, json, config);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param json
     * @return
     */
    public static <T> T fromJSON(final Class<? extends T> targetClass, final Reader json) {
        return Utils.jsonParser.deserialize(targetClass, json);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param json
     * @param config
     * @return
     */
    public static <T> T fromJSON(final Class<? extends T> targetClass, final Reader json, final JSONDeserializationConfig config) {
        return Utils.jsonParser.deserialize(targetClass, json, config);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param json
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T> T fromJSON(final Class<? extends T> targetClass, final String json, final int fromIndex, final int toIndex) {
        return Utils.jsonParser.deserialize(targetClass, json, fromIndex, toIndex);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param json
     * @param fromIndex
     * @param toIndex
     * @param config
     * @return
     */
    public static <T> T fromJSON(final Class<? extends T> targetClass, final String json, final int fromIndex, final int toIndex,
            final JSONDeserializationConfig config) {
        return Utils.jsonParser.deserialize(targetClass, json, fromIndex, toIndex, config);
    }

    /**
     *
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param json
     * @return
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final String json) {
        return fromJSON(targetType, json, null);
    }

    /**
     *
     * @param <T>
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
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param json
     * @return
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final File json) {
        return fromJSON(targetType, json, null);
    }

    /**
     *
     * @param <T>
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
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param json
     * @return
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final InputStream json) {
        return fromJSON(targetType, json, null);
    }

    /**
     *
     * @param <T>
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
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param json
     * @return
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final Reader json) {
        return fromJSON(targetType, json, null);
    }

    /**
     *
     * @param <T>
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
     * @param <T>
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
     * @param <T>
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

    /**
     *
     * @param json
     * @return
     */
    public static String formatJSON(final String json) {
        return formatJSON(Object.class, json);
    }

    /**
     *
     * @param type
     * @param json
     * @return
     */
    public static String formatJSON(final Class<?> type, final String json) {
        return toJSON(fromJSON(type, json), Utils.jscPrettyFormat);
    }

    /**
     *
     * @param type
     * @param json
     * @return
     */
    public static String formatJSON(final Type<?> type, final String json) {
        return toJSON(fromJSON(type, json), Utils.jscPrettyFormat);
    }

    /**
     *
     * @param obj
     * @return
     */
    public static String toXML(final Object obj) {
        return Utils.xmlParser.serialize(obj);
    }

    /**
     *
     * @param obj
     * @param prettyFormat
     * @return
     */
    public static String toXML(final Object obj, final boolean prettyFormat) {
        return Utils.xmlParser.serialize(obj, prettyFormat ? Utils.xscPrettyFormat : Utils.xsc);
    }

    /**
     *
     * @param obj
     * @param config
     * @return
     */
    public static String toXML(final Object obj, final XMLSerializationConfig config) {
        return Utils.xmlParser.serialize(obj, config);
    }

    /**
     *
     * @param file
     * @param obj
     */
    public static void toXML(final File file, final Object obj) {
        Utils.xmlParser.serialize(file, obj);
    }

    /**
     *
     * @param file
     * @param obj
     * @param config
     */
    public static void toXML(final File file, final Object obj, final XMLSerializationConfig config) {
        Utils.xmlParser.serialize(file, obj, config);
    }

    /**
     *
     * @param os
     * @param obj
     */
    public static void toXML(final OutputStream os, final Object obj) {
        Utils.xmlParser.serialize(os, obj);
    }

    /**
     *
     * @param os
     * @param obj
     * @param config
     */
    public static void toXML(final OutputStream os, final Object obj, final XMLSerializationConfig config) {
        Utils.xmlParser.serialize(os, obj, config);
    }

    /**
     *
     * @param writer
     * @param obj
     */
    public static void toXML(final Writer writer, final Object obj) {
        Utils.xmlParser.serialize(writer, obj);
    }

    /**
     *
     * @param writer
     * @param obj
     * @param config
     */
    public static void toXML(final Writer writer, final Object obj, final XMLSerializationConfig config) {
        Utils.xmlParser.serialize(writer, obj, config);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param xml
     * @return
     */
    public static <T> T fromXML(final Class<? extends T> targetClass, final String xml) {
        return Utils.xmlParser.deserialize(targetClass, xml);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param xml
     * @param config
     * @return
     */
    public static <T> T fromXML(final Class<? extends T> targetClass, final String xml, final XMLDeserializationConfig config) {
        return Utils.xmlParser.deserialize(targetClass, xml, config);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param xml
     * @return
     */
    public static <T> T fromXML(final Class<? extends T> targetClass, final File xml) {
        return Utils.xmlParser.deserialize(targetClass, xml);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param xml
     * @param config
     * @return
     */
    public static <T> T fromXML(final Class<? extends T> targetClass, final File xml, final XMLDeserializationConfig config) {
        return Utils.xmlParser.deserialize(targetClass, xml, config);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param xml
     * @return
     */
    public static <T> T fromXML(final Class<? extends T> targetClass, final InputStream xml) {
        return Utils.xmlParser.deserialize(targetClass, xml);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param xml
     * @param config
     * @return
     */
    public static <T> T fromXML(final Class<? extends T> targetClass, final InputStream xml, final XMLDeserializationConfig config) {
        return Utils.xmlParser.deserialize(targetClass, xml, config);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param xml
     * @return
     */
    public static <T> T fromXML(final Class<? extends T> targetClass, final Reader xml) {
        return Utils.xmlParser.deserialize(targetClass, xml);
    }

    /**
     *
     * @param <T>
     * @param targetClass
     * @param xml
     * @param config
     * @return
     */
    public static <T> T fromXML(final Class<? extends T> targetClass, final Reader xml, final XMLDeserializationConfig config) {
        return Utils.xmlParser.deserialize(targetClass, xml, config);
    }

    /**
     *
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param xml
     * @return
     */
    public static <T> T fromXML(final Type<? extends T> targetType, final String xml) {
        return fromJSON(targetType, xml, null);
    }

    /**
     *
     * @param <T>
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
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param xml
     * @return
     */
    public static <T> T fromXML(final Type<? extends T> targetType, final File xml) {
        return fromJSON(targetType, xml, null);
    }

    /**
     *
     * @param <T>
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
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param xml
     * @return
     */
    public static <T> T fromXML(final Type<? extends T> targetType, final InputStream xml) {
        return fromJSON(targetType, xml, null);
    }

    /**
     *
     * @param <T>
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
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param xml
     * @return
     */
    public static <T> T fromXML(final Type<? extends T> targetType, final Reader xml) {
        return fromJSON(targetType, xml, null);
    }

    /**
     *
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Entity/Array/Collection/Map}.
     * @param xml
     * @param config
     * @return
     */
    public static <T> T fromXML(final Type<? extends T> targetType, final Reader xml, final XMLDeserializationConfig config) {
        return Utils.xmlParser.deserialize(targetType.clazz(), xml, setConfig(targetType, config, false));
    }

    /**
     * Sets the config.
     *
     * @param <C>
     * @param targetType
     * @param config
     * @param isJSON
     * @return
     */
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

    /**
     * Xml 2 JSON.
     *
     * @param xml
     * @return
     */
    public static String xml2JSON(final String xml) {
        return xml2JSON(Map.class, xml);
    }

    /**
     * Xml 2 JSON.
     *
     * @param cls
     * @param xml
     * @return
     */
    public static String xml2JSON(final Class<?> cls, final String xml) {
        return Utils.jsonParser.serialize(Utils.xmlParser.deserialize(cls, xml), Utils.jsc);
    }

    /**
     * Json 2 XML.
     *
     * @param json
     * @return
     */
    public static String json2XML(final String json) {
        return json2XML(Map.class, json);
    }

    /**
     * Json 2 XML.
     *
     * @param cls
     * @param json
     * @return
     */
    public static String json2XML(final Class<?> cls, final String json) {
        return Utils.xmlParser.serialize(Utils.jsonParser.deserialize(cls, json));
    }

    /**
     *
     * @param cmd
     * @param retryTimes
     * @param retryInterval
     * @param retryCondition
     */
    public static void execute(final Throwables.Runnable<? extends Exception> cmd, final int retryTimes, final long retryInterval,
            final Predicate<? super Exception> retryCondition) {
        try {
            Retry.of(retryTimes, retryInterval, retryCondition).run(cmd);
        } catch (Exception e) {
            throw toRuntimeException(e);
        }
    }

    /**
     *
     * @param <T>
     * @param cmd
     * @param retryTimes
     * @param retryInterval
     * @param retryCondition
     * @return
     */
    public static <T> T execute(final Callable<T> cmd, final int retryTimes, final long retryInterval,
            final BiPredicate<? super T, ? super Exception> retryCondition) {
        try {
            final Retry<T> retry = Retry.of(retryTimes, retryInterval, retryCondition);
            return retry.call(cmd);
        } catch (Exception e) {
            throw toRuntimeException(e);
        }
    }

    /**
     *
     * @param command
     * @return
     */
    public static ContinuableFuture<Void> asyncExecute(final Throwables.Runnable<? extends Exception> command) {
        return asyncExecutor.execute(command);
    }

    /**
     *
     * @param command
     * @param delayInMillis
     * @return
     */
    public static ContinuableFuture<Void> asyncExecute(final Throwables.Runnable<? extends Exception> command, final long delayInMillis) {
        return new ContinuableFuture<>(SCHEDULED_EXECUTOR.schedule(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                command.run();
                return null;
            }
        }, delayInMillis, TimeUnit.MILLISECONDS));
    }

    /**
     *
     * @param commands
     * @return
     */
    @SafeVarargs
    public static List<ContinuableFuture<Void>> asyncExecute(final Throwables.Runnable<? extends Exception>... commands) {
        return asyncExecutor.execute(commands);
    }

    /**
     *
     * @param commands
     * @return
     */
    public static List<ContinuableFuture<Void>> asyncExecute(final List<? extends Throwables.Runnable<? extends Exception>> commands) {
        return asyncExecutor.execute(commands);
    }

    /**
     *
     * @param <T>
     * @param command
     * @return
     */
    public static <T> ContinuableFuture<T> asyncExecute(final Callable<T> command) {
        return asyncExecutor.execute(command);
    }

    /**
     *
     * @param <T>
     * @param command
     * @param delayInMillis
     * @return
     */
    public static <T> ContinuableFuture<T> asyncExecute(final Callable<T> command, final long delayInMillis) {
        return new ContinuableFuture<>(SCHEDULED_EXECUTOR.schedule(command, delayInMillis, TimeUnit.MILLISECONDS));
    }

    /**
     *
     * @param <T>
     * @param commands
     * @return
     */
    @SafeVarargs
    public static <T> List<ContinuableFuture<T>> asyncExecute(final Callable<T>... commands) {
        return asyncExecutor.execute(commands);
    }

    /**
     *
     * @param <T>
     * @param commands
     * @return
     */
    public static <T> List<ContinuableFuture<T>> asyncExecute(final Collection<? extends Callable<T>> commands) {
        return asyncExecutor.execute(commands);
    }

    /**
     *
     * @param cmd
     * @param retryTimes
     * @param retryIntervalInMillis
     * @param retryCondition
     * @return
     */
    public static ContinuableFuture<Void> asyncExecute(final Throwables.Runnable<? extends Exception> cmd, final int retryTimes,
            final long retryIntervalInMillis, final Predicate<? super Exception> retryCondition) {
        return asyncExecutor.execute(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Retry.of(retryTimes, retryIntervalInMillis, retryCondition).run(cmd);
                return null;
            }
        });
    }

    /**
     *
     * @param <T>
     * @param cmd
     * @param retryTimes
     * @param retryIntervalInMillis
     * @param retryCondition
     * @return
     */
    public static <T> ContinuableFuture<T> asyncExecute(final Callable<T> cmd, final int retryTimes, final long retryIntervalInMillis,
            final BiPredicate<? super T, ? super Exception> retryCondition) {
        return asyncExecutor.execute(new Callable<T>() {
            @Override
            public T call() throws Exception {
                final Retry<T> retry = Retry.of(retryTimes, retryIntervalInMillis, retryCondition);
                return retry.call(cmd);
            }
        });
    }

    public static ContinuableFuture<Void> asyncExecute(final Throwables.Runnable<? extends Exception> command, final Executor executor) {
        return ContinuableFuture.run(command, executor);
    }

    public static <T> ContinuableFuture<T> asyncExecute(final Callable<T> command, final Executor executor) {
        return ContinuableFuture.call(command, executor);
    }

    /**
     * To runtime exception.
     *
     * @param e
     * @return
     */
    public static RuntimeException toRuntimeException(Throwable e) {
        return ExceptionUtil.toRuntimeException(e);
    }

    /**
     *
     * @param timeoutInMillis
     */
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

    /**
     *
     * @param timeout
     * @param unit
     */
    public static void sleep(final long timeout, final TimeUnit unit) {
        checkArgNotNull(unit, "unit");

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
     * @param timeout
     * @param unit
     */
    public static void sleepUninterruptibly(final long timeout, final TimeUnit unit) {
        checkArgNotNull(unit, "unit");

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
    public static void runUninterruptibly(final Throwables.Runnable<InterruptedException> cmd) {
        checkArgNotNull(cmd);

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
    public static void runUninterruptibly(final long timeoutInMillis, final Throwables.LongConsumer<InterruptedException> cmd) {
        checkArgNotNull(cmd);

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
    public static void runUninterruptibly(final long timeout, final TimeUnit unit, final Throwables.BiConsumer<Long, TimeUnit, InterruptedException> cmd) {
        checkArgNotNull(unit, "unit");
        checkArgNotNull(cmd);

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
     * @param <T>
     * @param cmd
     * @return
     */
    public static <T> T callUninterruptibly(Throwables.Callable<T, InterruptedException> cmd) {
        checkArgNotNull(cmd);

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
     * @param <T>
     * @param timeoutInMillis
     * @param cmd
     * @return
     */
    public static <T> T callUninterruptibly(final long timeoutInMillis, final Throwables.LongFunction<T, InterruptedException> cmd) {
        checkArgNotNull(cmd);

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
     * @param <T>
     * @param timeout
     * @param unit
     * @param cmd
     * @return
     */
    public static <T> T callUninterruptibly(final long timeout, final TimeUnit unit, final Throwables.BiFunction<Long, TimeUnit, T, InterruptedException> cmd) {
        checkArgNotNull(unit, "unit");
        checkArgNotNull(cmd);

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
     * @param <T>
     * @param obj
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> T println(final T obj) {
        if (obj instanceof Collection) {
            System.out.println(Joiner.with(ELEMENT_SEPARATOR, "[", "]").reuseCachedBuffer(true).appendAll((Collection) obj));
        } else if (obj instanceof Map) {
            System.out.println(Joiner.with(ELEMENT_SEPARATOR, "=", "{", "}").reuseCachedBuffer(true).appendEntries((Map) obj));
        } else {
            System.out.println(toString(obj));
        }

        return obj;
    }

    /**
     *
     * @param <T>
     * @param format
     * @param args
     * @return
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
     * @return
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
     * @param <T>
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
     * @param <R>
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
     * @param <T>
     * @param <R>
     * @param <E>
     * @param init
     * @param func
     * @return
     */
    public static <T, R, E extends Exception> Nullable<R> tryOrEmpty(final T init, final Throwables.Function<? super T, R, E> func) {
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
     * @param <R>
     * @param <E>
     * @param b
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <R, E extends Exception> Nullable<R> ifOrEmpty(final boolean b, final Throwables.Supplier<R, E> supplier) throws E {
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
     * @param <T>
     * @param <R>
     * @param <E>
     * @param b
     * @param init
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> Nullable<R> ifOrEmpty(final boolean b, final T init, final Throwables.Function<? super T, R, E> func) throws E {
        if (b) {
            return Nullable.of(func.apply(init));
        } else {
            return Nullable.empty();
        }
    }

    /**
     * If or else.
     *
     * @param <E1>
     * @param <E2>
     * @param b
     * @param actionForTrue do nothing if it's {@code null} even {@code b} is true.
     * @param actionForFalse do nothing if it's {@code null} even {@code b} is false.
     * @throws E1 the e1
     * @throws E2 the e2
     */
    public static <E1 extends Exception, E2 extends Exception> void ifOrElse(final boolean b, final Throwables.Runnable<E1> actionForTrue,
            final Throwables.Runnable<E2> actionForFalse) throws E1, E2 {
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
     * If or else.
     *
     * @param <T>
     * @param <E1>
     * @param <E2>
     * @param b
     * @param init
     * @param actionForTrue do nothing if it's {@code null} even {@code b} is true.
     * @param actionForFalse do nothing if it's {@code null} even {@code b} is false.
     * @throws E1 the e1
     * @throws E2 the e2
     */
    public static <T, E1 extends Exception, E2 extends Exception> void ifOrElse(final boolean b, final T init,
            final Throwables.Consumer<? super T, E1> actionForTrue, final Throwables.Consumer<? super T, E2> actionForFalse) throws E1, E2 {
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

    public static <T> LazyInitializer<T> lazyInit(final Supplier<T> supplier) {
        return LazyInitializer.of(supplier);
    }

    public static <T, E extends Exception> Throwables.LazyInitializer<T, E> lazyInitialize(final Throwables.Supplier<T, E> supplier) {
        return Throwables.LazyInitializer.of(supplier);
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T> ObjIterator<T> iterate(final T[] a) {
        return ObjIterator.of(a);
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T> Iterator<T> iterate(final Iterable<T> iterable) {
        return iterable == null ? ObjIterator.empty() : iterable.iterator();
    }

    /**
     *
     * @param a
     * @param b
     * @return true, if successful
     */
    public static boolean disjoint(final Object[] a, final Object[] b) {
        if (isNullOrEmpty(a) || isNullOrEmpty(b)) {
            return true;
        }

        return a.length >= b.length ? disjoint(Arrays.asList(a), asSet(b)) : disjoint(asSet(a), Arrays.asList(b));
    }

    /**
     * Returns {@code true} if the two specified arrays have no elements in common.
     *
     * @param c1
     * @param c2
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

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param nextSelector
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> merge(final T[] a, final T[] b, final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector)
            throws E {
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? new ArrayList<>() : asList(b);
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
                    if (nextSelector.apply(a[cursorA], b[cursorB]) == MergeResult.TAKE_FIRST) {
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

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param nextSelector
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> merge(final Collection<? extends T> a, final Collection<? extends T> b,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) throws E {
        if (isNullOrEmpty(a)) {
            return isNullOrEmpty(b) ? new ArrayList<>() : new ArrayList<>(b);
        } else if (isNullOrEmpty(b)) {
            return new ArrayList<>(a);
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
                    if (nextSelector.apply(nextA, (nextB = iterB.next())) == MergeResult.TAKE_FIRST) {
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
                    if (nextSelector.apply((nextA = iterA.next()), nextB) == MergeResult.TAKE_FIRST) {
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
                    if (nextSelector.apply((nextA = iterA.next()), (nextB = iterB.next())) == MergeResult.TAKE_FIRST) {
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

    /**
     *
     * @param <A>
     * @param <B>
     * @param <R>
     * @param <E>
     * @param a
     * @param b
     * @param zipFunction
     * @return
     * @throws E the e
     */
    public static <A, B, R, E extends Exception> List<R> zip(final A[] a, final B[] b, final Throwables.BiFunction<? super A, ? super B, R, E> zipFunction)
            throws E {
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

    /**
     *
     * @param <A>
     * @param <B>
     * @param <R>
     * @param <E>
     * @param a
     * @param b
     * @param zipFunction
     * @return
     * @throws E the e
     */
    public static <A, B, R, E extends Exception> List<R> zip(final Collection<A> a, final Collection<B> b,
            final Throwables.BiFunction<? super A, ? super B, R, E> zipFunction) throws E {
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

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <R>
     * @param <E>
     * @param a
     * @param b
     * @param c
     * @param zipFunction
     * @return
     * @throws E the e
     */
    public static <A, B, C, R, E extends Exception> List<R> zip(final A[] a, final B[] b, final C[] c,
            final Throwables.TriFunction<? super A, ? super B, ? super C, R, E> zipFunction) throws E {
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

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <R>
     * @param <E>
     * @param a
     * @param b
     * @param c
     * @param zipFunction
     * @return
     * @throws E the e
     */
    public static <A, B, C, R, E extends Exception> List<R> zip(final Collection<A> a, final Collection<B> b, final Collection<C> c,
            final Throwables.TriFunction<? super A, ? super B, ? super C, R, E> zipFunction) throws E {
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

    /**
     *
     * @param <A>
     * @param <B>
     * @param <R>
     * @param <E>
     * @param a
     * @param b
     * @param valueForNoneA
     * @param valueForNoneB
     * @param zipFunction
     * @return
     * @throws E the e
     */
    public static <A, B, R, E extends Exception> List<R> zip(final A[] a, final B[] b, final A valueForNoneA, final B valueForNoneB,
            final Throwables.BiFunction<? super A, ? super B, R, E> zipFunction) throws E {
        final int lenA = len(a);
        final int lenB = len(b);
        final int maxLen = max(lenA, lenB);
        final List<R> result = new ArrayList<>(maxLen);

        for (int i = 0; i < maxLen; i++) {
            result.add(zipFunction.apply(i < lenA ? a[i] : valueForNoneA, i < lenB ? b[i] : valueForNoneB));
        }

        return result;
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <R>
     * @param <E>
     * @param a
     * @param b
     * @param valueForNoneA
     * @param valueForNoneB
     * @param zipFunction
     * @return
     * @throws E the e
     */
    public static <A, B, R, E extends Exception> List<R> zip(final Collection<A> a, final Collection<B> b, final A valueForNoneA, final B valueForNoneB,
            final Throwables.BiFunction<? super A, ? super B, R, E> zipFunction) throws E {
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

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <R>
     * @param <E>
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA
     * @param valueForNoneB
     * @param valueForNoneC
     * @param zipFunction
     * @return
     * @throws E the e
     */
    public static <A, B, C, R, E extends Exception> List<R> zip(final A[] a, final B[] b, final C[] c, final A valueForNoneA, final B valueForNoneB,
            final C valueForNoneC, final Throwables.TriFunction<? super A, ? super B, ? super C, R, E> zipFunction) throws E {
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

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <R>
     * @param <E>
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA
     * @param valueForNoneB
     * @param valueForNoneC
     * @param zipFunction
     * @return
     * @throws E the e
     */
    public static <A, B, C, R, E extends Exception> List<R> zip(final Collection<A> a, final Collection<B> b, final Collection<C> c, final A valueForNoneA,
            final B valueForNoneB, final C valueForNoneC, final Throwables.TriFunction<? super A, ? super B, ? super C, R, E> zipFunction) throws E {
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
     * @param <T>
     * @param <L>
     * @param <R>
     * @param <E>
     * @param c
     * @param unzip the second parameter is an output parameter.
     * @return
     * @throws E the e
     */
    public static <T, L, R, E extends Exception> Pair<List<L>, List<R>> unzip(final Collection<? extends T> c,
            final Throwables.BiConsumer<? super T, Pair<L, R>, E> unzip) throws E {
        final int len = size(c);

        final List<L> l = new ArrayList<>(len);
        final List<R> r = new ArrayList<>(len);
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
     * @param <T>
     * @param <L>
     * @param <R>
     * @param <LC>
     * @param <RC>
     * @param <E>
     * @param c
     * @param unzip the second parameter is an output parameter.
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, L, R, LC extends Collection<L>, RC extends Collection<R>, E extends Exception> Pair<LC, RC> unzip(final Collection<? extends T> c,
            final Throwables.BiConsumer<? super T, Pair<L, R>, E> unzip, final IntFunction<? extends Collection<?>> supplier) throws E {
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
     * @param <T>
     * @param <L>
     * @param <M>
     * @param <R>
     * @param <E>
     * @param c
     * @param unzip the second parameter is an output parameter.
     * @return
     * @throws E the e
     */
    public static <T, L, M, R, E extends Exception> Triple<List<L>, List<M>, List<R>> unzipp(final Collection<? extends T> c,
            final Throwables.BiConsumer<? super T, Triple<L, M, R>, E> unzip) throws E {
        final int len = size(c);

        final List<L> l = new ArrayList<>(len);
        final List<M> m = new ArrayList<>(len);
        final List<R> r = new ArrayList<>(len);
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
     * @param <T>
     * @param <L>
     * @param <M>
     * @param <R>
     * @param <LC>
     * @param <MC>
     * @param <RC>
     * @param <E>
     * @param c
     * @param unzip the second parameter is an output parameter.
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, L, M, R, LC extends Collection<L>, MC extends Collection<M>, RC extends Collection<R>, E extends Exception> Triple<LC, MC, RC> unzipp(
            final Collection<? extends T> c, final Throwables.BiConsumer<? super T, Triple<L, M, R>, E> unzip,
            final IntFunction<? extends Collection<?>> supplier) throws E {
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

    //    /**
    //     *
    //     * @param <T>
    //     * @param <U>
    //     * @param a
    //     * @param b
    //     * @return
    //     */
    //    @SuppressWarnings("deprecation")
    //    public static <T, U> List<Pair<T, U>> crossJoin(final Collection<T> a, final Collection<U> b) {
    //        return Iterables.crossJoin(a, b);
    //    }
    //
    //    /**
    //     *
    //     * @param <T>
    //     * @param <U>
    //     * @param <R>
    //     * @param <E>
    //     * @param a
    //     * @param b
    //     * @param func
    //     * @return
    //     * @throws E
    //     */
    //    @SuppressWarnings("deprecation")
    //    public static <T, U, R, E extends Exception> List<R> crossJoin(final Collection<T> a, final Collection<U> b,
    //            final Throwables.BiFunction<? super T, ? super U, R, E> func) throws E {
    //        return Iterables.crossJoin(a, b, func);
    //    }
    //
    //    /**
    //     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Seq</code> and <i>m</i> is the size of specified collection <code>b</code>.
    //     *
    //     * @param <T> the generic type
    //     * @param <U> the generic type
    //     * @param <E> the element type
    //     * @param <E2> the generic type
    //     * @param a the a
    //     * @param b the b
    //     * @param leftKeyMapper the left key mapper
    //     * @param rightKeyMapper the right key mapper
    //     * @return the list
    //     * @throws E the e
    //     * @throws E2 the e2
    //     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-full-join">sql join</a>
    //     */
    //    @SuppressWarnings("deprecation")
    //    public static <T, U, K, E extends Exception, E2 extends Exception> List<Pair<T, U>> innerJoin(final Collection<T> a, final Collection<U> b,
    //            final Throwables.Function<? super T, ? extends K, E> leftKeyMapper, final Throwables.Function<? super U, ? extends K, E2> rightKeyMapper)
    //            throws E, E2 {
    //        return Iterables.innerJoin(a, b, leftKeyMapper, rightKeyMapper);
    //    }
    //
    //    /**
    //     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Seq</code> and <i>m</i> is the size of specified collection <code>b</code>.
    //     *
    //     * @param <T> the generic type
    //     * @param <U> the generic type
    //     * @param <E> the element type
    //     * @param a the a
    //     * @param b the b
    //     * @param predicate the predicate
    //     * @return the list
    //     * @throws E the e
    //     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-full-join">sql join</a>
    //     */
    //    @SuppressWarnings("deprecation")
    //    public static <T, U, E extends Exception> List<Pair<T, U>> innerJoin(final Collection<T> a, final Collection<U> b,
    //            final Throwables.BiPredicate<? super T, ? super U, E> predicate) throws E {
    //        return Iterables.innerJoin(a, b, predicate);
    //    }
    //
    //    /**
    //     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Seq</code> and <i>m</i> is the size of specified collection <code>b</code>.
    //     *
    //     * @param <T> the generic type
    //     * @param <U> the generic type
    //     * @param <E> the element type
    //     * @param <E2> the generic type
    //     * @param a the a
    //     * @param b the b
    //     * @param leftKeyMapper the left key mapper
    //     * @param rightKeyMapper the right key mapper
    //     * @return the list
    //     * @throws E the e
    //     * @throws E2 the e2
    //     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-full-join">sql join</a>
    //     */
    //    @SuppressWarnings("deprecation")
    //    public static <T, U, K, E extends Exception, E2 extends Exception> List<Pair<T, U>> fullJoin(final Collection<T> a, final Collection<U> b,
    //            final Throwables.Function<? super T, ? extends K, E> leftKeyMapper, final Throwables.Function<? super U, ? extends K, E2> rightKeyMapper)
    //            throws E, E2 {
    //        return Iterables.fullJoin(a, b, leftKeyMapper, rightKeyMapper);
    //    }
    //
    //    /**
    //     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Seq</code> and <i>m</i> is the size of specified collection <code>b</code>.
    //     *
    //     * @param <T> the generic type
    //     * @param <U> the generic type
    //     * @param <E> the element type
    //     * @param a the a
    //     * @param b the b
    //     * @param predicate the predicate
    //     * @return the list
    //     * @throws E the e
    //     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-full-join">sql join</a>
    //     */
    //    @SuppressWarnings("deprecation")
    //    public static <T, U, E extends Exception> List<Pair<T, U>> fullJoin(final Collection<T> a, final Collection<U> b,
    //            final Throwables.BiPredicate<? super T, ? super U, E> predicate) throws E {
    //        return Iterables.fullJoin(a, b, predicate);
    //    }
    //
    //    /**
    //     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Seq</code> and <i>m</i> is the size of specified collection <code>b</code>.
    //     *
    //     * @param <T> the generic type
    //     * @param <U> the generic type
    //     * @param <E> the element type
    //     * @param <E2> the generic type
    //     * @param a the a
    //     * @param b the b
    //     * @param leftKeyMapper the left key mapper
    //     * @param rightKeyMapper the right key mapper
    //     * @return the list
    //     * @throws E the e
    //     * @throws E2 the e2
    //     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-full-join">sql join</a>
    //     */
    //    @SuppressWarnings("deprecation")
    //    public static <T, U, K, E extends Exception, E2 extends Exception> List<Pair<T, U>> leftJoin(final Collection<T> a, final Collection<U> b,
    //            final Throwables.Function<? super T, ? extends K, E> leftKeyMapper, final Throwables.Function<? super U, ? extends K, E2> rightKeyMapper)
    //            throws E, E2 {
    //        return Iterables.leftJoin(a, b, leftKeyMapper, rightKeyMapper);
    //    }
    //
    //    /**
    //     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Seq</code> and <i>m</i> is the size of specified collection <code>b</code>.
    //     *
    //     * @param <T> the generic type
    //     * @param <U> the generic type
    //     * @param <E> the element type
    //     * @param a the a
    //     * @param b the b
    //     * @param predicate the predicate
    //     * @return the list
    //     * @throws E the e
    //     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-full-join">sql join</a>
    //     */
    //    @SuppressWarnings("deprecation")
    //    public static <T, U, E extends Exception> List<Pair<T, U>> leftJoin(final Collection<T> a, final Collection<U> b,
    //            final Throwables.BiPredicate<? super T, ? super U, E> predicate) throws E {
    //        return Iterables.leftJoin(a, b, predicate);
    //    }
    //
    //    /**
    //     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Seq</code> and <i>m</i> is the size of specified collection <code>b</code>.
    //     *
    //     * @param <T> the generic type
    //     * @param <U> the generic type
    //     * @param <E> the element type
    //     * @param <E2> the generic type
    //     * @param a the a
    //     * @param b the b
    //     * @param leftKeyMapper the left key mapper
    //     * @param rightKeyMapper the right key mapper
    //     * @return the list
    //     * @throws E the e
    //     * @throws E2 the e2
    //     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-full-join">sql join</a>
    //     */
    //    @SuppressWarnings("deprecation")
    //    public static <T, U, K, E extends Exception, E2 extends Exception> List<Pair<T, U>> rightJoin(final Collection<T> a, final Collection<U> b,
    //            final Throwables.Function<? super T, ? extends K, E> leftKeyMapper, final Throwables.Function<? super U, ? extends K, E2> rightKeyMapper)
    //            throws E, E2 {
    //        return Iterables.rightJoin(a, b, leftKeyMapper, rightKeyMapper);
    //    }
    //
    //    /**
    //     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Seq</code> and <i>m</i> is the size of specified collection <code>b</code>.
    //     *
    //     * @param <T> the generic type
    //     * @param <U> the generic type
    //     * @param <E> the element type
    //     * @param a the a
    //     * @param b the b
    //     * @param predicate the predicate
    //     * @return the list
    //     * @throws E the e
    //     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-full-join">sql join</a>
    //     */
    //    @SuppressWarnings("deprecation")
    //    public static <T, U, E extends Exception> List<Pair<T, U>> rightJoin(final Collection<T> a, final Collection<U> b,
    //            final Throwables.BiPredicate<? super T, ? super U, E> predicate) throws E {
    //        return Iterables.rightJoin(a, b, predicate);
    //    }
}
