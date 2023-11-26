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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.MayReturnNull;
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
import com.landawn.abacus.util.Fn.IntFunctions;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.Iterables.Slice;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.Tuple.Tuple5;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * <p>
 * Note: This class includes codes copied from Apache Commons Lang, Google Guava and other open source projects under the Apache License 2.0.
 * The methods copied from other libraries/frameworks/projects may be modified in this class.
 * </p>
 * Class <code>N</code> is a general java utility class. It provides the most daily used operations for Object/primitive types/String/Array/Collection/Map/Bean...:
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
 * @see com.landawn.abacus.util.Array
 * @see com.landawn.abacus.util.Iterables
 * @see com.landawn.abacus.util.Iterators
 * @see com.landawn.abacus.util.Maps
 * @see com.landawn.abacus.util.Strings
 * @see com.landawn.abacus.util.IOUtil
 */
@SuppressWarnings({ "java:S1192", "java:S6539" })
public final class N extends CommonUtil { // public final class N extends π implements ℕ, ℂ, ℚ, ℝ, ℤ { //  Error while storing the mojo status in Maven

    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    private static final float LOAD_FACTOR_FOR_FLAT_MAP = 1.75f;

    private static final int LOAD_FACTOR_FOR_TWO_FLAT_MAP = 2;

    static final AsyncExecutor asyncExecutor = new AsyncExecutor(//
            max(64, IOUtil.CPU_CORES * 8), // coreThreadPoolSize
            max(128, IOUtil.CPU_CORES * 16), // maxThreadPoolSize
            180L, TimeUnit.SECONDS);

    static final ScheduledExecutorService SCHEDULED_EXECUTOR;

    static {
        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(Math.max(64, CPU_CORES));
        //    executor.setKeepAliveTime(180, TimeUnit.SECONDS);
        //    executor.allowCoreThreadTimeOut(true);
        //    executor.setRemoveOnCancelPolicy(true);
        SCHEDULED_EXECUTOR = MoreExecutors.getExitingScheduledExecutorService(executor);
    }

    private N() {
        // Utility class.
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static int occurrencesOf(final boolean[] a, final boolean valueToFind) {
        if (isEmpty(a)) {
            return 0;
        }

        int occurrences = 0;

        for (boolean element : a) {
            if (element == valueToFind) {
                occurrences++;
            }
        }

        return occurrences;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static int occurrencesOf(final char[] a, final char valueToFind) {
        if (isEmpty(a)) {
            return 0;
        }

        int occurrences = 0;

        for (char element : a) {
            if (element == valueToFind) {
                occurrences++;
            }
        }

        return occurrences;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static int occurrencesOf(final byte[] a, final byte valueToFind) {
        if (isEmpty(a)) {
            return 0;
        }

        int occurrences = 0;

        for (byte element : a) {
            if (element == valueToFind) {
                occurrences++;
            }
        }

        return occurrences;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static int occurrencesOf(final short[] a, final short valueToFind) {
        if (isEmpty(a)) {
            return 0;
        }

        int occurrences = 0;

        for (short element : a) {
            if (element == valueToFind) {
                occurrences++;
            }
        }

        return occurrences;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static int occurrencesOf(final int[] a, final int valueToFind) {
        if (isEmpty(a)) {
            return 0;
        }

        int occurrences = 0;

        for (int element : a) {
            if (element == valueToFind) {
                occurrences++;
            }
        }

        return occurrences;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static int occurrencesOf(final long[] a, final long valueToFind) {
        if (isEmpty(a)) {
            return 0;
        }

        int occurrences = 0;

        for (long element : a) {
            if (element == valueToFind) {
                occurrences++;
            }
        }

        return occurrences;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static int occurrencesOf(final float[] a, final float valueToFind) {
        if (isEmpty(a)) {
            return 0;
        }

        int occurrences = 0;

        for (float element : a) {
            if (Float.compare(element, valueToFind) == 0) {
                occurrences++;
            }
        }

        return occurrences;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static int occurrencesOf(final double[] a, final double valueToFind) {
        if (isEmpty(a)) {
            return 0;
        }

        int occurrences = 0;

        for (double element : a) {
            if (Double.compare(element, valueToFind) == 0) {
                occurrences++;
            }
        }

        return occurrences;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static int occurrencesOf(final Object[] a, final Object valueToFind) {
        if (isEmpty(a)) {
            return 0;
        }

        int occurrences = 0;

        if (valueToFind == null) {
            for (Object element : a) {
                if (element == null) {
                    occurrences++;
                }
            }
        } else {
            for (Object element : a) {
                if (valueToFind.equals(element)) {
                    occurrences++;
                }
            }
        }

        return occurrences;
    }

    /**
     *
     * @param c
     * @param valueToFind
     * @return
     * @see java.util.Collections#frequency(Collection, Object)
     */
    public static int occurrencesOf(final Iterable<?> c, final Object valueToFind) {
        if (c == null) {
            return 0;
        }

        long occurrences = 0;

        if (valueToFind == null) {
            for (Object e : c) {
                if (e == null) {
                    occurrences++;
                }
            }
        } else {
            for (Object e : c) {
                if (equals(e, valueToFind)) {
                    occurrences++;
                }
            }
        }

        return Numbers.toIntExact(occurrences);
    }

    /**
     *
     *
     * @param iter
     * @param valueToFind
     * @return
     * @throws ArithmeticException if the {@code occurrences} is bigger than {@code Integer.MAX_VALUE}
     * @see Iterators#occurrencesOf(Iterator, Object)
     */
    public static int occurrencesOf(final Iterator<?> iter, final Object valueToFind) throws ArithmeticException {
        return Numbers.toIntExact(Iterators.occurrencesOf(iter, valueToFind));
    }

    /**
     *
     * @param str
     * @param ch
     * @return
     * @see Strings#countMatches(String, char)
     */
    public static int occurrencesOf(final String str, final char ch) {
        return Strings.countMatches(str, ch);
    }

    /**
     *
     * @param str
     * @param substr
     * @return
     * @see Strings#countMatches(String, String)
     */
    public static int occurrencesOf(final String str, final String substr) {
        return Strings.countMatches(str, substr);
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     * @see Multiset#create(Collection)
     */
    public static <T> Map<T, Integer> occurrencesMap(final T[] a) {
        return occurrencesMap(a, Suppliers.<T, Integer> ofMap());
    }

    /**
     *
     * @param <T>
     * @param a
     * @param mapSupplier
     * @return
     * @see Multiset#create(Collection)
     */
    public static <T> Map<T, Integer> occurrencesMap(final T[] a, final Supplier<Map<T, Integer>> mapSupplier) {
        if (isEmpty(a)) {
            return mapSupplier.get();
        }

        final Map<T, Integer> map = mapSupplier.get();

        for (T e : a) {
            map.merge(e, 1, (o, n) -> o + n);
        }

        return map;
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     * @see Multiset#create(Collection)
     */
    public static <T> Map<T, Integer> occurrencesMap(final Iterable<? extends T> c) {
        return occurrencesMap(c, Suppliers.<T, Integer> ofMap());
    }

    /**
     *
     * @param <T>
     * @param c
     * @param mapSupplier
     * @return
     * @see Multiset#create(Collection)
     */
    public static <T> Map<T, Integer> occurrencesMap(final Iterable<? extends T> c, final Supplier<Map<T, Integer>> mapSupplier) {
        if (c == null) {
            return mapSupplier.get();
        }

        final Multiset<T> multiset = new Multiset<>();

        for (T e : c) {
            multiset.add(e, 1);
        }

        final Map<T, Integer> map = mapSupplier.get();

        for (T e : multiset) {
            map.put(e, multiset.get(e));
        }

        return map;
    }

    /**
     *
     *
     * @param <T>
     * @param iter
     * @return
     * @see Multiset#create(Iterator)
     */
    public static <T> Map<T, Integer> occurrencesMap(final Iterator<? extends T> iter) {
        return occurrencesMap(iter, Suppliers.<T, Integer> ofMap());
    }

    /**
     *
     * @param <T>
     * @param iter
     * @param mapSupplier
     * @return
     * @see Multiset#create(Iterator)
     */
    public static <T> Map<T, Integer> occurrencesMap(final Iterator<? extends T> iter, final Supplier<Map<T, Integer>> mapSupplier) {
        if (iter == null) {
            return mapSupplier.get();
        }

        final LongMultiset<T> multiset = new LongMultiset<>();

        while (iter.hasNext()) {
            multiset.add(iter.next(), 1);
        }

        final Map<T, Integer> map = mapSupplier.get();

        for (T e : multiset) {
            map.put(e, Numbers.toIntExact(multiset.get(e)));
        }

        return map;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static boolean contains(final boolean[] a, final boolean valueToFind) {
        return indexOf(a, valueToFind) != INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static boolean contains(final char[] a, final char valueToFind) {
        return indexOf(a, valueToFind) != INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static boolean contains(final byte[] a, final byte valueToFind) {
        return indexOf(a, valueToFind) != INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static boolean contains(final short[] a, final short valueToFind) {
        return indexOf(a, valueToFind) != INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static boolean contains(final int[] a, final int valueToFind) {
        return indexOf(a, valueToFind) != INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static boolean contains(final long[] a, final long valueToFind) {
        return indexOf(a, valueToFind) != INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static boolean contains(final float[] a, final float valueToFind) {
        return indexOf(a, valueToFind) != INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static boolean contains(final double[] a, final double valueToFind) {
        return indexOf(a, valueToFind) != INDEX_NOT_FOUND;
    }

    /**
     *
     * @param a
     * @param valueToFind
     * @return
     */
    public static boolean contains(final Object[] a, final Object valueToFind) {
        return indexOf(a, valueToFind) != INDEX_NOT_FOUND;
    }

    /**
     *
     * @param c
     * @param valueToFind
     * @return
     */
    public static boolean contains(final Collection<?> c, final Object valueToFind) {
        if (isEmpty(c)) {
            return false;
        }

        return c.contains(valueToFind);
    }

    /**
     *
     *
     * @param c
     * @param valueToFind
     * @return
     */
    public static boolean contains(final Iterable<?> c, final Object valueToFind) {
        if (c == null) {
            return false;
        }

        for (Object e : c) {
            if (equals(e, valueToFind)) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     *
     * @param iter
     * @param valueToFind
     * @return
     */
    public static boolean contains(final Iterator<?> iter, final Object valueToFind) {
        if (iter == null) {
            return false;
        }

        while (iter.hasNext()) {
            if (equals(iter.next(), valueToFind)) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @param c
     * @param valuesToFind the objs to find
     * @return
     */
    public static boolean containsAll(final Collection<?> c, final Collection<?> valuesToFind) {
        if (isEmpty(valuesToFind)) {
            return true;
        } else if (isEmpty(c)) {
            return false;
        }

        return c.containsAll(valuesToFind);
    }

    /**
     *
     * @param c
     * @param valuesToFind the objs to find
     * @return
     */
    @SafeVarargs
    public static boolean containsAll(final Collection<?> c, final Object... valuesToFind) {
        if (isEmpty(valuesToFind)) {
            return true;
        } else if (isEmpty(c)) {
            return false;
        }

        return c.containsAll(Array.asList(valuesToFind));
    }

    /**
     *
     *
     * @param c
     * @param valuesToFind
     * @return
     */
    public static boolean containsAll(final Iterable<?> c, final Collection<?> valuesToFind) {
        if (isEmpty(valuesToFind)) {
            return true;
        } else if (c == null) {
            return false;
        }

        final Set<?> set = new HashSet<>(valuesToFind);

        for (Object e : c) {
            if (set.remove(e) && (set.size() == 0)) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     *
     * @param iter
     * @param valuesToFind
     * @return
     */
    public static boolean containsAll(final Iterator<?> iter, final Collection<?> valuesToFind) {
        if (isEmpty(valuesToFind)) {
            return true;
        } else if (iter == null) {
            return false;
        }

        final Set<?> set = new HashSet<>(valuesToFind);

        while (iter.hasNext()) {
            if (set.remove(iter.next()) && (set.size() == 0)) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @param c
     * @param valuesToFind the objs to find
     * @return
     */
    public static boolean containsAny(final Collection<?> c, final Collection<?> valuesToFind) {
        if (isEmpty(c) || isEmpty(valuesToFind)) {
            return false;
        }

        return !disjoint(c, valuesToFind);
    }

    /**
     *
     * @param c
     * @param valuesToFind the objs to find
     * @return
     */
    @SafeVarargs
    public static boolean containsAny(final Collection<?> c, final Object... valuesToFind) {
        if (isEmpty(c) || isEmpty(valuesToFind)) {
            return false;
        }

        return !disjoint(c, Array.asList(valuesToFind));
    }

    /**
     *
     *
     * @param c
     * @param valuesToFind
     * @return
     */
    public static boolean containsAny(final Iterable<?> c, final Set<?> valuesToFind) {
        if (c == null || isEmpty(valuesToFind)) {
            return false;
        }

        for (Object e : c) {
            if (valuesToFind.contains(e)) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     *
     * @param iter
     * @param valuesToFind
     * @return
     */
    public static boolean containsAny(final Iterator<?> iter, final Set<?> valuesToFind) {
        if (iter == null || isEmpty(valuesToFind)) {
            return false;
        }

        while (iter.hasNext()) {
            if (valuesToFind.contains(iter.next())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns a read-only <code>Seq</code>.
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return the immutable collection<? extends t>
     */
    public static <T> ImmutableList<T> slice(final T[] a, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a)) {
            return ImmutableList.empty();
        }

        return slice(Array.asList(a), fromIndex, toIndex);
    }

    /**
     * Returns a read-only <code>ImmutableCollection</code>.
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return the immutable collection<? extends t>
     */
    @SuppressWarnings("deprecation")
    public static <T> ImmutableList<T> slice(final List<? extends T> c, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (isEmpty(c)) {
            return ImmutableList.empty();
        }

        return ImmutableList.wrap(((List<T>) c).subList(fromIndex, toIndex));
    }

    /**
     * Returns a read-only <code>ImmutableCollection</code>.
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return the immutable collection<? extends t>
     */
    public static <T> ImmutableCollection<T> slice(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (isEmpty(c)) {
            return ImmutableList.empty();
        }

        if (c instanceof List) {
            return slice((List<T>) c, fromIndex, toIndex);
        }

        return new Slice<>(c, fromIndex, toIndex);
    }

    /**
     *
     * @param <T>
     * @param iter
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T> ObjIterator<T> slice(final Iterator<? extends T> iter, final long fromIndex, final long toIndex) {
        if (fromIndex < 0 || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException("Index range [" + fromIndex + ", " + toIndex + "] is out-of-bounds");
        }

        if (iter == null || fromIndex == toIndex) {
            return ObjIterator.empty();
        }

        return Iterators.skipAndLimit(iter, fromIndex, toIndex - fromIndex);
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same size (the final list may be smaller),
     * or an empty List if the specified array is {@code null} or empty.
     *
     * @param a
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<boolean[]> split(final boolean[] a, final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize"); //NOSONAR

        if (isEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = a.length;
        final List<boolean[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int fromIndex = 0, toIndex = a.length; fromIndex < toIndex; fromIndex += chunkSize) {
            res.add(copyOfRange(a, fromIndex, fromIndex <= toIndex - chunkSize ? fromIndex + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is {@code null} or empty.
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

        if (isEmpty(a) || fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final int len = toIndex - fromIndex;
        final List<boolean[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int i = fromIndex; i < toIndex; i = i <= toIndex - chunkSize ? i + chunkSize : toIndex) {
            res.add(copyOfRange(a, i, i <= toIndex - chunkSize ? i + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is {@code null} or empty.
     *
     * @param a
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<char[]> split(final char[] a, final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        if (isEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = a.length;
        final List<char[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int fromIndex = 0, toIndex = a.length; fromIndex < toIndex; fromIndex += chunkSize) {
            res.add(copyOfRange(a, fromIndex, fromIndex <= toIndex - chunkSize ? fromIndex + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is {@code null} or empty.
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

        if (isEmpty(a) || fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final int len = toIndex - fromIndex;
        final List<char[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int i = fromIndex; i < toIndex; i = i <= toIndex - chunkSize ? i + chunkSize : toIndex) {
            res.add(copyOfRange(a, i, i <= toIndex - chunkSize ? i + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is {@code null} or empty.
     *
     * @param a
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<byte[]> split(final byte[] a, final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        if (isEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = a.length;
        final List<byte[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int fromIndex = 0, toIndex = a.length; fromIndex < toIndex; fromIndex += chunkSize) {
            res.add(copyOfRange(a, fromIndex, fromIndex <= toIndex - chunkSize ? fromIndex + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is {@code null} or empty.
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

        if (isEmpty(a) || fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final int len = toIndex - fromIndex;
        final List<byte[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int i = fromIndex; i < toIndex; i = i <= toIndex - chunkSize ? i + chunkSize : toIndex) {
            res.add(copyOfRange(a, i, i <= toIndex - chunkSize ? i + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is {@code null} or empty.
     *
     * @param a
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<short[]> split(final short[] a, final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        if (isEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = a.length;
        final List<short[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int fromIndex = 0, toIndex = a.length; fromIndex < toIndex; fromIndex += chunkSize) {
            res.add(copyOfRange(a, fromIndex, fromIndex <= toIndex - chunkSize ? fromIndex + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is {@code null} or empty.
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

        if (isEmpty(a) || fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final int len = toIndex - fromIndex;
        final List<short[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int i = fromIndex; i < toIndex; i = i <= toIndex - chunkSize ? i + chunkSize : toIndex) {
            res.add(copyOfRange(a, i, i <= toIndex - chunkSize ? i + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is {@code null} or empty.
     *
     * @param a
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<int[]> split(final int[] a, final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        if (isEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = a.length;
        final List<int[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int fromIndex = 0, toIndex = a.length; fromIndex < toIndex; fromIndex += chunkSize) {
            res.add(copyOfRange(a, fromIndex, fromIndex <= toIndex - chunkSize ? fromIndex + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is {@code null} or empty.
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

        if (isEmpty(a) || fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final int len = toIndex - fromIndex;
        final List<int[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int i = fromIndex; i < toIndex; i = i <= toIndex - chunkSize ? i + chunkSize : toIndex) {
            res.add(copyOfRange(a, i, i <= toIndex - chunkSize ? i + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is {@code null} or empty.
     *
     * @param a
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<long[]> split(final long[] a, final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        if (isEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = a.length;
        final List<long[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int fromIndex = 0, toIndex = a.length; fromIndex < toIndex; fromIndex += chunkSize) {
            res.add(copyOfRange(a, fromIndex, fromIndex <= toIndex - chunkSize ? fromIndex + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is {@code null} or empty.
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

        if (isEmpty(a) || fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final int len = toIndex - fromIndex;
        final List<long[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int i = fromIndex; i < toIndex; i = i <= toIndex - chunkSize ? i + chunkSize : toIndex) {
            res.add(copyOfRange(a, i, i <= toIndex - chunkSize ? i + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is {@code null} or empty.
     *
     * @param a
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<float[]> split(final float[] a, final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        if (isEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = a.length;
        final List<float[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int fromIndex = 0, toIndex = a.length; fromIndex < toIndex; fromIndex += chunkSize) {
            res.add(copyOfRange(a, fromIndex, fromIndex <= toIndex - chunkSize ? fromIndex + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is {@code null} or empty.
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

        if (isEmpty(a) || fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final int len = toIndex - fromIndex;
        final List<float[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int i = fromIndex; i < toIndex; i = i <= toIndex - chunkSize ? i + chunkSize : toIndex) {
            res.add(copyOfRange(a, i, i <= toIndex - chunkSize ? i + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is {@code null} or empty.
     *
     * @param a
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static List<double[]> split(final double[] a, final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        if (isEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = a.length;
        final List<double[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int fromIndex = 0, toIndex = a.length; fromIndex < toIndex; fromIndex += chunkSize) {
            res.add(copyOfRange(a, fromIndex, fromIndex <= toIndex - chunkSize ? fromIndex + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is {@code null} or empty.
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

        if (isEmpty(a) || fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final int len = toIndex - fromIndex;
        final List<double[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int i = fromIndex; i < toIndex; i = i <= toIndex - chunkSize ? i + chunkSize : toIndex) {
            res.add(copyOfRange(a, i, i <= toIndex - chunkSize ? i + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is {@code null} or empty.
     *
     * @param <T>
     * @param a
     * @param chunkSize the desired size of each sub array (the last may be smaller).
     * @return
     */
    public static <T> List<T[]> split(final T[] a, final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        if (isEmpty(a)) {
            return new ArrayList<>();
        }

        final int len = a.length;
        final List<T[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int fromIndex = 0, toIndex = a.length; fromIndex < toIndex; fromIndex += chunkSize) {
            res.add(copyOfRange(a, fromIndex, fromIndex <= toIndex - chunkSize ? fromIndex + chunkSize : toIndex));
        }

        return res;
    }

    /**
     * Returns consecutive sub arrays of an array, each of the same chunkSize (the final list may be smaller),
     * or an empty List if the specified array is {@code null} or empty.
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

        if (isEmpty(a) || fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final int len = toIndex - fromIndex;
        final List<T[]> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int i = fromIndex; i < toIndex; i = i <= toIndex - chunkSize ? i + chunkSize : toIndex) {
            res.add(copyOfRange(a, i, i <= toIndex - chunkSize ? i + chunkSize : toIndex));
        }

        return res;
    }

    /**
     *
     * @param <T>
     * @param c
     * @param chunkSize
     * @return
     */
    public static <T> List<List<T>> split(final Collection<? extends T> c, final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        if (isEmpty(c)) {
            return new ArrayList<>();
        }

        return split(c, 0, c.size(), chunkSize);
    }

    /**
     * Returns consecutive sub lists of a collection, each of the same chunkSize (the final list may be smaller).
     * or an empty List if the specified collection is {@code null} or empty. The order of elements in the original collection is kept
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

        if (isEmpty(c) || fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final int len = toIndex - fromIndex;
        final List<List<T>> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        if (c instanceof List) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i = i <= toIndex - chunkSize ? i + chunkSize : toIndex) {
                res.add(list.subList(i, i <= toIndex - chunkSize ? i + chunkSize : toIndex));
            }
        } else {
            final Iterator<? extends T> iter = c.iterator();

            for (int i = 0; i < toIndex;) {
                if (i < fromIndex) {
                    iter.next();
                    i++;
                    continue;
                }

                final List<T> subList = new ArrayList<>(min(chunkSize, toIndex - i));

                for (int j = i <= toIndex - chunkSize ? i + chunkSize : toIndex; i < j; i++) {
                    subList.add(iter.next());
                }

                res.add(subList);
            }
        }

        return res;
    }

    /**
     * Returns consecutive sub lists of a collection, each of the same chunkSize (the final list may be smaller).
     * or an empty List if the specified collection is {@code null} or empty. The order of elements in the original collection is kept
     *
     * @param <T>
     * @param c
     * @param chunkSize the desired size of each sub list (the last may be smaller).
     * @return
     */
    public static <T> List<List<T>> split(final Iterable<? extends T> c, final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        if (c == null) {
            return new ArrayList<>();
        } else if (c instanceof Collection) {
            final Collection<T> coll = (Collection<T>) c;

            return split(coll, 0, coll.size(), chunkSize);
        } else {
            return toList(split(c.iterator(), chunkSize));
        }
    }

    /**
     *
     *
     * @param <T>
     * @param iter
     * @param chunkSize
     * @return
     */
    public static <T> ObjIterator<List<T>> split(final Iterator<? extends T> iter, final int chunkSize) {
        checkArgument(chunkSize > 0, "'chunkSize' must be greater than 0, can't be: %s", chunkSize);

        if (iter == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private final Iterator<? extends T> iterator = iter;

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public List<T> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final List<T> next = new ArrayList<>(chunkSize);

                for (int i = 0; i < chunkSize && iterator.hasNext(); i++) {
                    next.add(iterator.next());
                }

                return next;
            }
        };
    }

    /**
     * Returns consecutive substring of the specified string, each of the same length (the final list may be smaller),
     * or an empty array if the specified string is {@code null} or empty.
     *
     * @param str
     * @param chunkSize the desired size of each sub String (the last may be smaller).
     * @return
     */
    public static List<String> split(final CharSequence str, final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        if (Strings.isEmpty(str)) {
            return new ArrayList<>();
        }

        return split(str, 0, str.length(), chunkSize);
    }

    /**
     * Returns consecutive substring of the specified string, each of the same length (the final list may be smaller),
     * or an empty array if the specified string is {@code null} or empty.
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

        if (Strings.isEmpty(str)) {
            return new ArrayList<>();
        }

        final int len = toIndex - fromIndex;
        final List<String> res = new ArrayList<>(len % chunkSize == 0 ? len / chunkSize : (len / chunkSize) + 1);

        for (int i = fromIndex; i < toIndex; i = i <= toIndex - chunkSize ? i + chunkSize : toIndex) {
            res.add(str.subSequence(i, i <= toIndex - chunkSize ? i + chunkSize : toIndex).toString());
        }

        return res;
    }

    /**
     * <pre>
     * <code>
     * final int[] a = Array.rangeClosed(1, 7);
     * splitByCount(5, 7, true, (fromIndex, toIndex) ->  copyOfRange(a, fromIndex, toIndex)).forEach(Fn.println()); // [[1], [2], [3], [4, 5], [6, 7]]
     * splitByCount(5, 7, false, (fromIndex, toIndex) ->  copyOfRange(a, fromIndex, toIndex)).forEach(Fn.println()); // [[1, 2], [3, 4], [5], [6], [7]]
     * </code>
     * </pre>
     *
     * @param <T>
     * @param maxCount max count of chunk want to split {@code totalSize} into.
     * @param totalSize
     * @param sizeSmallerFirst
     * @param func
     * @return the stream
     */
    public static <T> Stream<T> splitByCount(final int maxCount, final int totalSize, final boolean sizeSmallerFirst, final IntBiFunction<? extends T> func) {
        if (sizeSmallerFirst) {
            return splitByCountSmallerFirst(maxCount, totalSize, func);
        } else {
            return splitByCountSmallerLast(maxCount, totalSize, func);
        }
    }

    /**
     * <pre>
     * <code>
     * final int[] a = Array.rangeClosed(1, 7);
     * splitByCountSmallerFirst(5, 7, (fromIndex, toIndex) ->  copyOfRange(a, fromIndex, toIndex)).forEach(Fn.println()); // [[1], [2], [3], [4, 5], [6, 7]]
     * splitByCountSmallerLast(5, 7, (fromIndex, toIndex) ->  copyOfRange(a, fromIndex, toIndex)).forEach(Fn.println()); // [[1, 2], [3, 4], [5], [6], [7]]
     * </code>
     * </pre>
     *
     * @param <T>
     * @param maxCount
     * @param totalSize
     * @param func
     * @return the stream
     */
    static <T> Stream<T> splitByCountSmallerFirst(final int maxCount, final int totalSize, final IntBiFunction<? extends T> func) {
        checkArgPositive(maxCount, "maxCount");
        checkArgNotNegative(totalSize, "totalSize");
        checkArgNotNull(func, "func");

        if (totalSize == 0) {
            return Stream.empty();
        }

        final Iterator<T> iter = new ObjIteratorEx<>() {
            private final int smallerSize = Math.max(totalSize / maxCount, 1);
            private final int biggerSize = totalSize % maxCount == 0 ? totalSize / maxCount : totalSize / maxCount + 1;
            private int count = totalSize >= maxCount ? maxCount : totalSize;
            private int biggerCount = totalSize % maxCount;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < totalSize;
            }

            @Override
            public T next() {
                if (cursor >= totalSize) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return func.apply(cursor, cursor = (count-- > biggerCount ? cursor + smallerSize : cursor + biggerSize));
            }

            @Override
            public void advance(long n) {
                checkArgNotNegative(n, "n");

                if (n > 0) {
                    while (n-- > 0 && cursor < totalSize) {
                        cursor = count-- > biggerCount ? cursor + smallerSize : cursor + biggerSize;
                    }
                }
            }

            @Override
            public long count() {
                return count;
            }
        };

        return Stream.of(iter);
    }

    /**
     * <pre>
     * <code>
     * final int[] a = Array.rangeClosed(1, 7);
     * splitByCountSmallerFirst(5, 7, (fromIndex, toIndex) ->  copyOfRange(a, fromIndex, toIndex)).forEach(Fn.println()); // [[1], [2], [3], [4, 5], [6, 7]]
     * splitByCountSmallerLast(5, 7, (fromIndex, toIndex) ->  copyOfRange(a, fromIndex, toIndex)).forEach(Fn.println()); // [[1, 2], [3, 4], [5], [6], [7]]
     * </code>
     * </pre>
     *
     * @param <T>
     * @param maxCount
     * @param totalSize
     * @param func
     * @return the stream
     */
    static <T> Stream<T> splitByCountSmallerLast(final int maxCount, final int totalSize, final IntBiFunction<? extends T> func) {
        checkArgPositive(maxCount, "maxCount");
        checkArgNotNegative(totalSize, "totalSize");
        checkArgNotNull(func, "func");

        if (totalSize == 0) {
            return Stream.empty();
        }

        final Iterator<T> iter = new ObjIteratorEx<>() {
            private final int smallerSize = Math.max(totalSize / maxCount, 1);
            private final int biggerSize = totalSize % maxCount == 0 ? totalSize / maxCount : totalSize / maxCount + 1;
            private int count = totalSize >= maxCount ? maxCount : totalSize;
            private int smallerCount = count - totalSize % maxCount;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < totalSize;
            }

            @Override
            public T next() {
                if (cursor >= totalSize) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return func.apply(cursor, cursor = (count-- > smallerCount ? cursor + biggerSize : cursor + smallerSize));
            }

            @Override
            public void advance(long n) {
                checkArgNotNegative(n, "n");

                if (n > 0) {
                    while (n-- > 0 && cursor < totalSize) {
                        cursor = count-- > smallerCount ? cursor + biggerSize : cursor + smallerSize;
                    }
                }
            }

            @Override
            public long count() {
                return count;
            }

        };

        return Stream.of(iter);
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean[] concat(final boolean[] a, final boolean[] b) {
        if (isEmpty(a)) {
            return isEmpty(b) ? EMPTY_BOOLEAN_ARRAY : b.clone();
        } else if (isEmpty(b)) {
            return isEmpty(a) ? EMPTY_BOOLEAN_ARRAY : a.clone();
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
        if (isEmpty(aa)) {
            return EMPTY_BOOLEAN_ARRAY;
        } else if (aa.length == 1) {
            return isEmpty(aa[0]) ? EMPTY_BOOLEAN_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (boolean[] a : aa) {
            if (isEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final boolean[] c = new boolean[len];
        int fromIndex = 0;

        for (boolean[] a : aa) {
            if (isEmpty(a)) {
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
        if (isEmpty(a)) {
            return isEmpty(b) ? EMPTY_CHAR_ARRAY : b.clone();
        } else if (isEmpty(b)) {
            return isEmpty(a) ? EMPTY_CHAR_ARRAY : a.clone();
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
        if (isEmpty(aa)) {
            return EMPTY_CHAR_ARRAY;
        } else if (aa.length == 1) {
            return isEmpty(aa[0]) ? EMPTY_CHAR_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (char[] a : aa) {
            if (isEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final char[] c = new char[len];
        int fromIndex = 0;

        for (char[] a : aa) {
            if (isEmpty(a)) {
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
        if (isEmpty(a)) {
            return isEmpty(b) ? EMPTY_BYTE_ARRAY : b.clone();
        } else if (isEmpty(b)) {
            return isEmpty(a) ? EMPTY_BYTE_ARRAY : a.clone();
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
        if (isEmpty(aa)) {
            return EMPTY_BYTE_ARRAY;
        } else if (aa.length == 1) {
            return isEmpty(aa[0]) ? EMPTY_BYTE_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (byte[] a : aa) {
            if (isEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final byte[] c = new byte[len];
        int fromIndex = 0;

        for (byte[] a : aa) {
            if (isEmpty(a)) {
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
        if (isEmpty(a)) {
            return isEmpty(b) ? EMPTY_SHORT_ARRAY : b.clone();
        } else if (isEmpty(b)) {
            return isEmpty(a) ? EMPTY_SHORT_ARRAY : a.clone();
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
        if (isEmpty(aa)) {
            return EMPTY_SHORT_ARRAY;
        } else if (aa.length == 1) {
            return isEmpty(aa[0]) ? EMPTY_SHORT_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (short[] a : aa) {
            if (isEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final short[] c = new short[len];
        int fromIndex = 0;

        for (short[] a : aa) {
            if (isEmpty(a)) {
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
        if (isEmpty(a)) {
            return isEmpty(b) ? EMPTY_INT_ARRAY : b.clone();
        } else if (isEmpty(b)) {
            return isEmpty(a) ? EMPTY_INT_ARRAY : a.clone();
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
        if (isEmpty(aa)) {
            return EMPTY_INT_ARRAY;
        } else if (aa.length == 1) {
            return isEmpty(aa[0]) ? EMPTY_INT_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (int[] a : aa) {
            if (isEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final int[] c = new int[len];
        int fromIndex = 0;

        for (int[] a : aa) {
            if (isEmpty(a)) {
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
        if (isEmpty(a)) {
            return isEmpty(b) ? EMPTY_LONG_ARRAY : b.clone();
        } else if (isEmpty(b)) {
            return isEmpty(a) ? EMPTY_LONG_ARRAY : a.clone();
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
        if (isEmpty(aa)) {
            return EMPTY_LONG_ARRAY;
        } else if (aa.length == 1) {
            return isEmpty(aa[0]) ? EMPTY_LONG_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (long[] a : aa) {
            if (isEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final long[] c = new long[len];
        int fromIndex = 0;

        for (long[] a : aa) {
            if (isEmpty(a)) {
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
        if (isEmpty(a)) {
            return isEmpty(b) ? EMPTY_FLOAT_ARRAY : b.clone();
        } else if (isEmpty(b)) {
            return isEmpty(a) ? EMPTY_FLOAT_ARRAY : a.clone();
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
        if (isEmpty(aa)) {
            return EMPTY_FLOAT_ARRAY;
        } else if (aa.length == 1) {
            return isEmpty(aa[0]) ? EMPTY_FLOAT_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (float[] a : aa) {
            if (isEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final float[] c = new float[len];
        int fromIndex = 0;

        for (float[] a : aa) {
            if (isEmpty(a)) {
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
        if (isEmpty(a)) {
            return isEmpty(b) ? EMPTY_DOUBLE_ARRAY : b.clone();
        } else if (isEmpty(b)) {
            return isEmpty(a) ? EMPTY_DOUBLE_ARRAY : a.clone();
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
        if (isEmpty(aa)) {
            return EMPTY_DOUBLE_ARRAY;
        } else if (aa.length == 1) {
            return isEmpty(aa[0]) ? EMPTY_DOUBLE_ARRAY : aa[0].clone();
        }

        int len = 0;

        for (double[] a : aa) {
            if (isEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final double[] c = new double[len];
        int fromIndex = 0;

        for (double[] a : aa) {
            if (isEmpty(a)) {
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
        if (isEmpty(a)) {
            return isEmpty(b) ? a : b.clone();
        } else if (isEmpty(b)) {
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
     * @return {@code null} if the specified array {@code aa} is {@code null}.
     */
    @MayReturnNull
    @SafeVarargs
    public static <T> T[] concat(final T[]... aa) {
        // checkArgNotNull(aa, "aa");

        if (aa == null) {
            return null; // NOSONAR
        } else if (aa.length == 0) {
            return newArray(aa.getClass().getComponentType().getComponentType(), 0);
        } else if (aa.length == 1) {
            return aa[0] == null ? newArray(aa.getClass().getComponentType().getComponentType(), 0) : aa[0].clone();
        }

        int len = 0;

        for (T[] a : aa) {
            if (isEmpty(a)) {
                continue;
            }

            len += a.length;
        }

        final T[] c = newArray(aa.getClass().getComponentType().getComponentType(), len);
        int fromIndex = 0;

        for (T[] a : aa) {
            if (isEmpty(a)) {
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
    public static <T> List<T> concat(final Iterable<? extends T> a, final Iterable<? extends T> b) {
        return concat(Arrays.asList(a, b));
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> List<T> concat(final Iterable<? extends T>... a) {
        if (isEmpty(a)) {
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
    public static <T> List<T> concat(final Collection<? extends Iterable<? extends T>> c) {
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
    public static <T, C extends Collection<T>> C concat(final Collection<? extends Iterable<? extends T>> c, final IntFunction<? extends C> supplier) {
        if (isEmpty(c)) {
            return supplier.apply(0);
        }

        long count = 0;

        for (Iterable<? extends T> e : c) {
            count += getSizeOrDefault(e, 0);
        }

        final C result = supplier.apply(Numbers.toIntExact(count));

        for (Iterable<? extends T> e : c) {
            if (e != null) {
                if (e instanceof Collection) {
                    result.addAll((Collection<T>) e);
                } else {
                    for (T t : e) {
                        result.add(t);
                    }
                }
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
     * @see Iterators#concat(Iterator...)
     */
    public static <T> ObjIterator<T> concat(final Iterator<? extends T> a, final Iterator<? extends T> b) {
        return Iterators.concat(a, b);
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     * @see Iterators#concat(Iterator...)
     */
    @SafeVarargs
    public static <T> ObjIterator<T> concat(final Iterator<? extends T>... a) {
        return Iterators.concat(a);
    }

    /**
     *
     * @param a
     * @return an empty {@code boolean[]} if {@code a} is null.
     */
    public static boolean[] flatten(final boolean[][] a) {
        if (isEmpty(a)) {
            return EMPTY_BOOLEAN_ARRAY;
        }

        int count = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            count += (a[i] == null ? 0 : a[i].length);
        }

        final boolean[] ret = new boolean[count];
        int from = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            if (notEmpty(a[i])) {
                copy(a[i], 0, ret, from, a[i].length);
                from += a[i].length;
            }
        }

        return ret;
    }

    /**
     *
     * @param a
     * @return an empty {@code char[]} if {@code a} is null.
     */
    public static char[] flatten(final char[][] a) {
        if (isEmpty(a)) {
            return EMPTY_CHAR_ARRAY;
        }

        int count = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            count += (a[i] == null ? 0 : a[i].length);
        }

        final char[] ret = new char[count];
        int from = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            if (notEmpty(a[i])) {
                copy(a[i], 0, ret, from, a[i].length);
                from += a[i].length;
            }
        }

        return ret;
    }

    /**
     *
     * @param a
     * @return an empty {@code byte[]} if {@code a} is null.
     */
    public static byte[] flatten(final byte[][] a) {
        if (isEmpty(a)) {
            return EMPTY_BYTE_ARRAY;
        }

        int count = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            count += (a[i] == null ? 0 : a[i].length);
        }

        final byte[] ret = new byte[count];
        int from = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            if (notEmpty(a[i])) {
                copy(a[i], 0, ret, from, a[i].length);
                from += a[i].length;
            }
        }

        return ret;
    }

    /**
     *
     * @param a
     * @return an empty {@code short[]} if {@code a} is null.
     */
    public static short[] flatten(final short[][] a) {
        if (isEmpty(a)) {
            return EMPTY_SHORT_ARRAY;
        }

        int count = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            count += (a[i] == null ? 0 : a[i].length);
        }

        final short[] ret = new short[count];
        int from = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            if (notEmpty(a[i])) {
                copy(a[i], 0, ret, from, a[i].length);
                from += a[i].length;
            }
        }

        return ret;
    }

    /**
     *
     * @param a
     * @return an empty {@code int[]} if {@code a} is null.
     */
    public static int[] flatten(final int[][] a) {
        if (isEmpty(a)) {
            return EMPTY_INT_ARRAY;
        }

        int count = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            count += (a[i] == null ? 0 : a[i].length);
        }

        final int[] ret = new int[count];
        int from = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            if (notEmpty(a[i])) {
                copy(a[i], 0, ret, from, a[i].length);
                from += a[i].length;
            }
        }

        return ret;
    }

    /**
     *
     * @param a
     * @return an empty {@code long[]} if {@code a} is null.
     */
    public static long[] flatten(final long[][] a) {
        if (isEmpty(a)) {
            return EMPTY_LONG_ARRAY;
        }

        int count = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            count += (a[i] == null ? 0 : a[i].length);
        }

        final long[] ret = new long[count];
        int from = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            if (notEmpty(a[i])) {
                copy(a[i], 0, ret, from, a[i].length);
                from += a[i].length;
            }
        }

        return ret;
    }

    /**
     *
     * @param a
     * @return an empty {@code float[]} if {@code a} is null.
     */
    public static float[] flatten(final float[][] a) {
        if (isEmpty(a)) {
            return EMPTY_FLOAT_ARRAY;
        }

        int count = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            count += (a[i] == null ? 0 : a[i].length);
        }

        final float[] ret = new float[count];
        int from = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            if (notEmpty(a[i])) {
                copy(a[i], 0, ret, from, a[i].length);
                from += a[i].length;
            }
        }

        return ret;
    }

    /**
     *
     * @param a
     * @return an empty {@code double[]} if {@code a} is null.
     */
    public static double[] flatten(final double[][] a) {
        if (isEmpty(a)) {
            return EMPTY_DOUBLE_ARRAY;
        }

        int count = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            count += (a[i] == null ? 0 : a[i].length);
        }

        final double[] ret = new double[count];
        int from = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            if (notEmpty(a[i])) {
                copy(a[i], 0, ret, from, a[i].length);
                from += a[i].length;
            }
        }

        return ret;
    }

    /**
     *
     * @param <T>
     * @param a
     * @return {@code null} if {@code a} is null.
     */
    @MayReturnNull
    public static <T> T[] flatten(final T[][] a) {
        if (a == null) {
            return null; // NOSONAR
        }

        return flatten(a, (Class<T>) a.getClass().getComponentType().getComponentType());
    }

    /**
     *
     *
     * @param <T>
     * @param a
     * @param componentType
     * @return an empty {@code T[]} if {@code a} is null.
     */
    public static <T> T[] flatten(final T[][] a, final Class<T> componentType) {
        if (isEmpty(a)) {
            return newArray(componentType, 0);
        }

        int count = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            count += (a[i] == null ? 0 : a[i].length);
        }

        final T[] ret = newArray(componentType, count);
        int from = 0;

        for (int i = 0, n = a.length; i < n; i++) {
            if (notEmpty(a[i])) {
                copy(a[i], 0, ret, from, a[i].length);
                from += a[i].length;
            }
        }

        return ret;
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> List<T> flatten(final Collection<? extends Iterable<? extends T>> c) {
        return flatten(c, IntFunctions.ofList());
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
    @SuppressWarnings("rawtypes")
    public static <T, C extends Collection<T>> C flatten(final Collection<? extends Iterable<? extends T>> c, IntFunction<? extends C> supplier) {
        if (isEmpty(c)) {
            return supplier.apply(0);
        }

        int count = 0;

        for (Iterable<? extends T> e : c) {
            count += e instanceof Collection ? ((Collection) e).size() : 3; //NOSONAR
        }

        final C ret = supplier.apply(count);

        for (Iterable<? extends T> e : c) {
            if (e == null) {
                continue; //NOSONAR
            } else if (e instanceof Collection) {
                ret.addAll((Collection) e);
            } else {
                for (T ee : e) {
                    ret.add(ee);
                }
            }
        }

        return ret;
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> List<T> flatten(final Iterable<? extends Iterable<? extends T>> c) {
        return flatten(c, Suppliers.ofList());
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
    @SuppressWarnings("rawtypes")
    public static <T, C extends Collection<T>> C flatten(final Iterable<? extends Iterable<? extends T>> c, Supplier<? extends C> supplier) {
        if (isEmpty(c)) {
            return supplier.get();
        }

        final C ret = supplier.get();

        for (Iterable<? extends T> e : c) {
            if (e == null) {
                continue; //NOSONAR
            } else if (e instanceof Collection) {
                ret.addAll((Collection) e);
            } else {
                for (T ee : e) {
                    ret.add(ee);
                }
            }
        }

        return ret;
    }

    /**
     *
     * @param <T>
     * @param iters
     * @return
     */
    public static <T> Iterator<T> flatten(final Iterator<? extends Iterator<? extends T>> iters) {
        if (iters == null) {
            return ObjIterator.empty();
        }

        return new ObjIterator<>() {
            private Iterator<? extends T> cur = null;

            @Override
            public boolean hasNext() {
                if (cur == null || !cur.hasNext()) {
                    while (iters.hasNext()) {
                        cur = iters.next();

                        if (cur != null && cur.hasNext()) {
                            break;
                        }
                    }
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }
        };
    }

    /**
     * Flatten each element if it's a {@code Iterable}, otherwise just add it to result Collection.
     *
     * @param iter
     * @return
     */
    @Beta
    public static List<?> flattenEachElement(final Iterable<?> iter) { //NOSONAR
        return flattenEachElement(iter, IntFunctions.ofList());
    }

    /**
     * Flatten each element if it's a {@code Iterable}, otherwise just add it to result Collection.
     *
     *
     * @param <T>
     * @param <C>
     * @param iter
     * @param supplier
     * @return
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T, C extends Collection<T>> C flattenEachElement(final Iterable<?> iter, final IntFunction<? extends C> supplier) {
        if (iter == null) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(iter instanceof Collection ? ((Collection) iter).size() : 0);

        flattenEachElement((Iterable<Object>) iter, (Collection<Object>) result);

        return result;
    }

    private static void flattenEachElement(final Iterable<Object> c, final Collection<Object> output) {
        for (Object next : c) {
            if (next instanceof Iterable) {
                flattenEachElement((Iterable<Object>) next, output);
            } else {
                output.add(next);
            }
        }
    }

    /**
     *
     * @param a
     * @param b
     * @return
     * @see IntList#intersection(IntList)
     */
    public static boolean[] intersection(final boolean[] a, final boolean[] b) {
        if (isEmpty(a) || isEmpty(b)) {
            return isEmpty(a) ? a : EMPTY_BOOLEAN_ARRAY;
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
        if (isEmpty(a) || isEmpty(b)) {
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
        if (isEmpty(a) || isEmpty(b)) {
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
        if (isEmpty(a) || isEmpty(b)) {
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
        if (isEmpty(a) || isEmpty(b)) {
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
        if (isEmpty(a) || isEmpty(b)) {
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
        if (isEmpty(a) || isEmpty(b)) {
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
        if (isEmpty(a) || isEmpty(b)) {
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
        if (isEmpty(a) || isEmpty(b)) {
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
     * @see #commonSet(Collection, Collection)
     * @see Collection#retainAll(Collection)
     */
    public static <T> List<T> intersection(final Collection<? extends T> a, final Collection<?> b) {
        if (isEmpty(a) || isEmpty(b)) {
            return new ArrayList<>();
        }

        final Multiset<Object> bOccurrences = Multiset.create(b);

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
     * @see #commonSet(Collection)
     * @see Collection#retainAll(Collection)
     */
    public static <T> List<T> intersection(final Collection<? extends Collection<? extends T>> c) {
        if (isEmpty(c)) {
            return new ArrayList<>();
        } else if (c.size() == 1) {
            return newArrayList(c.iterator().next());
        }

        for (Collection<? extends T> e : c) {
            if (isEmpty(e)) {
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
     * Returns the elements from {@code a}, but exclude the elements in {@code b} by occurrences.
     *
     * @param a
     * @param b
     * @return
     * @see IntList#difference(IntList)
     */
    public static boolean[] difference(final boolean[] a, final boolean[] b) {
        if (isEmpty(a)) {
            return EMPTY_BOOLEAN_ARRAY;
        } else if (isEmpty(b)) {
            return a.clone();
        }

        return BooleanList.of(a).difference(BooleanList.of(b)).trimToSize().array();
    }

    /**
     * Returns the elements from {@code a}, but exclude the elements in {@code b} by occurrences.
     *
     * @param a
     * @param b
     * @return
     * @see IntList#difference(IntList)
     */
    public static char[] difference(final char[] a, final char[] b) {
        if (isEmpty(a)) {
            return EMPTY_CHAR_ARRAY;
        } else if (isEmpty(b)) {
            return a.clone();
        }

        return CharList.of(a).difference(CharList.of(b)).trimToSize().array();
    }

    /**
     * Returns the elements from {@code a}, but exclude the elements in {@code b} by occurrences.
     *
     * @param a
     * @param b
     * @return
     * @see IntList#difference(IntList)
     */
    public static byte[] difference(final byte[] a, final byte[] b) {
        if (isEmpty(a)) {
            return EMPTY_BYTE_ARRAY;
        } else if (isEmpty(b)) {
            return a.clone();
        }

        return ByteList.of(a).difference(ByteList.of(b)).trimToSize().array();
    }

    /**
     * Returns the elements from {@code a}, but exclude the elements in {@code b} by occurrences.
     *
     * @param a
     * @param b
     * @return
     * @see IntList#difference(IntList)
     */
    public static short[] difference(final short[] a, final short[] b) {
        if (isEmpty(a)) {
            return EMPTY_SHORT_ARRAY;
        } else if (isEmpty(b)) {
            return a.clone();
        }

        return ShortList.of(a).difference(ShortList.of(b)).trimToSize().array();
    }

    /**
     * Returns the elements from {@code a}, but exclude the elements in {@code b} by occurrences.
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
        if (isEmpty(a)) {
            return EMPTY_INT_ARRAY;
        } else if (isEmpty(b)) {
            return a.clone();
        }

        return IntList.of(a).difference(IntList.of(b)).trimToSize().array();
    }

    /**
     * Returns the elements from {@code a}, but exclude the elements in {@code b} by occurrences.
     *
     * @param a
     * @param b
     * @return
     * @see IntList#difference(IntList)
     */
    public static long[] difference(final long[] a, final long[] b) {
        if (isEmpty(a)) {
            return EMPTY_LONG_ARRAY;
        } else if (isEmpty(b)) {
            return a.clone();
        }

        return LongList.of(a).difference(LongList.of(b)).trimToSize().array();
    }

    /**
     * Returns the elements from {@code a}, but exclude the elements in {@code b} by occurrences.
     *
     * @param a
     * @param b
     * @return
     * @see IntList#difference(IntList)
     */
    public static float[] difference(final float[] a, final float[] b) {
        if (isEmpty(a)) {
            return EMPTY_FLOAT_ARRAY;
        } else if (isEmpty(b)) {
            return a.clone();
        }

        return FloatList.of(a).difference(FloatList.of(b)).trimToSize().array();
    }

    /**
     * Returns the elements from {@code a}, but exclude the elements in {@code b} by occurrences.
     *
     * @param a
     * @param b
     * @return
     * @see IntList#difference(IntList)
     */
    public static double[] difference(final double[] a, final double[] b) {
        if (isEmpty(a)) {
            return EMPTY_DOUBLE_ARRAY;
        } else if (isEmpty(b)) {
            return a.clone();
        }

        return DoubleList.of(a).difference(DoubleList.of(b)).trimToSize().array();
    }

    /**
     * Returns the elements from {@code a}, but exclude the elements in {@code b} by occurrences.
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     * @see IntList#difference(IntList)
     * @see #differentSet(Collection, Collection)
     * @see #excludeAll(Collection, Collection)
     * @see #excludeAllToSet(Collection, Collection)
     * @see #removeAll(Collection, Collection)
     * @see Difference#of(Collection, Collection)
     */
    public static <T> List<T> difference(final T[] a, final Object[] b) {
        if (isEmpty(a)) {
            return new ArrayList<>();
        } else if (isEmpty(b)) {
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
     * Returns the elements from {@code a}, but exclude the elements in {@code b} by occurrences.
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     * @see IntList#difference(IntList)
     * @see #differentSet(Collection, Collection)
     * @see #excludeAll(Collection, Collection)
     * @see #excludeAllToSet(Collection, Collection)
     * @see #removeAll(Collection, Collection)
     * @see Difference#of(Collection, Collection)
     */
    public static <T> List<T> difference(final Collection<? extends T> a, final Collection<?> b) {
        if (isEmpty(a)) {
            return new ArrayList<>();
        } else if (isEmpty(b)) {
            return new ArrayList<>(a);
        }

        final Multiset<Object> bOccurrences = Multiset.create(b);

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
        if (isEmpty(a)) {
            return isEmpty(b) ? EMPTY_BOOLEAN_ARRAY : b.clone();
        } else if (isEmpty(b)) {
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
        if (isEmpty(a)) {
            return isEmpty(b) ? EMPTY_CHAR_ARRAY : b.clone();
        } else if (isEmpty(b)) {
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
        if (isEmpty(a)) {
            return isEmpty(b) ? EMPTY_BYTE_ARRAY : b.clone();
        } else if (isEmpty(b)) {
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
        if (isEmpty(a)) {
            return isEmpty(b) ? EMPTY_SHORT_ARRAY : b.clone();
        } else if (isEmpty(b)) {
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
        if (isEmpty(a)) {
            return isEmpty(b) ? EMPTY_INT_ARRAY : b.clone();
        } else if (isEmpty(b)) {
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
        if (isEmpty(a)) {
            return isEmpty(b) ? EMPTY_LONG_ARRAY : b.clone();
        } else if (isEmpty(b)) {
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
        if (isEmpty(a)) {
            return isEmpty(b) ? EMPTY_FLOAT_ARRAY : b.clone();
        } else if (isEmpty(b)) {
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
        if (isEmpty(a)) {
            return isEmpty(b) ? EMPTY_DOUBLE_ARRAY : b.clone();
        } else if (isEmpty(b)) {
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
     * @see #symmetricDifferentSet(Collection, Collection)
     * @see #excludeAll(Collection, Collection)
     * @see #excludeAllToSet(Collection, Collection)
     * @see Difference#of(Collection, Collection)
     */
    public static <T> List<T> symmetricDifference(final T[] a, final T[] b) {
        if (isEmpty(a)) {
            return asList(b);
        } else if (isEmpty(b)) {
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
     * @see #symmetricDifferentSet(Collection, Collection)
     * @see #excludeAll(Collection, Collection)
     * @see #excludeAllToSet(Collection, Collection)
     * @see Difference#of(Collection, Collection)
     */
    public static <T> List<T> symmetricDifference(final Collection<? extends T> a, final Collection<? extends T> b) {
        if (isEmpty(a)) {
            return isEmpty(b) ? new ArrayList<>() : new ArrayList<>(b);
        } else if (isEmpty(b)) {
            return isEmpty(a) ? new ArrayList<>() : new ArrayList<>(a);
        }

        final Multiset<T> bOccurrences = Multiset.create(b);
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
     *
     * @param <T>
     * @param a
     * @param b
     * @return the sets the
     * @see #difference(Collection, Collection)
     * @see #excludeAll(Collection, Collection)
     * @see #excludeAllToSet(Collection, Collection)
     * @see #removeAll(Collection, Collection)
     * @see Difference#of(Collection, Collection)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Set<T> differentSet(final Collection<? extends T> a, final Collection<?> b) {
        if (isEmpty(a)) {
            return newHashSet();
        } else if (isEmpty(b)) {
            return newHashSet(a);
        }

        final Set<T> result = newHashSet(a);

        removeAll(result, (Collection) b);

        return result;
    }

    /**
     * Symmetric different set.
     *
     * @param <T>
     * @param a
     * @param b
     * @return the sets the
     * @see #symmetricDifference(Collection, Collection)
     * @see #excludeAll(Collection, Collection)
     * @see #excludeAllToSet(Collection, Collection)
     * @see Difference#of(Collection, Collection)
     */
    public static <T> Set<T> symmetricDifferentSet(final Collection<? extends T> a, final Collection<? extends T> b) {
        if (isEmpty(a)) {
            return isEmpty(b) ? newHashSet() : newHashSet(b);
        } else if (isEmpty(b)) {
            return isEmpty(a) ? newHashSet() : newHashSet(a);
        }

        final Set<T> commonSet = commonSet(a, b);
        final Set<T> result = newHashSet();

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
     *
     * @param <T>
     * @param a
     * @param b
     * @return the sets the
     * @see #intersection(Collection, Collection)
     * @see Collection#retainAll(Collection)
     */
    public static <T> Set<T> commonSet(final Collection<? extends T> a, final Collection<?> b) {
        if (isEmpty(a) || isEmpty(b)) {
            return newHashSet();
        }

        return commonSet(Array.asList(a, (Collection<? extends T>) b));
    }

    /**
     *
     * @param <T>
     * @param c
     * @return the sets the
     * @see #intersection(Collection)
     * @see Collection#retainAll(Collection)
     */
    public static <T> Set<T> commonSet(final Collection<? extends Collection<? extends T>> c) {
        if (isEmpty(c)) {
            return newHashSet();
        } else if (c.size() == 1) {
            return newHashSet(c.iterator().next());
        }

        Collection<? extends T> smallest = null;

        for (final Collection<? extends T> e : c) {
            if (isEmpty(e)) {
                return newHashSet();
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
            if (ec == smallest) { // NOSONAR
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

        final Set<T> result = newHashSet(map.size());

        for (Map.Entry<T, MutableInt> entry : map.entrySet()) {
            if (entry.getValue().intValue() == cnt) {
                result.add(entry.getKey());
            }
        }

        return result;
    }

    /**
     * Returns a new {@code List} with specified {@code objToExclude} excluded.
     * That's to say no more {@code objToExclude} will present in the returned {@code List}.
     *
     * @param <T>
     * @param c
     * @param objToExclude
     * @return a new {@code List}
     * @see #difference(Collection, Collection)
     * @see #removeAll(Collection, Collection)
     * @see Difference#of(Collection, Collection)
     */
    public static <T> List<T> exclude(final Collection<? extends T> c, final Object objToExclude) {
        if (isEmpty(c)) {
            return new ArrayList<>();
        }

        final List<T> result = new ArrayList<>(c.size() - 1);

        for (T e : c) {
            if (!equals(e, objToExclude)) {
                result.add(e);
            }
        }

        return result;
    }

    /**
     * Returns a new {@code Set} with specified {@code objToExclude} excluded.
     * That's to say no more {@code objToExclude} will present in the returned {@code Set}.
     *
     * @param <T>
     * @param c
     * @param objToExclude
     * @return a new {@code Set}
     * @see #difference(Collection, Collection)
     * @see #removeAll(Collection, Collection)
     * @see Difference#of(Collection, Collection)
     */
    public static <T> Set<T> excludeToSet(final Collection<? extends T> c, final Object objToExclude) {
        if (isEmpty(c)) {
            return new HashSet<>();
        }

        final Set<T> result = newHashSet(c);

        result.remove(objToExclude);

        return result;
    }

    /**
     * Returns a new {@code List} with specified {@code objsToExclude} excluded.
     * That's to say no more {@code objsToExclude} will present in the returned {@code List}.
     *
     * @param <T>
     * @param c
     * @param objsToExclude
     * @return a new {@code List}
     * @see #difference(Collection, Collection)
     * @see #removeAll(Collection, Collection)
     * @see Difference#of(Collection, Collection)
     */
    public static <T> List<T> excludeAll(final Collection<? extends T> c, final Collection<?> objsToExclude) {
        if (isEmpty(c)) {
            return new ArrayList<>();
        } else if (isEmpty(objsToExclude)) {
            return new ArrayList<>(c);
        } else if (objsToExclude.size() == 1) {
            return exclude(c, firstOrNullIfEmpty(objsToExclude));
        }

        final Set<Object> set = objsToExclude instanceof Set ? ((Set<Object>) objsToExclude) : new HashSet<>(objsToExclude);
        final List<T> result = new ArrayList<>(max(0, c.size() - set.size()));

        for (T e : c) {
            if (!set.contains(e)) {
                result.add(e);
            }
        }

        return result;
    }

    /**
     * Returns a new {@code Set} with specified {@code objsToExclude} excluded.
     * That's to say no more {@code objsToExclude} will present in the returned {@code Set}.
     *
     * @param <T>
     * @param c
     * @param objsToExclude
     * @return a new {@code Set}
     * @see #difference(Collection, Collection)
     * @see #removeAll(Collection, Collection)
     * @see Difference#of(Collection, Collection)
     */
    public static <T> Set<T> excludeAllToSet(final Collection<? extends T> c, final Collection<?> objsToExclude) {
        if (isEmpty(c)) {
            return new HashSet<>();
        } else if (isEmpty(objsToExclude)) {
            return new HashSet<>(c);
        } else if (objsToExclude.size() == 1) {
            return excludeToSet(c, firstOrNullIfEmpty(objsToExclude));
        }

        final Set<Object> set = objsToExclude instanceof Set ? ((Set<Object>) objsToExclude) : new HashSet<>(objsToExclude);
        final Set<T> result = newHashSet(max(0, c.size() - set.size()));

        for (T e : c) {
            if (!set.contains(e)) {
                result.add(e);
            }
        }

        return result;
    }

    /**
     * Returns {@code true} if <i>subColl</i> is a sub-collection of <i>coll</i>,
     * that is, if the cardinality of <i>e</i> in <i>subColl</i> is less than or
     * equal to the cardinality of <i>e</i> in <i>coll</i>, for each element <i>e</i>
     * in <i>subColl</i>.
     *
     * @param subColl the first (sub?) collection, must not be null
     * @param coll the second (super?) collection, must not be null
     * @return <code>true</code> if <i>subColl</i> is a sub-collection of <i>coll</i>
     * @see #isProperSubCollection
     * @see Collection#containsAll
     */
    public static boolean isSubCollection(final Collection<?> subColl, final Collection<?> coll) {
        checkArgNotNull(subColl, "a");
        checkArgNotNull(coll, "b");

        if (isEmpty(coll)) {
            return true;
        } else if (isEmpty(subColl)) {
            return false;
        }

        final Multiset<?> multisetA = Multiset.create(subColl);
        final Multiset<?> multisetB = Multiset.create(coll);

        for (final Object e : subColl) {
            if (multisetA.occurrencesOf(e) > multisetB.occurrencesOf(e)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns {@code true} if <i>subColl</i> is a <i>proper</i> sub-collection of <i>coll</i>,
     * that is, if the cardinality of <i>e</i> in <i>subColl</i> is less
     * than or equal to the cardinality of <i>e</i> in <i>coll</i>,
     * for each element <i>e</i> in <i>subColl</i>, and there is at least one
     * element <i>f</i> such that the cardinality of <i>f</i> in <i>coll</i>
     * is strictly greater than the cardinality of <i>f</i> in <i>subColl</i>.
     * <p>
     * The implementation assumes
     * </p>
     * <ul>
     *    <li><code>subColl.size()</code> and <code>coll.size()</code> represent the
     *    total cardinality of <i>a</i> and <i>b</i>, resp. </li>
     *    <li><code>subColl.size() &lt; Integer.MAXVALUE</code></li>
     * </ul>
     *
     * @param subColl the first (sub?) collection, must not be null
     * @param coll the second (super?) collection, must not be null
     * @return <code>true</code> if <i>subColl</i> is a <i>proper</i> sub-collection of <i>coll</i>
     * @see #isSubCollection
     * @see Collection#containsAll
     */
    public static boolean isProperSubCollection(final Collection<?> subColl, final Collection<?> coll) {
        checkArgNotNull(subColl, "a");
        checkArgNotNull(coll, "b");

        return subColl.size() < coll.size() && isSubCollection(subColl, coll);
    }

    /**
     * Returns {@code true} if the given {@link Collection}s contain
     * exactly the same elements with exactly the same cardinalities.
     * <p>
     * That is, if the cardinality of <i>e</i> in <i>a</i> is
     * equal to the cardinality of <i>e</i> in <i>b</i>,
     * for each element <i>e</i> in <i>a</i> or <i>b</i>.
     * </p>
     *
     * @param a the first collection
     * @param b the second collection
     * @return <code>true</code> if the collections contain the same elements with the same cardinalities.
     */
    public static boolean isEqualCollection(final Collection<?> a, final Collection<?> b) {
        if (a == null && b == null) {
            return true;
        } else if ((a == null && b != null) || (a != null && b == null)) { //NOSONAR
            return false;
        }

        final int sizeA = size(a);
        final int sizeB = size(b);

        if (sizeA != sizeB) {
            return false;
        }

        final Multiset<?> multisetA = Multiset.create(a);
        final Multiset<?> multisetB = Multiset.create(b);

        if (multisetA.size() != multisetB.size()) {
            return false;
        }

        for (final Object e : b) {
            if (multisetA.occurrencesOf(e) != multisetB.occurrencesOf(e)) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param a
     * @param oldVal
     * @param newVal
     * @return
     */
    public static int replaceAll(final boolean[] a, final boolean oldVal, final boolean newVal) {
        if (isEmpty(a)) {
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
        if (isEmpty(a)) {
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
        if (isEmpty(a)) {
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
        if (isEmpty(a)) {
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
        if (isEmpty(a)) {
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
        if (isEmpty(a)) {
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
        if (isEmpty(a)) {
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
        if (isEmpty(a)) {
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
        if (isEmpty(a)) {
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
        if (isEmpty(list)) {
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
     *
     *
     * @param <E>
     * @param a
     * @param operator
     * @throws E
     */
    public static <E extends Exception> void replaceAll(final boolean[] a, final Throwables.BooleanUnaryOperator<E> operator) throws E {
        if (isEmpty(a)) {
            return;
        }

        for (int i = 0, n = a.length; i < n; i++) {
            a[i] = operator.applyAsBoolean(a[i]);
        }
    }

    /**
     *
     *
     * @param <E>
     * @param a
     * @param operator
     * @throws E
     */
    public static <E extends Exception> void replaceAll(final char[] a, final Throwables.CharUnaryOperator<E> operator) throws E {
        if (isEmpty(a)) {
            return;
        }

        for (int i = 0, n = a.length; i < n; i++) {
            a[i] = operator.applyAsChar(a[i]);
        }
    }

    /**
     *
     *
     * @param <E>
     * @param a
     * @param operator
     * @throws E
     */
    public static <E extends Exception> void replaceAll(final byte[] a, final Throwables.ByteUnaryOperator<E> operator) throws E {
        if (isEmpty(a)) {
            return;
        }

        for (int i = 0, n = a.length; i < n; i++) {
            a[i] = operator.applyAsByte(a[i]);
        }
    }

    /**
     *
     *
     * @param <E>
     * @param a
     * @param operator
     * @throws E
     */
    public static <E extends Exception> void replaceAll(final short[] a, final Throwables.ShortUnaryOperator<E> operator) throws E {
        if (isEmpty(a)) {
            return;
        }

        for (int i = 0, n = a.length; i < n; i++) {
            a[i] = operator.applyAsShort(a[i]);
        }
    }

    /**
     *
     *
     * @param <E>
     * @param a
     * @param operator
     * @throws E
     */
    public static <E extends Exception> void replaceAll(final int[] a, final Throwables.IntUnaryOperator<E> operator) throws E {
        if (isEmpty(a)) {
            return;
        }

        for (int i = 0, n = a.length; i < n; i++) {
            a[i] = operator.applyAsInt(a[i]);
        }
    }

    /**
     *
     *
     * @param <E>
     * @param a
     * @param operator
     * @throws E
     */
    public static <E extends Exception> void replaceAll(final long[] a, final Throwables.LongUnaryOperator<E> operator) throws E {
        if (isEmpty(a)) {
            return;
        }

        for (int i = 0, n = a.length; i < n; i++) {
            a[i] = operator.applyAsLong(a[i]);
        }
    }

    /**
     *
     *
     * @param <E>
     * @param a
     * @param operator
     * @throws E
     */
    public static <E extends Exception> void replaceAll(final float[] a, final Throwables.FloatUnaryOperator<E> operator) throws E {
        if (isEmpty(a)) {
            return;
        }

        for (int i = 0, n = a.length; i < n; i++) {
            a[i] = operator.applyAsFloat(a[i]);
        }
    }

    /**
     *
     *
     * @param <E>
     * @param a
     * @param operator
     * @throws E
     */
    public static <E extends Exception> void replaceAll(final double[] a, final Throwables.DoubleUnaryOperator<E> operator) throws E {
        if (isEmpty(a)) {
            return;
        }

        for (int i = 0, n = a.length; i < n; i++) {
            a[i] = operator.applyAsDouble(a[i]);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param operator
     * @throws E
     */
    public static <T, E extends Exception> void replaceAll(final T[] a, final Throwables.UnaryOperator<T, E> operator) throws E {
        if (isEmpty(a)) {
            return;
        }

        for (int i = 0, n = a.length; i < n; i++) {
            a[i] = operator.apply(a[i]);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param list
     * @param operator
     * @throws E
     */
    public static <T, E extends Exception> void replaceAll(final List<T> list, final Throwables.UnaryOperator<T, E> operator) throws E {
        if (isEmpty(list)) {
            return;
        }

        final int size = list.size();

        if (size < REPLACEALL_THRESHOLD || list instanceof RandomAccess) {
            for (int i = 0; i < size; i++) {
                list.set(i, operator.apply(list.get(i)));
            }
        } else {
            final ListIterator<T> itr = list.listIterator();

            for (int i = 0; i < size; i++) {
                itr.set(operator.apply(itr.next()));
            }
        }
    }

    /**
     *
     *
     * @param <E>
     * @param a
     * @param predicate
     * @param newValue
     * @return
     * @throws E
     */
    public static <E extends Exception> int replaceIf(final boolean[] a, final Throwables.BooleanPredicate<E> predicate, final boolean newValue) throws E {
        if (isEmpty(a)) {
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

    /**
     *
     *
     * @param <E>
     * @param a
     * @param predicate
     * @param newValue
     * @return
     * @throws E
     */
    public static <E extends Exception> int replaceIf(final char[] a, final Throwables.CharPredicate<E> predicate, final char newValue) throws E {
        if (isEmpty(a)) {
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

    /**
     *
     *
     * @param <E>
     * @param a
     * @param predicate
     * @param newValue
     * @return
     * @throws E
     */
    public static <E extends Exception> int replaceIf(final byte[] a, final Throwables.BytePredicate<E> predicate, final byte newValue) throws E {
        if (isEmpty(a)) {
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

    /**
     *
     *
     * @param <E>
     * @param a
     * @param predicate
     * @param newValue
     * @return
     * @throws E
     */
    public static <E extends Exception> int replaceIf(final short[] a, final Throwables.ShortPredicate<E> predicate, final short newValue) throws E {
        if (isEmpty(a)) {
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

    /**
     *
     *
     * @param <E>
     * @param a
     * @param predicate
     * @param newValue
     * @return
     * @throws E
     */
    public static <E extends Exception> int replaceIf(final int[] a, final Throwables.IntPredicate<E> predicate, final int newValue) throws E {
        if (isEmpty(a)) {
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

    /**
     *
     *
     * @param <E>
     * @param a
     * @param predicate
     * @param newValue
     * @return
     * @throws E
     */
    public static <E extends Exception> int replaceIf(final long[] a, final Throwables.LongPredicate<E> predicate, final long newValue) throws E {
        if (isEmpty(a)) {
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

    /**
     *
     *
     * @param <E>
     * @param a
     * @param predicate
     * @param newValue
     * @return
     * @throws E
     */
    public static <E extends Exception> int replaceIf(final float[] a, final Throwables.FloatPredicate<E> predicate, final float newValue) throws E {
        if (isEmpty(a)) {
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

    /**
     *
     *
     * @param <E>
     * @param a
     * @param predicate
     * @param newValue
     * @return
     * @throws E
     */
    public static <E extends Exception> int replaceIf(final double[] a, final Throwables.DoublePredicate<E> predicate, final double newValue) throws E {
        if (isEmpty(a)) {
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

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param predicate
     * @param newValue
     * @return
     * @throws E
     */
    public static <T, E extends Exception> int replaceIf(final T[] a, final Throwables.Predicate<? super T, E> predicate, final T newValue) throws E {
        if (isEmpty(a)) {
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

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param predicate
     * @param newValue
     * @return
     * @throws E
     */
    public static <T, E extends Exception> int replaceIf(final List<T> c, final Throwables.Predicate<? super T, E> predicate, final T newValue) throws E {
        if (isEmpty(c)) {
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
     * A fake method defined to remind user to use {@code replaceAll} when {@code update/updateAll/updateIf} is searched.
     *
     * @throws UnsupportedOperationException
     * @deprecated use {@code replaceAll}
     * @see #replaceAll(Object[], com.landawn.abacus.util.Throwables.UnaryOperator)
     * @see #replaceAll(Object[], Object, Object)
     */
    @Deprecated
    public static void updateAllUsingReplaceAllInstead() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Fake method. Please use 'replaceAll'");
    }

    /**
     * A fake method defined to remind user to use {@code replaceIf} when {@code update/updateAll/updateIf} is searched.
     *
     *
     * @throws UnsupportedOperationException
     * @deprecated use {@code replaceIf}
     * @see #replaceIf(Object[], com.landawn.abacus.util.Throwables.Predicate, Object)
     */
    @Deprecated
    public static void updateIfUsingReplaceIfInstead() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Fake method. Please use 'replaceIf'");
    }

    /**
     * <p>
     * Copies the given array and adds the given elementToAdd at the end of the new
     * array.
     *
     * @param a
     * @param elementToAdd
     * @return A new array containing the existing elementToAdds plus the new elementToAdd
     */
    public static boolean[] add(final boolean[] a, final boolean elementToAdd) {
        if (isEmpty(a)) {
            return Array.of(elementToAdd);
        }

        final boolean[] newArray = new boolean[a.length + 1];

        copy(a, 0, newArray, 0, a.length);
        newArray[a.length] = elementToAdd;

        return newArray;
    }

    /**
     * <p>
     * Copies the given array and adds the given elementToAdd at the end of the new
     * array.
     *
     * @param a
     * @param elementToAdd
     * @return A new array containing the existing elementToAdds plus the new elementToAdd
     */
    public static char[] add(final char[] a, final char elementToAdd) {
        if (isEmpty(a)) {
            return Array.of(elementToAdd);
        }

        final char[] newArray = new char[a.length + 1];

        copy(a, 0, newArray, 0, a.length);
        newArray[a.length] = elementToAdd;

        return newArray;
    }

    /**
     * <p>
     * Copies the given array and adds the given elementToAdd at the end of the new
     * array.
     *
     * @param a
     * @param elementToAdd
     * @return A new array containing the existing elementToAdds plus the new elementToAdd
     */
    public static byte[] add(final byte[] a, final byte elementToAdd) {
        if (isEmpty(a)) {
            return Array.of(elementToAdd);
        }

        final byte[] newArray = new byte[a.length + 1];

        copy(a, 0, newArray, 0, a.length);
        newArray[a.length] = elementToAdd;

        return newArray;
    }

    /**
     * <p>
     * Copies the given array and adds the given elementToAdd at the end of the new
     * array.
     *
     * @param a
     * @param elementToAdd
     * @return A new array containing the existing elementToAdds plus the new elementToAdd
     */
    public static short[] add(final short[] a, final short elementToAdd) {
        if (isEmpty(a)) {
            return Array.of(elementToAdd);
        }

        final short[] newArray = new short[a.length + 1];

        copy(a, 0, newArray, 0, a.length);
        newArray[a.length] = elementToAdd;

        return newArray;
    }

    /**
     * <p>
     * Copies the given array and adds the given elementToAdd at the end of the new
     * array.
     *
     * @param a
     * @param elementToAdd
     * @return A new array containing the existing elementToAdds plus the new elementToAdd
     */
    public static int[] add(final int[] a, final int elementToAdd) {
        if (isEmpty(a)) {
            return Array.of(elementToAdd);
        }

        final int[] newArray = new int[a.length + 1];

        copy(a, 0, newArray, 0, a.length);
        newArray[a.length] = elementToAdd;

        return newArray;
    }

    /**
     * <p>
     * Copies the given array and adds the given elementToAdd at the end of the new
     * array.
     *
     * @param a
     * @param elementToAdd
     * @return A new array containing the existing elementToAdds plus the new elementToAdd
     */
    public static long[] add(final long[] a, final long elementToAdd) {
        if (isEmpty(a)) {
            return Array.of(elementToAdd);
        }

        final long[] newArray = new long[a.length + 1];

        copy(a, 0, newArray, 0, a.length);
        newArray[a.length] = elementToAdd;

        return newArray;
    }

    /**
     * <p>
     * Copies the given array and adds the given elementToAdd at the end of the new
     * array.
     *
     * @param a
     * @param elementToAdd
     * @return A new array containing the existing elementToAdds plus the new elementToAdd
     */
    public static float[] add(final float[] a, final float elementToAdd) {
        if (isEmpty(a)) {
            return Array.of(elementToAdd);
        }

        final float[] newArray = new float[a.length + 1];

        copy(a, 0, newArray, 0, a.length);
        newArray[a.length] = elementToAdd;

        return newArray;
    }

    /**
     * <p>
     * Copies the given array and adds the given elementToAdd at the end of the new
     * array.
     *
     * @param a
     * @param elementToAdd
     * @return A new array containing the existing elementToAdds plus the new elementToAdd
     */
    public static double[] add(final double[] a, final double elementToAdd) {
        if (isEmpty(a)) {
            return Array.of(elementToAdd);
        }

        final double[] newArray = new double[a.length + 1];

        copy(a, 0, newArray, 0, a.length);
        newArray[a.length] = elementToAdd;

        return newArray;
    }

    /**
     * <p>
     * Copies the given array and adds the given elementToAdd at the end of the new
     * array.
     *
     * @param a
     * @param elementToAdd
     * @return A new array containing the existing elementToAdds plus the new elementToAdd
     */
    public static String[] add(final String[] a, final String elementToAdd) {
        if (isEmpty(a)) {
            return asArray(elementToAdd);
        }

        final String[] newArray = new String[a.length + 1];

        copy(a, 0, newArray, 0, a.length);
        newArray[a.length] = elementToAdd;

        return newArray;
    }

    /**
     * <p>
     * Copies the given array and adds the given elementToAdd at the end of the new
     * array.
     *
     * @param <T>
     * @param a
     * @param elementToAdd
     * @return A new array containing the existing elementToAdds plus the new elementToAdd
     * @throws IllegalArgumentException if the specified {@code Array} is <code>null</code>.
     */
    public static <T> T[] add(final T[] a, final T elementToAdd) throws IllegalArgumentException {
        checkArgNotNull(a, "a");

        final int len = a.length;
        final T[] newArray = (T[]) Array.newInstance(a.getClass().getComponentType(), len + 1);

        if (len > 0) {
            copy(a, 0, newArray, 0, len);
        }

        newArray[len] = elementToAdd;

        return newArray;
    }

    /**
     * <p>
     * Adds all the elementToAdds of the given arrays into a new array.
     * </p>
     *
     * @param a
     *            the first array whose elementToAdds are added to the new array.
     * @param elementsToAdd
     *            the second array whose elementToAdds are added to the new array.
     * @return A new array containing the elementToAdds from a and b
     */
    @SafeVarargs
    public static boolean[] addAll(final boolean[] a, final boolean... elementsToAdd) {
        if (isEmpty(a)) {
            return isEmpty(elementsToAdd) ? EMPTY_BOOLEAN_ARRAY : elementsToAdd.clone();
        }

        final boolean[] newArray = new boolean[a.length + elementsToAdd.length];

        copy(a, 0, newArray, 0, a.length);
        copy(elementsToAdd, 0, newArray, a.length, elementsToAdd.length);

        return newArray;
    }

    /**
     * <p>
     * Adds all the elementToAdds of the given arrays into a new array.
     * </p>
     *
     * @param a
     *            the first array whose elementToAdds are added to the new array.
     * @param elementsToAdd
     *            the second array whose elementToAdds are added to the new array.
     * @return A new array containing the elementToAdds from a and b
     */
    @SafeVarargs
    public static char[] addAll(final char[] a, final char... elementsToAdd) {
        if (isEmpty(a)) {
            return isEmpty(elementsToAdd) ? EMPTY_CHAR_ARRAY : elementsToAdd.clone();
        }

        final char[] newArray = new char[a.length + elementsToAdd.length];

        copy(a, 0, newArray, 0, a.length);
        copy(elementsToAdd, 0, newArray, a.length, elementsToAdd.length);

        return newArray;
    }

    /**
     * <p>
     * Adds all the elementToAdds of the given arrays into a new array.
     * </p>
     *
     * @param a
     *            the first array whose elementToAdds are added to the new array.
     * @param elementsToAdd
     *            the second array whose elementToAdds are added to the new array.
     * @return A new array containing the elementToAdds from a and b
     */
    @SafeVarargs
    public static byte[] addAll(final byte[] a, final byte... elementsToAdd) {
        if (isEmpty(a)) {
            return isEmpty(elementsToAdd) ? EMPTY_BYTE_ARRAY : elementsToAdd.clone();
        }

        final byte[] newArray = new byte[a.length + elementsToAdd.length];

        copy(a, 0, newArray, 0, a.length);
        copy(elementsToAdd, 0, newArray, a.length, elementsToAdd.length);

        return newArray;
    }

    /**
     * <p>
     * Adds all the elementToAdds of the given arrays into a new array.
     * </p>
     *
     * @param a
     *            the first array whose elementToAdds are added to the new array.
     * @param elementsToAdd
     *            the second array whose elementToAdds are added to the new array.
     * @return A new array containing the elementToAdds from a and b
     */
    @SafeVarargs
    public static short[] addAll(final short[] a, final short... elementsToAdd) {
        if (isEmpty(a)) {
            return isEmpty(elementsToAdd) ? EMPTY_SHORT_ARRAY : elementsToAdd.clone();
        }

        final short[] newArray = new short[a.length + elementsToAdd.length];

        copy(a, 0, newArray, 0, a.length);
        copy(elementsToAdd, 0, newArray, a.length, elementsToAdd.length);

        return newArray;
    }

    /**
     * <p>
     * Adds all the elementToAdds of the given arrays into a new array.
     * </p>
     *
     * @param a
     *            the first array whose elementToAdds are added to the new array.
     * @param elementsToAdd
     *            the second array whose elementToAdds are added to the new array.
     * @return A new array containing the elementToAdds from a and b
     */
    @SafeVarargs
    public static int[] addAll(final int[] a, final int... elementsToAdd) {
        if (isEmpty(a)) {
            return isEmpty(elementsToAdd) ? EMPTY_INT_ARRAY : elementsToAdd.clone();
        }

        final int[] newArray = new int[a.length + elementsToAdd.length];

        copy(a, 0, newArray, 0, a.length);
        copy(elementsToAdd, 0, newArray, a.length, elementsToAdd.length);

        return newArray;
    }

    /**
     * <p>
     * Adds all the elementToAdds of the given arrays into a new array.
     * </p>
     *
     * @param a
     *            the first array whose elementToAdds are added to the new array.
     * @param elementsToAdd
     *            the second array whose elementToAdds are added to the new array.
     * @return A new array containing the elementToAdds from a and b
     */
    @SafeVarargs
    public static long[] addAll(final long[] a, final long... elementsToAdd) {
        if (isEmpty(a)) {
            return isEmpty(elementsToAdd) ? EMPTY_LONG_ARRAY : elementsToAdd.clone();
        }

        final long[] newArray = new long[a.length + elementsToAdd.length];

        copy(a, 0, newArray, 0, a.length);
        copy(elementsToAdd, 0, newArray, a.length, elementsToAdd.length);

        return newArray;
    }

    /**
     * <p>
     * Adds all the elementToAdds of the given arrays into a new array.
     * </p>
     *
     * @param a
     *            the first array whose elementToAdds are added to the new array.
     * @param elementsToAdd
     *            the second array whose elementToAdds are added to the new array.
     * @return A new array containing the elementToAdds from a and b
     */
    @SafeVarargs
    public static float[] addAll(final float[] a, final float... elementsToAdd) {
        if (isEmpty(a)) {
            return isEmpty(elementsToAdd) ? EMPTY_FLOAT_ARRAY : elementsToAdd.clone();
        }

        final float[] newArray = new float[a.length + elementsToAdd.length];

        copy(a, 0, newArray, 0, a.length);
        copy(elementsToAdd, 0, newArray, a.length, elementsToAdd.length);

        return newArray;
    }

    /**
     * <p>
     * Adds all the elementToAdds of the given arrays into a new array.
     * </p>
     *
     * @param a
     *            the first array whose elementToAdds are added to the new array.
     * @param elementsToAdd
     *            the second array whose elementToAdds are added to the new array.
     * @return A new array containing the elementToAdds from a and b
     */
    @SafeVarargs
    public static double[] addAll(final double[] a, final double... elementsToAdd) {
        if (isEmpty(a)) {
            return isEmpty(elementsToAdd) ? EMPTY_DOUBLE_ARRAY : elementsToAdd.clone();
        }

        final double[] newArray = new double[a.length + elementsToAdd.length];

        copy(a, 0, newArray, 0, a.length);
        copy(elementsToAdd, 0, newArray, a.length, elementsToAdd.length);

        return newArray;
    }

    /**
     * <p>
     * Adds all the elementToAdds of the given arrays into a new array.
     * </p>
     *
     * @param a
     *            the first array whose elementToAdds are added to the new array.
     * @param elementsToAdd
     *            the second array whose elementToAdds are added to the new array.
     * @return A new array containing the elementToAdds from a and b
     */
    @SafeVarargs
    public static String[] addAll(final String[] a, final String... elementsToAdd) {
        if (isEmpty(a)) {
            return isEmpty(elementsToAdd) ? EMPTY_STRING_ARRAY : elementsToAdd.clone();
        }

        final String[] newArray = new String[a.length + elementsToAdd.length];

        copy(a, 0, newArray, 0, a.length);
        copy(elementsToAdd, 0, newArray, a.length, elementsToAdd.length);

        return newArray;
    }

    /**
     * <p>
     * Adds all the elementToAdds of the given arrays into a new array.
     * </p>
     *
     * @param <T>
     * @param a the first array whose elementToAdds are added to the new array.
     * @param elementsToAdd the second array whose elementToAdds are added to the new array.
     * @return A new array containing the elementToAdds from a and b
     * @throws IllegalArgumentException if the specified {@code Array} is <code>null</code>.
     */
    @SafeVarargs
    public static <T> T[] addAll(final T[] a, final T... elementsToAdd) throws IllegalArgumentException {
        checkArgNotNull(a, "a");

        if (isEmpty(a)) {
            return isEmpty(elementsToAdd) ? a.clone() : elementsToAdd.clone();
        }

        final T[] newArray = (T[]) Array.newInstance(a.getClass().getComponentType(), a.length + elementsToAdd.length);

        copy(a, 0, newArray, 0, a.length);
        copy(elementsToAdd, 0, newArray, a.length, elementsToAdd.length);

        return newArray;
    }

    /**
     *
     *
     * @param <T>
     * @param c
     * @param elementsToAdd
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> boolean addAll(final Collection<T> c, final T... elementsToAdd) throws IllegalArgumentException {
        checkArgNotNull(c, "c");

        if (isEmpty(elementsToAdd)) {
            return false;
        }

        return Collections.addAll(c, elementsToAdd);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param elementsToAdd
     * @return {@code true} if the specified Collection {@code c} has been modified.
     * @throws IllegalArgumentException
     */
    public static <T> boolean addAll(final Collection<T> c, final Iterator<? extends T> elementsToAdd) throws IllegalArgumentException {
        checkArgNotNull(c, "c");

        if (elementsToAdd == null) {
            return false;
        }

        boolean modified = false;

        while (elementsToAdd.hasNext()) {
            modified = c.add(elementsToAdd.next()) || modified;
        }

        return modified;
    }

    /**
     * <p>
     * Inserts the specified elementToInsert at the specified position in the array.
     * Shifts the elementToInsert currently at that position (if any) and any subsequent
     * elementToInserts to the right (adds one to their indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elementToInserts of the input array
     * plus the given elementToInsert on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param index the position of the new object
     * @param elementToInsert the object to add
     * @return A new array containing the existing elementToInserts and the new elementToInsert
     */
    public static boolean[] insert(final boolean[] a, final int index, final boolean elementToInsert) {
        if (isEmpty(a) && index == 0) {
            return Array.of(elementToInsert);
        }

        final boolean[] newArray = new boolean[a.length + 1];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        newArray[index] = elementToInsert;

        if (index < a.length) {
            copy(a, index, newArray, index + 1, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elementToInsert at the specified position in the array.
     * Shifts the elementToInsert currently at that position (if any) and any subsequent
     * elementToInserts to the right (adds one to their indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elementToInserts of the input array
     * plus the given elementToInsert on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param index the position of the new object
     * @param elementToInsert the object to add
     * @return A new array containing the existing elementToInserts and the new elementToInsert
     */
    public static char[] insert(final char[] a, final int index, final char elementToInsert) {
        if (isEmpty(a) && index == 0) {
            return Array.of(elementToInsert);
        }

        final char[] newArray = new char[a.length + 1];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        newArray[index] = elementToInsert;

        if (index < a.length) {
            copy(a, index, newArray, index + 1, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elementToInsert at the specified position in the array.
     * Shifts the elementToInsert currently at that position (if any) and any subsequent
     * elementToInserts to the right (adds one to their indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elementToInserts of the input array
     * plus the given elementToInsert on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param index the position of the new object
     * @param elementToInsert the object to add
     * @return A new array containing the existing elementToInserts and the new elementToInsert
     */
    public static byte[] insert(final byte[] a, final int index, final byte elementToInsert) {
        if (isEmpty(a) && index == 0) {
            return Array.of(elementToInsert);
        }

        final byte[] newArray = new byte[a.length + 1];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        newArray[index] = elementToInsert;

        if (index < a.length) {
            copy(a, index, newArray, index + 1, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elementToInsert at the specified position in the array.
     * Shifts the elementToInsert currently at that position (if any) and any subsequent
     * elementToInserts to the right (adds one to their indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elementToInserts of the input array
     * plus the given elementToInsert on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param index the position of the new object
     * @param elementToInsert the object to add
     * @return A new array containing the existing elementToInserts and the new elementToInsert
     */
    public static short[] insert(final short[] a, final int index, final short elementToInsert) {
        if (isEmpty(a) && index == 0) {
            return Array.of(elementToInsert);
        }

        final short[] newArray = new short[a.length + 1];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        newArray[index] = elementToInsert;

        if (index < a.length) {
            copy(a, index, newArray, index + 1, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elementToInsert at the specified position in the array.
     * Shifts the elementToInsert currently at that position (if any) and any subsequent
     * elementToInserts to the right (adds one to their indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elementToInserts of the input array
     * plus the given elementToInsert on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param index the position of the new object
     * @param elementToInsert the object to add
     * @return A new array containing the existing elementToInserts and the new elementToInsert
     */
    public static int[] insert(final int[] a, final int index, final int elementToInsert) {
        if (isEmpty(a) && index == 0) {
            return Array.of(elementToInsert);
        }

        final int[] newArray = new int[a.length + 1];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        newArray[index] = elementToInsert;

        if (index < a.length) {
            copy(a, index, newArray, index + 1, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elementToInsert at the specified position in the array.
     * Shifts the elementToInsert currently at that position (if any) and any subsequent
     * elementToInserts to the right (adds one to their indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elementToInserts of the input array
     * plus the given elementToInsert on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param index the position of the new object
     * @param elementToInsert the object to add
     * @return A new array containing the existing elementToInserts and the new elementToInsert
     */
    public static long[] insert(final long[] a, final int index, final long elementToInsert) {
        if (isEmpty(a) && index == 0) {
            return Array.of(elementToInsert);
        }

        final long[] newArray = new long[a.length + 1];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        newArray[index] = elementToInsert;

        if (index < a.length) {
            copy(a, index, newArray, index + 1, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elementToInsert at the specified position in the array.
     * Shifts the elementToInsert currently at that position (if any) and any subsequent
     * elementToInserts to the right (adds one to their indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elementToInserts of the input array
     * plus the given elementToInsert on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param index the position of the new object
     * @param elementToInsert the object to add
     * @return A new array containing the existing elementToInserts and the new elementToInsert
     */
    public static float[] insert(final float[] a, final int index, final float elementToInsert) {
        if (isEmpty(a) && index == 0) {
            return Array.of(elementToInsert);
        }

        final float[] newArray = new float[a.length + 1];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        newArray[index] = elementToInsert;

        if (index < a.length) {
            copy(a, index, newArray, index + 1, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elementToInsert at the specified position in the array.
     * Shifts the elementToInsert currently at that position (if any) and any subsequent
     * elementToInserts to the right (adds one to their indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elementToInserts of the input array
     * plus the given elementToInsert on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param index the position of the new object
     * @param elementToInsert the object to add
     * @return A new array containing the existing elementToInserts and the new elementToInsert
     */
    public static double[] insert(final double[] a, final int index, final double elementToInsert) {
        if (isEmpty(a) && index == 0) {
            return Array.of(elementToInsert);
        }

        final double[] newArray = new double[a.length + 1];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        newArray[index] = elementToInsert;

        if (index < a.length) {
            copy(a, index, newArray, index + 1, a.length - index);
        }

        return newArray;
    }

    /**
     *
     * @param a
     * @param index
     * @param elementToInsert
     * @return
     */
    public static String[] insert(final String[] a, final int index, final String elementToInsert) {
        if (isEmpty(a) && index == 0) {
            return asArray(elementToInsert);
        }

        final String[] newArray = new String[a.length + 1];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        newArray[index] = elementToInsert;

        if (index < a.length) {
            copy(a, index, newArray, index + 1, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elementToInsert at the specified position in the array.
     * Shifts the elementToInsert currently at that position (if any) and any subsequent
     * elementToInserts to the right (adds one to their indices).
     * </p>
     *
     * <p>
     * This method returns a new array with the same elementToInserts of the input array
     * plus the given elementToInsert on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     *
     * @param <T>
     * @param a
     * @param index the position of the new object
     * @param elementToInsert the object to add
     * @return A new array containing the existing elementToInserts and the new elementToInsert
     * @throws IllegalArgumentException if the specified {@code Array} is <code>null</code>.
     */
    public static <T> T[] insert(final T[] a, final int index, final T elementToInsert) throws IllegalArgumentException {
        checkArgNotNull(a, "a");

        final T[] newArray = newArray(a.getClass().getComponentType(), a.length + 1);

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        newArray[index] = elementToInsert;

        if (index < a.length) {
            copy(a, index, newArray, index + 1, a.length - index);
        }

        return newArray;
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param list
    //     * @param index
    //     * @param elementToInsert
    //     * @throws IllegalArgumentException
    //     */
    //    public static <T> void insert(final List<T> list, final int index, final T elementToInsert) throws IllegalArgumentException {
    //        checkArgNotNull(list, "list");
    //
    //        list.add(index, elementToInsert);
    //    }

    /**
     * Returns a new String.
     *
     * @param str
     * @param index
     * @param strToInsert
     * @return a new String
     */
    public static String insert(final String str, final int index, final String strToInsert) {
        checkIndex(index, len(str));

        if (Strings.isEmpty(strToInsert)) {
            return Strings.nullToEmpty(str);
        } else if (Strings.isEmpty(str)) {
            return Strings.nullToEmpty(strToInsert);
        } else if (index == str.length()) {
            return Strings.concat(str + strToInsert);
        }

        return str;
    }

    /**
     * <p>
     * Inserts the specified elementToInserts at the specified position in the array.
     * Shifts the elementToInsert currently at that position (if any) and any subsequent
     * elementToInserts to the right (adds one to their indices).
     * </p>
     *
     * @param a
     *            the first array whose elementToInserts are added to the new array.
     * @param index
     *            the position of the new elementToInserts start from
     * @param elementsToInsert
     *            the second array whose elementToInserts are added to the new array.
     * @return A new array containing the elementToInserts from a and b
     */
    @SafeVarargs
    public static boolean[] insertAll(final boolean[] a, final int index, final boolean... elementsToInsert) {
        if (isEmpty(a) && index == 0) {
            return elementsToInsert.clone();
        }

        final boolean[] newArray = new boolean[a.length + elementsToInsert.length];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        copy(elementsToInsert, 0, newArray, index, elementsToInsert.length);

        if (index < a.length) {
            copy(a, index, newArray, index + elementsToInsert.length, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elementToInserts at the specified position in the array.
     * Shifts the elementToInsert currently at that position (if any) and any subsequent
     * elementToInserts to the right (adds one to their indices).
     * </p>
     *
     * @param a
     *            the first array whose elementToInserts are added to the new array.
     * @param index
     *            the position of the new elementToInserts start from
     * @param elementsToInsert
     *            the second array whose elementToInserts are added to the new array.
     * @return A new array containing the elementToInserts from a and b
     */
    @SafeVarargs
    public static char[] insertAll(final char[] a, final int index, final char... elementsToInsert) {
        if (isEmpty(a) && index == 0) {
            return elementsToInsert.clone();
        }

        final char[] newArray = new char[a.length + elementsToInsert.length];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        copy(elementsToInsert, 0, newArray, index, elementsToInsert.length);

        if (index < a.length) {
            copy(a, index, newArray, index + elementsToInsert.length, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elementToInserts at the specified position in the array.
     * Shifts the elementToInsert currently at that position (if any) and any subsequent
     * elementToInserts to the right (adds one to their indices).
     * </p>
     *
     * @param a
     *            the first array whose elementToInserts are added to the new array.
     * @param index
     *            the position of the new elementToInserts start from
     * @param elementsToInsert
     *            the second array whose elementToInserts are added to the new array.
     * @return A new array containing the elementToInserts from a and b
     */
    @SafeVarargs
    public static byte[] insertAll(final byte[] a, final int index, final byte... elementsToInsert) {
        if (isEmpty(a) && index == 0) {
            return elementsToInsert.clone();
        }

        final byte[] newArray = new byte[a.length + elementsToInsert.length];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        copy(elementsToInsert, 0, newArray, index, elementsToInsert.length);

        if (index < a.length) {
            copy(a, index, newArray, index + elementsToInsert.length, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elementToInserts at the specified position in the array.
     * Shifts the elementToInsert currently at that position (if any) and any subsequent
     * elementToInserts to the right (adds one to their indices).
     * </p>
     *
     * @param a
     *            the first array whose elementToInserts are added to the new array.
     * @param index
     *            the position of the new elementToInserts start from
     * @param elementsToInsert
     *            the second array whose elementToInserts are added to the new array.
     * @return A new array containing the elementToInserts from a and b
     */
    @SafeVarargs
    public static short[] insertAll(final short[] a, final int index, final short... elementsToInsert) {
        if (isEmpty(a) && index == 0) {
            return elementsToInsert.clone();
        }

        final short[] newArray = new short[a.length + elementsToInsert.length];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        copy(elementsToInsert, 0, newArray, index, elementsToInsert.length);

        if (index < a.length) {
            copy(a, index, newArray, index + elementsToInsert.length, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elementToInserts at the specified position in the array.
     * Shifts the elementToInsert currently at that position (if any) and any subsequent
     * elementToInserts to the right (adds one to their indices).
     * </p>
     *
     * @param a
     *            the first array whose elementToInserts are added to the new array.
     * @param index
     *            the position of the new elementToInserts start from
     * @param elementsToInsert
     *            the second array whose elementToInserts are added to the new array.
     * @return A new array containing the elementToInserts from a and b
     */
    @SafeVarargs
    public static int[] insertAll(final int[] a, final int index, final int... elementsToInsert) {
        if (isEmpty(a) && index == 0) {
            return elementsToInsert.clone();
        }

        final int[] newArray = new int[a.length + elementsToInsert.length];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        copy(elementsToInsert, 0, newArray, index, elementsToInsert.length);

        if (index < a.length) {
            copy(a, index, newArray, index + elementsToInsert.length, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elementToInserts at the specified position in the array.
     * Shifts the elementToInsert currently at that position (if any) and any subsequent
     * elementToInserts to the right (adds one to their indices).
     * </p>
     *
     * @param a
     *            the first array whose elementToInserts are added to the new array.
     * @param index
     *            the position of the new elementToInserts start from
     * @param elementsToInsert
     *            the second array whose elementToInserts are added to the new array.
     * @return A new array containing the elementToInserts from a and b
     */
    @SafeVarargs
    public static long[] insertAll(final long[] a, final int index, final long... elementsToInsert) {
        if (isEmpty(a) && index == 0) {
            return elementsToInsert.clone();
        }

        final long[] newArray = new long[a.length + elementsToInsert.length];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        copy(elementsToInsert, 0, newArray, index, elementsToInsert.length);

        if (index < a.length) {
            copy(a, index, newArray, index + elementsToInsert.length, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elementToInserts at the specified position in the array.
     * Shifts the elementToInsert currently at that position (if any) and any subsequent
     * elementToInserts to the right (adds one to their indices).
     * </p>
     *
     * @param a
     *            the first array whose elementToInserts are added to the new array.
     * @param index
     *            the position of the new elementToInserts start from
     * @param elementsToInsert
     *            the second array whose elementToInserts are added to the new array.
     * @return A new array containing the elementToInserts from a and b
     */
    @SafeVarargs
    public static float[] insertAll(final float[] a, final int index, final float... elementsToInsert) {
        if (isEmpty(a) && index == 0) {
            return elementsToInsert.clone();
        }

        final float[] newArray = new float[a.length + elementsToInsert.length];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        copy(elementsToInsert, 0, newArray, index, elementsToInsert.length);

        if (index < a.length) {
            copy(a, index, newArray, index + elementsToInsert.length, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elementToInserts at the specified position in the array.
     * Shifts the elementToInsert currently at that position (if any) and any subsequent
     * elementToInserts to the right (adds one to their indices).
     * </p>
     *
     * @param a
     *            the first array whose elementToInserts are added to the new array.
     * @param index
     *            the position of the new elementToInserts start from
     * @param elementsToInsert
     *            the second array whose elementToInserts are added to the new array.
     * @return A new array containing the elementToInserts from a and b
     */
    @SafeVarargs
    public static double[] insertAll(final double[] a, final int index, final double... elementsToInsert) {
        if (isEmpty(a) && index == 0) {
            return elementsToInsert.clone();
        }

        final double[] newArray = new double[a.length + elementsToInsert.length];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        copy(elementsToInsert, 0, newArray, index, elementsToInsert.length);

        if (index < a.length) {
            copy(a, index, newArray, index + elementsToInsert.length, a.length - index);
        }

        return newArray;
    }

    /**
     *
     * @param a
     * @param index
     * @param elementsToInsert
     * @return
     */
    @SafeVarargs
    public static String[] insertAll(final String[] a, final int index, final String... elementsToInsert) {
        if (isEmpty(a) && index == 0) {
            return elementsToInsert.clone();
        }

        final String[] newArray = new String[a.length + elementsToInsert.length];

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        copy(elementsToInsert, 0, newArray, index, elementsToInsert.length);

        if (index < a.length) {
            copy(a, index, newArray, index + elementsToInsert.length, a.length - index);
        }

        return newArray;
    }

    /**
     * <p>
     * Inserts the specified elementToInserts at the specified position in the array.
     * Shifts the elementToInsert currently at that position (if any) and any subsequent
     * elementToInserts to the right (adds one to their indices).
     * </p>
     *
     * @param <T>
     * @param a the first array whose elementToInserts are added to the new array.
     * @param index the position of the new elementToInserts start from
     * @param elementsToInsert the second array whose elementToInserts are added to the new array.
     * @return A new array containing the elementToInserts from a and b
     * @throws IllegalArgumentException if the specified {@code Array} is <code>null</code>.
     */
    @SafeVarargs
    public static <T> T[] insertAll(final T[] a, final int index, final T... elementsToInsert) throws IllegalArgumentException {
        checkArgNotNull(a, "a");

        final T[] newArray = (T[]) Array.newInstance(a.getClass().getComponentType(), a.length + elementsToInsert.length);

        if (index > 0) {
            copy(a, 0, newArray, 0, index);
        }

        copy(elementsToInsert, 0, newArray, index, elementsToInsert.length);

        if (index < a.length) {
            copy(a, index, newArray, index + elementsToInsert.length, a.length - index);
        }

        return newArray;
    }

    /**
     *
     * @param <T>
     * @param list
     * @param index
     * @param elementsToInsert
     * @return
     * @throws IllegalArgumentException
     */
    @SafeVarargs
    public static <T> boolean insertAll(final List<T> list, final int index, final T... elementsToInsert) throws IllegalArgumentException {
        checkArgNotNull(list, "list");

        if (isEmpty(elementsToInsert)) {
            return false;
        }

        list.addAll(index, asList(elementsToInsert));

        return true;
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
        checkArgNotNull(a, "a");

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
        checkArgNotNull(a, "a");

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
        checkArgNotNull(a, "a");

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
        checkArgNotNull(a, "a");

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
        checkArgNotNull(a, "a");

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
        checkArgNotNull(a, "a");

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
        checkArgNotNull(a, "a");

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
        checkArgNotNull(a, "a");

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
     * @throws IllegalArgumentException if the specified {@code Array} is <code>null</code>.
     */
    public static <T> T[] delete(final T[] a, final int index) throws IllegalArgumentException {
        checkArgNotNull(a, "a");

        final T[] result = newArray(a.getClass().getComponentType(), a.length - 1);

        if (index > 0) {
            copy(a, 0, result, 0, index);
        }

        if (index + 1 < a.length) {
            copy(a, index + 1, result, index, a.length - index - 1);
        }

        return result;
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param list
    //     * @param index
    //     * @return
    //     * @throws IllegalArgumentException
    //     */
    //    public static <T> T delete(final List<T> list, final int index) throws IllegalArgumentException {
    //        checkArgNotNull(list, "list");
    //
    //        return list.remove(index);
    //    }

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
     * deleteAll([true, false, true], 0, 2) = [false]
     * removeAll([true, false, true], 1, 2) = [true]
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
        if (isEmpty(indices)) {
            return a == null ? EMPTY_BOOLEAN_ARRAY : a.clone();
        } else if (indices.length == 1) {
            return delete(a, indices[0]);
        }

        indices = indices.clone();
        sort(indices);
        final int lastIndex = indices[indices.length - 1];

        if (indices[0] < 0 || lastIndex >= a.length) {
            throw new IndexOutOfBoundsException("The specified indices are from: " + indices[0] + " to: " + lastIndex); //NOSONAR
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
     * deleteAll([1], 0)             = []
     * deleteAll([2, 6], 0)          = [6]
     * deleteAll([2, 6], 0, 1)       = []
     * deleteAll([2, 6, 3], 1, 2)    = [2]
     * deleteAll([2, 6, 3], 0, 2)    = [6]
     * deleteAll([2, 6, 3], 0, 1, 2) = []
     * </pre>
     *
     * @param a
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those at the
     *         specified positions.
     */
    @SafeVarargs
    public static char[] deleteAll(final char[] a, int... indices) {
        if (isEmpty(indices)) {
            return a == null ? EMPTY_CHAR_ARRAY : a.clone();
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
     * deleteAll([1], 0)             = []
     * deleteAll([2, 6], 0)          = [6]
     * deleteAll([2, 6], 0, 1)       = []
     * deleteAll([2, 6, 3], 1, 2)    = [2]
     * deleteAll([2, 6, 3], 0, 2)    = [6]
     * deleteAll([2, 6, 3], 0, 1, 2) = []
     * </pre>
     *
     * @param a
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those at the
     *         specified positions.
     */
    @SafeVarargs
    public static byte[] deleteAll(final byte[] a, int... indices) {
        if (isEmpty(indices)) {
            return a == null ? EMPTY_BYTE_ARRAY : a.clone();
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
     * deleteAll([1], 0)             = []
     * deleteAll([2, 6], 0)          = [6]
     * deleteAll([2, 6], 0, 1)       = []
     * deleteAll([2, 6, 3], 1, 2)    = [2]
     * deleteAll([2, 6, 3], 0, 2)    = [6]
     * deleteAll([2, 6, 3], 0, 1, 2) = []
     * </pre>
     *
     * @param a
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those at the
     *         specified positions.
     */
    @SafeVarargs
    public static short[] deleteAll(final short[] a, int... indices) {
        if (isEmpty(indices)) {
            return a == null ? EMPTY_SHORT_ARRAY : a.clone();
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
     * deleteAll([1], 0)             = []
     * deleteAll([2, 6], 0)          = [6]
     * deleteAll([2, 6], 0, 1)       = []
     * deleteAll([2, 6, 3], 1, 2)    = [2]
     * deleteAll([2, 6, 3], 0, 2)    = [6]
     * deleteAll([2, 6, 3], 0, 1, 2) = []
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
        if (isEmpty(indices)) {
            return a == null ? EMPTY_INT_ARRAY : a.clone();
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
     * deleteAll([1], 0)             = []
     * deleteAll([2, 6], 0)          = [6]
     * deleteAll([2, 6], 0, 1)       = []
     * deleteAll([2, 6, 3], 1, 2)    = [2]
     * deleteAll([2, 6, 3], 0, 2)    = [6]
     * deleteAll([2, 6, 3], 0, 1, 2) = []
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
        if (isEmpty(indices)) {
            return a == null ? EMPTY_LONG_ARRAY : a.clone();
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
     * deleteAll([1], 0)             = []
     * deleteAll([2, 6], 0)          = [6]
     * deleteAll([2, 6], 0, 1)       = []
     * deleteAll([2, 6, 3], 1, 2)    = [2]
     * deleteAll([2, 6, 3], 0, 2)    = [6]
     * deleteAll([2, 6, 3], 0, 1, 2) = []
     * </pre>
     *
     * @param a
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those at the
     *         specified positions.
     */
    @SafeVarargs
    public static float[] deleteAll(final float[] a, int... indices) {
        if (isEmpty(indices)) {
            return a == null ? EMPTY_FLOAT_ARRAY : a.clone();
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
     * deleteAll([1], 0)             = []
     * deleteAll([2, 6], 0)          = [6]
     * deleteAll([2, 6], 0, 1)       = []
     * deleteAll([2, 6, 3], 1, 2)    = [2]
     * deleteAll([2, 6, 3], 0, 2)    = [6]
     * deleteAll([2, 6, 3], 0, 1, 2) = []
     * </pre>
     *
     * @param a
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those at the
     *         specified positions.
     */
    @SafeVarargs
    public static double[] deleteAll(final double[] a, int... indices) {
        if (isEmpty(indices)) {
            return a == null ? EMPTY_DOUBLE_ARRAY : a.clone();
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
        }

        return result;
    }

    /**
     *
     *
     * @param a
     * @param indices
     * @return
     * @throws IllegalArgumentException
     */
    @SafeVarargs
    public static String[] deleteAll(final String[] a, int... indices) throws IllegalArgumentException {
        if (isEmpty(indices)) {
            return a == null ? EMPTY_STRING_ARRAY : a.clone();
        } else if (indices.length == 1) {
            return delete(a, indices[0]);
        }

        indices = indices.clone();
        sort(indices);
        return deleteAllBySortedIndices(a, indices);
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
     * deleteAll(["a", "b", "c"], 0, 2) = ["b"]
     * deleteAll(["a", "b", "c"], 1, 2) = ["a"]
     * </pre>
     *
     * @param <T> the component type of the array
     * @param a
     * @param indices the positions of the elements to be removed
     * @return A new array containing the existing elements except those at the
     *         specified positions.
     * @throws IllegalArgumentException if the specified {@code Array} is <code>null</code>.
     */
    @SafeVarargs
    public static <T> T[] deleteAll(final T[] a, int... indices) throws IllegalArgumentException {
        checkArgNotNull(a, "a");

        if (isEmpty(indices)) {
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
        }

        return result;
    }

    /**
     * Removes the elements at the specified positions from the specified List.
     *
     * @param list
     * @param indices
     * @return
     */
    @SuppressWarnings("rawtypes")
    @SafeVarargs
    public static boolean deleteAll(final List<?> list, int... indices) {
        checkArgNotNull(list);

        if (isEmpty(indices)) {
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
     * <p>
     * Removes the first occurrence of the specified elementToRemove from the specified
     * array. All subsequent elementsToRemove are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an elementToRemove, no
     * elementsToRemove are removed from the array.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elementsToRemove of the input array
     * except the first occurrence of the specified elementToRemove. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param elementToRemove the elementToRemove to be removed
     * @return A new array containing the existing elementsToRemove except the first
     *         occurrence of the specified elementToRemove.
     */
    public static boolean[] remove(final boolean[] a, final boolean elementToRemove) {
        if (isEmpty(a)) {
            return EMPTY_BOOLEAN_ARRAY;
        }

        int index = indexOf(a, 0, elementToRemove);

        return index == INDEX_NOT_FOUND ? a.clone() : delete(a, index);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified elementToRemove from the specified
     * array. All subsequent elementsToRemove are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an elementToRemove, no
     * elementsToRemove are removed from the array.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elementsToRemove of the input array
     * except the first occurrence of the specified elementToRemove. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param elementToRemove the elementToRemove to be removed
     * @return A new array containing the existing elementsToRemove except the first
     *         occurrence of the specified elementToRemove.
     */
    public static char[] remove(final char[] a, final char elementToRemove) {
        if (isEmpty(a)) {
            return EMPTY_CHAR_ARRAY;
        }

        int index = indexOf(a, 0, elementToRemove);

        return index == INDEX_NOT_FOUND ? a.clone() : delete(a, index);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified elementToRemove from the specified
     * array. All subsequent elementsToRemove are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an elementToRemove, no
     * elementsToRemove are removed from the array.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elementsToRemove of the input array
     * except the first occurrence of the specified elementToRemove. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param elementToRemove the elementToRemove to be removed
     * @return A new array containing the existing elementsToRemove except the first
     *         occurrence of the specified elementToRemove.
     */
    public static byte[] remove(final byte[] a, final byte elementToRemove) {
        if (isEmpty(a)) {
            return EMPTY_BYTE_ARRAY;
        }

        int index = indexOf(a, 0, elementToRemove);

        return index == INDEX_NOT_FOUND ? a.clone() : delete(a, index);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified elementToRemove from the specified
     * array. All subsequent elementsToRemove are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an elementToRemove, no
     * elementsToRemove are removed from the array.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elementsToRemove of the input array
     * except the first occurrence of the specified elementToRemove. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param elementToRemove the elementToRemove to be removed
     * @return A new array containing the existing elementsToRemove except the first
     *         occurrence of the specified elementToRemove.
     */
    public static short[] remove(final short[] a, final short elementToRemove) {
        if (isEmpty(a)) {
            return EMPTY_SHORT_ARRAY;
        }

        int index = indexOf(a, 0, elementToRemove);

        return index == INDEX_NOT_FOUND ? a.clone() : delete(a, index);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified elementToRemove from the specified
     * array. All subsequent elementsToRemove are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an elementToRemove, no
     * elementsToRemove are removed from the array.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elementsToRemove of the input array
     * except the first occurrence of the specified elementToRemove. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param elementToRemove the elementToRemove to be removed
     * @return A new array containing the existing elementsToRemove except the first
     *         occurrence of the specified elementToRemove.
     */
    public static int[] remove(final int[] a, final int elementToRemove) {
        if (isEmpty(a)) {
            return EMPTY_INT_ARRAY;
        }

        int index = indexOf(a, 0, elementToRemove);

        return index == INDEX_NOT_FOUND ? a.clone() : delete(a, index);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified elementToRemove from the specified
     * array. All subsequent elementsToRemove are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an elementToRemove, no
     * elementsToRemove are removed from the array.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elementsToRemove of the input array
     * except the first occurrence of the specified elementToRemove. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param elementToRemove the elementToRemove to be removed
     * @return A new array containing the existing elementsToRemove except the first
     *         occurrence of the specified elementToRemove.
     */
    public static long[] remove(final long[] a, final long elementToRemove) {
        if (isEmpty(a)) {
            return EMPTY_LONG_ARRAY;
        }

        int index = indexOf(a, 0, elementToRemove);

        return index == INDEX_NOT_FOUND ? a.clone() : delete(a, index);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified elementToRemove from the specified
     * array. All subsequent elementsToRemove are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an elementToRemove, no
     * elementsToRemove are removed from the array.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elementsToRemove of the input array
     * except the first occurrence of the specified elementToRemove. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param elementToRemove the elementToRemove to be removed
     * @return A new array containing the existing elementsToRemove except the first
     *         occurrence of the specified elementToRemove.
     */
    public static float[] remove(final float[] a, final float elementToRemove) {
        if (isEmpty(a)) {
            return EMPTY_FLOAT_ARRAY;
        }

        int index = indexOf(a, 0, elementToRemove);

        return index == INDEX_NOT_FOUND ? a.clone() : delete(a, index);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified elementToRemove from the specified
     * array. All subsequent elementsToRemove are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an elementToRemove, no
     * elementsToRemove are removed from the array.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elementsToRemove of the input array
     * except the first occurrence of the specified elementToRemove. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     *
     * @param a
     * @param elementToRemove the elementToRemove to be removed
     * @return A new array containing the existing elementsToRemove except the first
     *         occurrence of the specified elementToRemove.
     */
    public static double[] remove(final double[] a, final double elementToRemove) {
        if (isEmpty(a)) {
            return EMPTY_DOUBLE_ARRAY;
        }

        int index = indexOf(a, 0, elementToRemove);

        return index == INDEX_NOT_FOUND ? a.clone() : delete(a, index);
    }

    /**
     *
     *
     * @param a
     * @param elementToRemove
     * @return
     */
    public static String[] remove(final String[] a, final String elementToRemove) {
        if (isEmpty(a)) {
            return EMPTY_STRING_ARRAY;
        }

        int index = indexOf(a, 0, elementToRemove);

        return index == INDEX_NOT_FOUND ? a.clone() : delete(a, index);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified elementToRemove from the specified
     * array. All subsequent elementsToRemove are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an elementToRemove, no
     * elementsToRemove are removed from the array.
     * </p>
     *
     * <p>
     * This method returns a new array with the same elementsToRemove of the input array
     * except the first occurrence of the specified elementToRemove. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     *
     * @param <T>
     * @param a
     * @param elementToRemove the elementToRemove to be removed
     * @return A new array containing the existing elementsToRemove except the first
     *         occurrence of the specified elementToRemove. Or the specified array if it's {@code null} or empty.
     * @throws IllegalArgumentException if the specified {@code Array} is <code>null</code>.
     */
    public static <T> T[] remove(final T[] a, final T elementToRemove) throws IllegalArgumentException {
        if (isEmpty(a)) {
            return a;
        }

        int index = indexOf(a, 0, elementToRemove);

        return index == INDEX_NOT_FOUND ? a.clone() : delete(a, index);
    }

    /**
     * <p>
     * Removes the first occurrence of the specified elementToRemove from the specified
     * collection. If the collection doesn't contains such an elementToRemove, no
     * elementsToRemove are removed from the collection.
     * </p>
     *
     * @param <T>
     * @param c
     * @param elementToRemove the elementToRemove to be removed
     * @return <tt>true</tt> if this collection changed as a result of the call
     */
    public static <T> boolean remove(final Collection<T> c, final T elementToRemove) {
        if (isEmpty(c)) {
            return false;
        }

        return c.remove(elementToRemove);
    }

    /**
     * Returns a new array with removes all the occurrences of specified elementsToRemove from <code>a</code>.
     *
     * @param a
     * @param elementsToRemove
     * @return
     * @see Collection#removeAll(Collection)
     * @see N#difference(boolean[], boolean[])
     */
    @SafeVarargs
    public static boolean[] removeAll(final boolean[] a, final boolean... elementsToRemove) {
        if (isEmpty(a)) {
            return EMPTY_BOOLEAN_ARRAY;
        } else if (isEmpty(elementsToRemove)) {
            return a.clone();
        } else if (elementsToRemove.length == 1) {
            return removeAllOccurrences(a, elementsToRemove[0]);
        }

        final BooleanList list = BooleanList.of(a.clone());
        list.removeAll(BooleanList.of(elementsToRemove));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elementsToRemove from <code>a</code>.
     *
     * @param a
     * @param elementsToRemove
     * @return
     * @see Collection#removeAll(Collection)
     * @see N#difference(char[], char[])
     */
    @SafeVarargs
    public static char[] removeAll(final char[] a, final char... elementsToRemove) {
        if (isEmpty(a)) {
            return EMPTY_CHAR_ARRAY;
        } else if (isEmpty(elementsToRemove)) {
            return a.clone();
        } else if (elementsToRemove.length == 1) {
            return removeAllOccurrences(a, elementsToRemove[0]);
        }

        final CharList list = CharList.of(a.clone());
        list.removeAll(CharList.of(elementsToRemove));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elementsToRemove from <code>a</code>.
     *
     * @param a
     * @param elementsToRemove
     * @return
     * @see Collection#removeAll(Collection)
     * @see N#difference(byte[], byte[])
     */
    @SafeVarargs
    public static byte[] removeAll(final byte[] a, final byte... elementsToRemove) {
        if (isEmpty(a)) {
            return EMPTY_BYTE_ARRAY;
        } else if (isEmpty(elementsToRemove)) {
            return a.clone();
        } else if (elementsToRemove.length == 1) {
            return removeAllOccurrences(a, elementsToRemove[0]);
        }

        final ByteList list = ByteList.of(a.clone());
        list.removeAll(ByteList.of(elementsToRemove));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elementsToRemove from <code>a</code>.
     *
     * @param a
     * @param elementsToRemove
     * @return
     * @see Collection#removeAll(Collection)
     * @see N#difference(short[], short[])
     */
    @SafeVarargs
    public static short[] removeAll(final short[] a, final short... elementsToRemove) {
        if (isEmpty(a)) {
            return EMPTY_SHORT_ARRAY;
        } else if (isEmpty(elementsToRemove)) {
            return a.clone();
        } else if (elementsToRemove.length == 1) {
            return removeAllOccurrences(a, elementsToRemove[0]);
        }

        final ShortList list = ShortList.of(a.clone());
        list.removeAll(ShortList.of(elementsToRemove));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elementsToRemove from <code>a</code>.
     *
     * @param a
     * @param elementsToRemove
     * @return
     * @see Collection#removeAll(Collection)
     * @see N#difference(int[], int[])
     */
    @SafeVarargs
    public static int[] removeAll(final int[] a, final int... elementsToRemove) {
        if (isEmpty(a)) {
            return EMPTY_INT_ARRAY;
        } else if (isEmpty(elementsToRemove)) {
            return a.clone();
        } else if (elementsToRemove.length == 1) {
            return removeAllOccurrences(a, elementsToRemove[0]);
        }

        final IntList list = IntList.of(a.clone());
        list.removeAll(IntList.of(elementsToRemove));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elementsToRemove from <code>a</code>.
     *
     * @param a
     * @param elementsToRemove
     * @return
     * @see Collection#removeAll(Collection)
     * @see N#difference(long[], long[])
     */
    @SafeVarargs
    public static long[] removeAll(final long[] a, final long... elementsToRemove) {
        if (isEmpty(a)) {
            return EMPTY_LONG_ARRAY;
        } else if (isEmpty(elementsToRemove)) {
            return a.clone();
        } else if (elementsToRemove.length == 1) {
            return removeAllOccurrences(a, elementsToRemove[0]);
        }

        final LongList list = LongList.of(a.clone());
        list.removeAll(LongList.of(elementsToRemove));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elementsToRemove from <code>a</code>.
     *
     * @param a
     * @param elementsToRemove
     * @return
     * @see Collection#removeAll(Collection)
     * @see N#difference(float[], float[])
     */
    @SafeVarargs
    public static float[] removeAll(final float[] a, final float... elementsToRemove) {
        if (isEmpty(a)) {
            return EMPTY_FLOAT_ARRAY;
        } else if (isEmpty(elementsToRemove)) {
            return a.clone();
        } else if (elementsToRemove.length == 1) {
            return removeAllOccurrences(a, elementsToRemove[0]);
        }

        final FloatList list = FloatList.of(a.clone());
        list.removeAll(FloatList.of(elementsToRemove));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elementsToRemove from <code>a</code>.
     *
     * @param a
     * @param elementsToRemove
     * @return
     * @see Collection#removeAll(Collection)
     * @see N#difference(double[], double[])
     */
    @SafeVarargs
    public static double[] removeAll(final double[] a, final double... elementsToRemove) {
        if (isEmpty(a)) {
            return EMPTY_DOUBLE_ARRAY;
        } else if (isEmpty(elementsToRemove)) {
            return a.clone();
        } else if (elementsToRemove.length == 1) {
            return removeAllOccurrences(a, elementsToRemove[0]);
        }

        final DoubleList list = DoubleList.of(a.clone());
        list.removeAll(DoubleList.of(elementsToRemove));
        return list.trimToSize().array();
    }

    /**
     * Returns a new array with removes all the occurrences of specified elementsToRemove from <code>a</code>.
     *
     * @param a
     * @param elementsToRemove
     * @return
     * @see Collection#removeAll(Collection)
     * @see N#difference(double[], double[])
     */
    @SafeVarargs
    public static String[] removeAll(final String[] a, final String... elementsToRemove) {
        if (isEmpty(a)) {
            return EMPTY_STRING_ARRAY;
        } else if (isEmpty(elementsToRemove)) {
            return a.clone();
        } else if (elementsToRemove.length == 1) {
            return removeAllOccurrences(a, elementsToRemove[0]);
        }

        final Set<String> set = asSet(elementsToRemove);
        final List<String> result = new ArrayList<>();

        for (String e : a) {
            if (!set.contains(e)) {
                result.add(e);
            }
        }

        return result.toArray(new String[result.size()]);
    }

    /**
     * Returns a new array with removes all the occurrences of specified elementsToRemove from <code>a</code>.
     *
     * @param <T>
     * @param a
     * @param elementsToRemove
     * @return the specified array if it's {@code null} or empty.
     * @see Collection#removeAll(Collection)
     * @see N#difference(Object[], Object[])
     */
    @SafeVarargs
    public static <T> T[] removeAll(final T[] a, final T... elementsToRemove) {
        if (isEmpty(a)) {
            return a;
        } else if (isEmpty(elementsToRemove)) {
            return a.clone();
        } else if (elementsToRemove.length == 1) {
            return removeAllOccurrences(a, elementsToRemove[0]);
        }

        final Set<Object> set = asSet(elementsToRemove);
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
     * @param <T>
     * @param c
     * @param elementsToRemove
     * @return
     * @see N#differentSet(Collection, Collection)
     */
    @SafeVarargs
    public static <T> boolean removeAll(final Collection<T> c, final T... elementsToRemove) {
        if (isEmpty(c) || isEmpty(elementsToRemove)) {
            return false;
        } else {
            return removeAll(c, asSet(elementsToRemove));
        }
    }

    /**
     * Removes the all.
     *
     * @param <T>
     * @param c
     * @param elementsToRemove
     * @return
     * @see N#differentSet(Collection, Collection)
     */
    public static <T> boolean removeAll(final Collection<T> c, final Collection<?> elementsToRemove) {
        if (isEmpty(c) || isEmpty(elementsToRemove)) {
            return false;
        }

        if (c instanceof HashSet && !(elementsToRemove instanceof Set)) {
            boolean result = false;

            for (Object e : elementsToRemove) {
                result |= c.remove(e);

                if (c.size() == 0) {
                    break;
                }
            }

            return result;
        } else {
            return c.removeAll(elementsToRemove);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param c
     * @param elementsToRemove
     * @return
     */
    public static <T> boolean removeAll(final Collection<T> c, final Iterator<?> elementsToRemove) {
        if (isEmpty(c) || elementsToRemove == null) {
            return false;
        }

        if (c instanceof Set) {
            final Set<T> set = (Set<T>) c;
            final int originalSize = set.size();

            while (elementsToRemove.hasNext()) {
                set.remove(elementsToRemove.next());
            }

            return set.size() != originalSize;
        } else {
            return removeAll(c, toSet(elementsToRemove));
        }
    }

    /**
     * Removes all the occurrences of the specified elementToRemove from the specified
     * array. All subsequent elementsToRemove are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an elementToRemove, no
     * elementsToRemove are removed from the array.
     *
     * @param a
     * @param elementToRemove
     * @return A new array containing the existing elementsToRemove except the
     *         occurrences of the specified elementToRemove.
     */
    public static boolean[] removeAllOccurrences(final boolean[] a, final boolean elementToRemove) {
        if (isEmpty(a)) {
            return EMPTY_BOOLEAN_ARRAY;
        }

        final boolean[] copy = a.clone();
        int idx = 0;

        for (boolean element : a) {
            if (element == elementToRemove) {
                continue;
            }

            copy[idx++] = element;
        }

        return idx == copy.length ? copy : copyOfRange(copy, 0, idx);
    }

    /**
     * Removes all the occurrences of the specified elementToRemove from the specified
     * array. All subsequent elementsToRemove are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an elementToRemove, no
     * elementsToRemove are removed from the array.
     *
     * @param a
     * @param elementToRemove
     * @return A new array containing the existing elementsToRemove except the
     *         occurrences of the specified elementToRemove.
     */
    public static char[] removeAllOccurrences(final char[] a, final char elementToRemove) {
        if (isEmpty(a)) {
            return EMPTY_CHAR_ARRAY;
        }

        final char[] copy = a.clone();
        int idx = 0;

        for (char element : a) {
            if (element == elementToRemove) {
                continue;
            }

            copy[idx++] = element;
        }

        return idx == copy.length ? copy : copyOfRange(copy, 0, idx);
    }

    /**
     * Removes all the occurrences of the specified elementToRemove from the specified
     * array. All subsequent elementsToRemove are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an elementToRemove, no
     * elementsToRemove are removed from the array.
     *
     * @param a
     * @param elementToRemove
     * @return A new array containing the existing elementsToRemove except the
     *         occurrences of the specified elementToRemove.
     */
    public static byte[] removeAllOccurrences(final byte[] a, final byte elementToRemove) {
        if (isEmpty(a)) {
            return EMPTY_BYTE_ARRAY;
        }

        final byte[] copy = a.clone();
        int idx = 0;

        for (byte element : a) {
            if (element == elementToRemove) {
                continue;
            }

            copy[idx++] = element;
        }

        return idx == copy.length ? copy : copyOfRange(copy, 0, idx);
    }

    /**
     * Removes all the occurrences of the specified elementToRemove from the specified
     * array. All subsequent elementsToRemove are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an elementToRemove, no
     * elementsToRemove are removed from the array.
     *
     * @param a
     * @param elementToRemove
     * @return A new array containing the existing elementsToRemove except the
     *         occurrences of the specified elementToRemove.
     */
    public static short[] removeAllOccurrences(final short[] a, final short elementToRemove) {
        if (isEmpty(a)) {
            return EMPTY_SHORT_ARRAY;
        }

        final short[] copy = a.clone();
        int idx = 0;

        for (short element : a) {
            if (element == elementToRemove) {
                continue;
            }

            copy[idx++] = element;
        }

        return idx == copy.length ? copy : copyOfRange(copy, 0, idx);
    }

    /**
     * Removes all the occurrences of the specified elementToRemove from the specified
     * array. All subsequent elementsToRemove are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an elementToRemove, no
     * elementsToRemove are removed from the array.
     *
     * @param a
     * @param elementToRemove
     * @return A new array containing the existing elementsToRemove except the
     *         occurrences of the specified elementToRemove.
     */
    public static int[] removeAllOccurrences(final int[] a, final int elementToRemove) {
        if (isEmpty(a)) {
            return EMPTY_INT_ARRAY;
        }

        final int[] copy = a.clone();
        int idx = 0;

        for (int element : a) {
            if (element == elementToRemove) {
                continue;
            }

            copy[idx++] = element;
        }

        return idx == copy.length ? copy : copyOfRange(copy, 0, idx);
    }

    /**
     * Removes all the occurrences of the specified elementToRemove from the specified
     * array. All subsequent elementsToRemove are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an elementToRemove, no
     * elementsToRemove are removed from the array.
     *
     * @param a
     * @param elementToRemove
     * @return A new array containing the existing elementsToRemove except the
     *         occurrences of the specified elementToRemove.
     */
    public static long[] removeAllOccurrences(final long[] a, final long elementToRemove) {
        if (isEmpty(a)) {
            return EMPTY_LONG_ARRAY;
        }

        final long[] copy = a.clone();
        int idx = 0;

        for (long element : a) {
            if (element == elementToRemove) {
                continue;
            }

            copy[idx++] = element;
        }

        return idx == copy.length ? copy : copyOfRange(copy, 0, idx);
    }

    /**
     * Removes all the occurrences of the specified elementToRemove from the specified
     * array. All subsequent elementsToRemove are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an elementToRemove, no
     * elementsToRemove are removed from the array.
     *
     * @param a
     * @param elementToRemove
     * @return A new array containing the existing elementsToRemove except the
     *         occurrences of the specified elementToRemove.
     */
    public static float[] removeAllOccurrences(final float[] a, final float elementToRemove) {
        if (isEmpty(a)) {
            return EMPTY_FLOAT_ARRAY;
        }

        final float[] copy = a.clone();
        int idx = 0;

        for (float element : a) {
            if (equals(element, elementToRemove)) {
                continue;
            }

            copy[idx++] = element;
        }

        return idx == copy.length ? copy : copyOfRange(copy, 0, idx);
    }

    /**
     * Removes all the occurrences of the specified elementToRemove from the specified
     * array. All subsequent elementsToRemove are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an elementToRemove, no
     * elementsToRemove are removed from the array.
     *
     * @param a
     * @param elementToRemove
     * @return A new array containing the existing elementsToRemove except the
     *         occurrences of the specified elementToRemove.
     */
    public static double[] removeAllOccurrences(final double[] a, final double elementToRemove) {
        if (isEmpty(a)) {
            return EMPTY_DOUBLE_ARRAY;
        }

        final double[] copy = a.clone();
        int idx = 0;

        for (double element : a) {
            if (equals(element, elementToRemove)) {
                continue;
            }

            copy[idx++] = element;
        }

        return idx == copy.length ? copy : copyOfRange(copy, 0, idx);
    }

    /**
     *
     *
     * @param a
     * @param elementToRemove
     * @return
     */
    public static String[] removeAllOccurrences(final String[] a, final String elementToRemove) {
        if (isEmpty(a)) {
            return EMPTY_STRING_ARRAY;
        }

        final String[] copy = a.clone();
        int idx = 0;

        for (String element : a) {
            if (equals(element, elementToRemove)) {
                continue;
            }

            copy[idx++] = element;
        }

        return idx == copy.length ? copy : copyOfRange(copy, 0, idx);
    }

    /**
     * Removes all the occurrences of the specified elementToRemove from the specified
     * array. All subsequent elementsToRemove are shifted to the left (subtracts one
     * from their indices). If the array doesn't contains such an elementToRemove, no
     * elementsToRemove are removed from the array.
     *
     * @param <T>
     * @param a
     * @param elementToRemove
     * @return A new array containing the existing elementsToRemove except the
     *         occurrences of the specified elementToRemove. Or the specified array if it's {@code null} or empty.
     */
    public static <T> T[] removeAllOccurrences(final T[] a, final T elementToRemove) {
        if (isEmpty(a)) {
            return a;
        }

        final T[] copy = a.clone();
        int idx = 0;

        for (T element : a) {
            if (equals(element, elementToRemove)) {
                continue;
            }

            copy[idx++] = element;
        }

        return idx == copy.length ? copy : copyOfRange(copy, 0, idx);
    }

    /**
     * Removes the all occurrences.
     *
     * @param <T>
     * @param c
     * @param elementToRemove
     * @return
     */
    public static <T> boolean removeAllOccurrences(final Collection<T> c, final T elementToRemove) {
        if (isEmpty(c)) {
            return false;
        }

        return removeAll(c, asSet(elementToRemove));
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @return
     */
    public static boolean[] removeDuplicates(final boolean[] a) {
        if (isEmpty(a)) {
            return EMPTY_BOOLEAN_ARRAY;
        }

        return removeDuplicates(a, 0, a.length);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static boolean[] removeDuplicates(final boolean[] a, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) && fromIndex == 0 && toIndex == 0) {
            return EMPTY_BOOLEAN_ARRAY;
        } else if (toIndex - fromIndex <= 1) {
            return copyOfRange(a, fromIndex, toIndex);
        }

        final Boolean[] b = new Boolean[2];

        for (int i = fromIndex; i < toIndex; i++) {
            if (b[0] == null) {
                b[0] = a[i];
            } else if (b[0].booleanValue() != a[i]) {
                b[1] = a[i];
                break;
            }
        }

        return b[1] == null ? new boolean[] { b[0] } : new boolean[] { b[0], b[1] };
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @return
     */
    public static char[] removeDuplicates(final char[] a) {
        if (isEmpty(a)) {
            return EMPTY_CHAR_ARRAY;
        }

        return removeDuplicates(a, isSorted(a));
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param isSorted
     * @return
     */
    public static char[] removeDuplicates(final char[] a, final boolean isSorted) {
        if (isEmpty(a)) {
            return EMPTY_CHAR_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param isSorted
     * @return
     */
    public static char[] removeDuplicates(final char[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) && fromIndex == 0 && toIndex == 0) {
            return EMPTY_CHAR_ARRAY;
        } else if (toIndex - fromIndex <= 1) {
            return copyOfRange(a, fromIndex, toIndex);
        }

        if (isSorted) {
            final char[] b = (fromIndex == 0 && toIndex == a.length) ? a.clone() : copyOfRange(a, fromIndex, toIndex);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (b[i] == b[i - 1]) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : copyOfRange(b, 0, idx);
        } else {
            final Set<Character> set = newLinkedHashSet(a.length);

            for (int i = fromIndex; i < toIndex; i++) {
                set.add(a[i]);
            }

            if (set.size() == toIndex - fromIndex) {
                return (fromIndex == 0 && toIndex == a.length) ? a.clone() : copyOfRange(a, fromIndex, toIndex);
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
        if (isEmpty(a)) {
            return EMPTY_BYTE_ARRAY;
        }

        return removeDuplicates(a, isSorted(a));
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param isSorted
     * @return
     */
    public static byte[] removeDuplicates(final byte[] a, final boolean isSorted) {
        if (isEmpty(a)) {
            return EMPTY_BYTE_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param isSorted
     * @return
     */
    public static byte[] removeDuplicates(final byte[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) && fromIndex == 0 && toIndex == 0) {
            return EMPTY_BYTE_ARRAY;
        } else if (toIndex - fromIndex <= 1) {
            return copyOfRange(a, fromIndex, toIndex);
        }

        if (isSorted) {
            final byte[] b = (fromIndex == 0 && toIndex == a.length) ? a.clone() : copyOfRange(a, fromIndex, toIndex);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (b[i] == b[i - 1]) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : copyOfRange(b, 0, idx);
        } else {
            final Set<Byte> set = newLinkedHashSet(a.length);

            for (int i = fromIndex; i < toIndex; i++) {
                set.add(a[i]);
            }

            if (set.size() == toIndex - fromIndex) {
                return (fromIndex == 0 && toIndex == a.length) ? a.clone() : copyOfRange(a, fromIndex, toIndex);
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
        if (isEmpty(a)) {
            return EMPTY_SHORT_ARRAY;
        }

        return removeDuplicates(a, isSorted(a));
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param isSorted
     * @return
     */
    public static short[] removeDuplicates(final short[] a, final boolean isSorted) {
        if (isEmpty(a)) {
            return EMPTY_SHORT_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param isSorted
     * @return
     */
    public static short[] removeDuplicates(final short[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) && fromIndex == 0 && toIndex == 0) {
            return EMPTY_SHORT_ARRAY;
        } else if (toIndex - fromIndex <= 1) {
            return copyOfRange(a, fromIndex, toIndex);
        }

        if (isSorted) {
            final short[] b = (fromIndex == 0 && toIndex == a.length) ? a.clone() : copyOfRange(a, fromIndex, toIndex);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (b[i] == b[i - 1]) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : copyOfRange(b, 0, idx);
        } else {
            final Set<Short> set = newLinkedHashSet(a.length);

            for (int i = fromIndex; i < toIndex; i++) {
                set.add(a[i]);
            }

            if (set.size() == toIndex - fromIndex) {
                return (fromIndex == 0 && toIndex == a.length) ? a.clone() : copyOfRange(a, fromIndex, toIndex);
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
        if (isEmpty(a)) {
            return EMPTY_INT_ARRAY;
        }

        return removeDuplicates(a, isSorted(a));
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param isSorted
     * @return
     */
    public static int[] removeDuplicates(final int[] a, final boolean isSorted) {
        if (isEmpty(a)) {
            return EMPTY_INT_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param isSorted
     * @return
     */
    public static int[] removeDuplicates(final int[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) && fromIndex == 0 && toIndex == 0) {
            return EMPTY_INT_ARRAY;
        } else if (toIndex - fromIndex <= 1) {
            return copyOfRange(a, fromIndex, toIndex);
        }

        if (isSorted) {
            final int[] b = (fromIndex == 0 && toIndex == a.length) ? a.clone() : copyOfRange(a, fromIndex, toIndex);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (b[i] == b[i - 1]) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : copyOfRange(b, 0, idx);
        } else {
            final Set<Integer> set = newLinkedHashSet(a.length);

            for (int i = fromIndex; i < toIndex; i++) {
                set.add(a[i]);
            }

            if (set.size() == toIndex - fromIndex) {
                return (fromIndex == 0 && toIndex == a.length) ? a.clone() : copyOfRange(a, fromIndex, toIndex);
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
        if (isEmpty(a)) {
            return EMPTY_LONG_ARRAY;
        }

        return removeDuplicates(a, isSorted(a));
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param isSorted
     * @return
     */
    public static long[] removeDuplicates(final long[] a, final boolean isSorted) {
        if (isEmpty(a)) {
            return EMPTY_LONG_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param isSorted
     * @return
     */
    public static long[] removeDuplicates(final long[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) && fromIndex == 0 && toIndex == 0) {
            return EMPTY_LONG_ARRAY;
        } else if (toIndex - fromIndex <= 1) {
            return copyOfRange(a, fromIndex, toIndex);
        }

        if (isSorted) {
            final long[] b = (fromIndex == 0 && toIndex == a.length) ? a.clone() : copyOfRange(a, fromIndex, toIndex);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (b[i] == b[i - 1]) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : copyOfRange(b, 0, idx);
        } else {
            final Set<Long> set = newLinkedHashSet(a.length);

            for (int i = fromIndex; i < toIndex; i++) {
                set.add(a[i]);
            }

            if (set.size() == toIndex - fromIndex) {
                return (fromIndex == 0 && toIndex == a.length) ? a.clone() : copyOfRange(a, fromIndex, toIndex);
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
        if (isEmpty(a)) {
            return EMPTY_FLOAT_ARRAY;
        }

        return removeDuplicates(a, isSorted(a));
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param isSorted
     * @return
     */
    public static float[] removeDuplicates(final float[] a, final boolean isSorted) {
        if (isEmpty(a)) {
            return EMPTY_FLOAT_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param isSorted
     * @return
     */
    public static float[] removeDuplicates(final float[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) && fromIndex == 0 && toIndex == 0) {
            return EMPTY_FLOAT_ARRAY;
        } else if (toIndex - fromIndex <= 1) {
            return copyOfRange(a, fromIndex, toIndex);
        }

        if (isSorted) {
            final float[] b = (fromIndex == 0 && toIndex == a.length) ? a.clone() : copyOfRange(a, fromIndex, toIndex);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (equals(b[i], b[i - 1])) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : copyOfRange(b, 0, idx);
        } else {

            final Set<Float> set = newLinkedHashSet(a.length);

            for (int i = fromIndex; i < toIndex; i++) {
                set.add(a[i]);
            }

            if (set.size() == toIndex - fromIndex) {
                return (fromIndex == 0 && toIndex == a.length) ? a.clone() : copyOfRange(a, fromIndex, toIndex);
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
        if (isEmpty(a)) {
            return EMPTY_DOUBLE_ARRAY;
        }

        return removeDuplicates(a, isSorted(a));
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param isSorted
     * @return
     */
    public static double[] removeDuplicates(final double[] a, final boolean isSorted) {
        if (isEmpty(a)) {
            return EMPTY_DOUBLE_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param isSorted
     * @return
     */
    public static double[] removeDuplicates(final double[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) && fromIndex == 0 && toIndex == 0) {
            return EMPTY_DOUBLE_ARRAY;
        } else if (toIndex - fromIndex <= 1) {
            return copyOfRange(a, fromIndex, toIndex);
        }

        if (isSorted) {
            final double[] b = (fromIndex == 0 && toIndex == a.length) ? a.clone() : copyOfRange(a, fromIndex, toIndex);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (equals(b[i], b[i - 1])) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : copyOfRange(b, 0, idx);
        } else {
            final Set<Double> set = newLinkedHashSet(a.length);

            for (int i = fromIndex; i < toIndex; i++) {
                set.add(a[i]);
            }

            if (set.size() == toIndex - fromIndex) {
                return (fromIndex == 0 && toIndex == a.length) ? a.clone() : copyOfRange(a, fromIndex, toIndex);
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
     * Removes the duplicates.
     *
     * @param a
     * @return
     */
    public static String[] removeDuplicates(final String[] a) {
        if (isEmpty(a)) {
            return EMPTY_STRING_ARRAY;
        }

        return removeDuplicates(a, isSorted(a));
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param isSorted
     * @return
     */
    public static String[] removeDuplicates(final String[] a, final boolean isSorted) {
        if (isEmpty(a)) {
            return EMPTY_STRING_ARRAY;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Removes the duplicates.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param isSorted
     * @return
     */
    public static String[] removeDuplicates(final String[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) && fromIndex == 0 && toIndex == 0) {
            return EMPTY_STRING_ARRAY;
        } else if (toIndex - fromIndex <= 1) {
            return copyOfRange(a, fromIndex, toIndex);
        }

        if (isSorted) {
            final String[] b = (fromIndex == 0 && toIndex == a.length) ? a.clone() : copyOfRange(a, fromIndex, toIndex);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (equals(b[i], b[i - 1])) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : copyOfRange(b, 0, idx);
        } else {
            final Set<String> set = newLinkedHashSet(a.length);

            for (int i = fromIndex; i < toIndex; i++) {
                set.add(a[i]); //NOSONAR
            }

            if (set.size() == toIndex - fromIndex) {
                return (fromIndex == 0 && toIndex == a.length) ? a.clone() : copyOfRange(a, fromIndex, toIndex);
            } else {
                final String[] result = new String[set.size()];
                int i = 0;

                for (String e : set) {
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
     * removeElements(["a", "b", "a"]) = ["a", "b"]
     * </pre>.
     *
     * @param <T> the component type of the array
     * @param a
     * @return A new array containing the existing elements except the duplicates. Or the specified array if it's {@code null} or empty.
     * @throws NullPointerException if the specified array <code>a</code> is null.
     */
    public static <T> T[] removeDuplicates(final T[] a) {
        if (isEmpty(a)) {
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
     * @return the specified array if it's {@code null} or empty.
     */
    public static <T> T[] removeDuplicates(final T[] a, final boolean isSorted) {
        if (isEmpty(a)) {
            return a;
        }

        return removeDuplicates(a, 0, a.length, isSorted);
    }

    /**
     * Removes the duplicates.
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param isSorted
     * @return the specified array if it's {@code null} or empty.
     */
    public static <T> T[] removeDuplicates(final T[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) && fromIndex == 0 && toIndex == 0) {
            return a;
        } else if (toIndex - fromIndex <= 1) {
            return copyOfRange(a, fromIndex, toIndex);
        }

        if (isSorted) {
            final T[] b = (fromIndex == 0 && toIndex == a.length) ? a.clone() : copyOfRange(a, fromIndex, toIndex);
            int idx = 1;

            for (int i = 1, len = b.length; i < len; i++) {
                if (equals(b[i], b[i - 1])) {
                    continue;
                }

                b[idx++] = b[i];
            }

            return idx == b.length ? b : copyOfRange(b, 0, idx);
        } else {
            final List<T> list = distinct(a, fromIndex, toIndex);
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
        if (isEmpty(c) || c.size() == 1 || c instanceof Set) {
            return false;
        } else if (c.size() == 2) {
            final Iterator<?> iter = c.iterator();
            final Object first = iter.next();

            if (equals(first, iter.next())) {
                iter.remove();
                return true;
            } else {
                return false;
            }
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

    /**
     * Deletes the values from {@code fromIndex} to {@code toIndex}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return a new array
     */
    public static boolean[] deleteRange(final boolean[] a, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return a == null ? EMPTY_BOOLEAN_ARRAY : a.clone();
        }

        final int len = len(a);
        final boolean[] b = new boolean[len - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < len) {
            copy(a, toIndex, b, fromIndex, len - toIndex);
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
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return a == null ? EMPTY_CHAR_ARRAY : a.clone();
        }

        final int len = len(a);
        final char[] b = new char[len - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < len) {
            copy(a, toIndex, b, fromIndex, len - toIndex);
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
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return a == null ? EMPTY_BYTE_ARRAY : a.clone();
        }

        final int len = len(a);
        final byte[] b = new byte[len - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < len) {
            copy(a, toIndex, b, fromIndex, len - toIndex);
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
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return a == null ? EMPTY_SHORT_ARRAY : a.clone();
        }

        final int len = len(a);
        final short[] b = new short[len - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < len) {
            copy(a, toIndex, b, fromIndex, len - toIndex);
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
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return a == null ? EMPTY_INT_ARRAY : a.clone();
        }

        final int len = len(a);
        final int[] b = new int[len - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < len) {
            copy(a, toIndex, b, fromIndex, len - toIndex);
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
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return a == null ? EMPTY_LONG_ARRAY : a.clone();
        }

        final int len = len(a);
        final long[] b = new long[len - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < len) {
            copy(a, toIndex, b, fromIndex, len - toIndex);
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
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return a == null ? EMPTY_FLOAT_ARRAY : a.clone();
        }

        final int len = len(a);
        final float[] b = new float[len - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < len) {
            copy(a, toIndex, b, fromIndex, len - toIndex);
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
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return a == null ? EMPTY_DOUBLE_ARRAY : a.clone();
        }

        final int len = len(a);
        final double[] b = new double[len - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < len) {
            copy(a, toIndex, b, fromIndex, len - toIndex);
        }

        return b;
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException
     */
    public static String[] deleteRange(final String[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return a == null ? EMPTY_STRING_ARRAY : a.clone();
        }

        final int len = len(a);
        final String[] b = new String[len - (toIndex - fromIndex)];

        if (fromIndex > 0) {
            copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < len) {
            copy(a, toIndex, b, fromIndex, len - toIndex);
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
     * @throws IllegalArgumentException if the specified {@code Array} is <code>null</code>.
     */
    public static <T> T[] deleteRange(final T[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        checkArgNotNull(a, "a");

        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return a.clone();
        }

        final int len = len(a);
        final T[] b = Array.newInstance(a.getClass().getComponentType(), len - (toIndex - fromIndex));

        if (fromIndex > 0) {
            copy(a, 0, b, 0, fromIndex);
        }

        if (toIndex < len) {
            copy(a, toIndex, b, fromIndex, len - toIndex);
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
     * @return
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> boolean deleteRange(final List<T> c, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (fromIndex == toIndex) {
            return false;
        }

        final int size = size(c);

        if (c instanceof LinkedList || toIndex - fromIndex <= 3) {
            c.subList(fromIndex, toIndex).clear();
        } else {
            final List<T> tmp = new ArrayList<>(size - (toIndex - fromIndex));

            if (fromIndex > 0) {
                tmp.addAll(c.subList(0, fromIndex));
            }

            if (toIndex < size) {
                tmp.addAll(c.subList(toIndex, size));
            }

            c.clear();
            c.addAll(tmp);
        }

        return true;
    }

    /**
     *
     *
     * @param str
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static String deleteRange(String str, final int fromIndex, final int toIndex) {
        final int len = len(str);

        checkFromToIndex(fromIndex, toIndex, len);

        if (fromIndex == toIndex || fromIndex >= len) {
            return str == null ? Strings.EMPTY_STRING : str;
        } else if (toIndex - fromIndex >= len) {
            return Strings.EMPTY_STRING;
        }

        return Strings.concat(str.substring(0, fromIndex) + str.subSequence(toIndex, len));
    }

    /**
     * Return a new array.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param replacement
     * @return a new array.
     */
    public static boolean[] replaceRange(final boolean[] a, final int fromIndex, final int toIndex, final boolean[] replacement) {
        final int len = len(a);

        checkFromToIndex(fromIndex, toIndex, len);

        if (isEmpty(a)) {
            return isEmpty(replacement) ? EMPTY_BOOLEAN_ARRAY : replacement.clone();
        } else if (isEmpty(replacement)) {
            return deleteRange(a, fromIndex, toIndex);
        }

        final boolean[] result = new boolean[len - (toIndex - fromIndex) + replacement.length];

        if (fromIndex > 0) {
            copy(a, 0, result, 0, fromIndex);
        }

        copy(replacement, 0, result, fromIndex, replacement.length);

        if (toIndex < len) {
            copy(a, toIndex, result, fromIndex + replacement.length, len - toIndex);
        }

        return result;
    }

    /**
     * Return a new array.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param replacement
     * @return a new array.
     */
    public static char[] replaceRange(final char[] a, final int fromIndex, final int toIndex, final char[] replacement) {
        final int len = len(a);

        checkFromToIndex(fromIndex, toIndex, len);

        if (isEmpty(a)) {
            return isEmpty(replacement) ? EMPTY_CHAR_ARRAY : replacement.clone();
        } else if (isEmpty(replacement)) {
            return deleteRange(a, fromIndex, toIndex);
        }

        final char[] result = new char[len - (toIndex - fromIndex) + replacement.length];

        if (fromIndex > 0) {
            copy(a, 0, result, 0, fromIndex);
        }

        copy(replacement, 0, result, fromIndex, replacement.length);

        if (toIndex < len) {
            copy(a, toIndex, result, fromIndex + replacement.length, len - toIndex);
        }

        return result;
    }

    /**
     * Return a new array.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param replacement
     * @return a new array.
     */
    public static byte[] replaceRange(final byte[] a, final int fromIndex, final int toIndex, final byte[] replacement) {
        final int len = len(a);

        checkFromToIndex(fromIndex, toIndex, len);

        if (isEmpty(a)) {
            return isEmpty(replacement) ? EMPTY_BYTE_ARRAY : replacement.clone();
        } else if (isEmpty(replacement)) {
            return deleteRange(a, fromIndex, toIndex);
        }

        final byte[] result = new byte[len - (toIndex - fromIndex) + replacement.length];

        if (fromIndex > 0) {
            copy(a, 0, result, 0, fromIndex);
        }

        copy(replacement, 0, result, fromIndex, replacement.length);

        if (toIndex < len) {
            copy(a, toIndex, result, fromIndex + replacement.length, len - toIndex);
        }

        return result;
    }

    /**
     * Return a new array.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param replacement
     * @return a new array.
     */
    public static short[] replaceRange(final short[] a, final int fromIndex, final int toIndex, final short[] replacement) {
        final int len = len(a);

        checkFromToIndex(fromIndex, toIndex, len);

        if (isEmpty(a)) {
            return isEmpty(replacement) ? EMPTY_SHORT_ARRAY : replacement.clone();
        } else if (isEmpty(replacement)) {
            return deleteRange(a, fromIndex, toIndex);
        }

        final short[] result = new short[len - (toIndex - fromIndex) + replacement.length];

        if (fromIndex > 0) {
            copy(a, 0, result, 0, fromIndex);
        }

        copy(replacement, 0, result, fromIndex, replacement.length);

        if (toIndex < len) {
            copy(a, toIndex, result, fromIndex + replacement.length, len - toIndex);
        }

        return result;
    }

    /**
     * Return a new array.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param replacement
     * @return a new array.
     */
    public static int[] replaceRange(final int[] a, final int fromIndex, final int toIndex, final int[] replacement) {
        final int len = len(a);

        checkFromToIndex(fromIndex, toIndex, len);

        if (isEmpty(a)) {
            return isEmpty(replacement) ? EMPTY_INT_ARRAY : replacement.clone();
        } else if (isEmpty(replacement)) {
            return deleteRange(a, fromIndex, toIndex);
        }

        final int[] result = new int[len - (toIndex - fromIndex) + replacement.length];

        if (fromIndex > 0) {
            copy(a, 0, result, 0, fromIndex);
        }

        copy(replacement, 0, result, fromIndex, replacement.length);

        if (toIndex < len) {
            copy(a, toIndex, result, fromIndex + replacement.length, len - toIndex);
        }

        return result;
    }

    /**
     * Return a new array.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param replacement
     * @return a new array.
     */
    public static long[] replaceRange(final long[] a, final int fromIndex, final int toIndex, final long[] replacement) {
        final int len = len(a);

        checkFromToIndex(fromIndex, toIndex, len);

        if (isEmpty(a)) {
            return isEmpty(replacement) ? EMPTY_LONG_ARRAY : replacement.clone();
        } else if (isEmpty(replacement)) {
            return deleteRange(a, fromIndex, toIndex);
        }

        final long[] result = new long[len - (toIndex - fromIndex) + replacement.length];

        if (fromIndex > 0) {
            copy(a, 0, result, 0, fromIndex);
        }

        copy(replacement, 0, result, fromIndex, replacement.length);

        if (toIndex < len) {
            copy(a, toIndex, result, fromIndex + replacement.length, len - toIndex);
        }

        return result;
    }

    /**
     * Return a new array.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param replacement
     * @return a new array.
     */
    public static float[] replaceRange(final float[] a, final int fromIndex, final int toIndex, final float[] replacement) {
        final int len = len(a);

        checkFromToIndex(fromIndex, toIndex, len);

        if (isEmpty(a)) {
            return isEmpty(replacement) ? EMPTY_FLOAT_ARRAY : replacement.clone();
        } else if (isEmpty(replacement)) {
            return deleteRange(a, fromIndex, toIndex);
        }

        final float[] result = new float[len - (toIndex - fromIndex) + replacement.length];

        if (fromIndex > 0) {
            copy(a, 0, result, 0, fromIndex);
        }

        copy(replacement, 0, result, fromIndex, replacement.length);

        if (toIndex < len) {
            copy(a, toIndex, result, fromIndex + replacement.length, len - toIndex);
        }

        return result;
    }

    /**
     * Return a new array.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param replacement
     * @return a new array.
     */
    public static double[] replaceRange(final double[] a, final int fromIndex, final int toIndex, final double[] replacement) {
        final int len = len(a);

        checkFromToIndex(fromIndex, toIndex, len);

        if (isEmpty(a)) {
            return isEmpty(replacement) ? EMPTY_DOUBLE_ARRAY : replacement.clone();
        } else if (isEmpty(replacement)) {
            return deleteRange(a, fromIndex, toIndex);
        }

        final double[] result = new double[len - (toIndex - fromIndex) + replacement.length];

        if (fromIndex > 0) {
            copy(a, 0, result, 0, fromIndex);
        }

        copy(replacement, 0, result, fromIndex, replacement.length);

        if (toIndex < len) {
            copy(a, toIndex, result, fromIndex + replacement.length, len - toIndex);
        }

        return result;
    }

    /**
     *
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param replacement
     * @return
     */
    public static String[] replaceRange(final String[] a, final int fromIndex, final int toIndex, final String[] replacement) {
        final int len = len(a);

        checkFromToIndex(fromIndex, toIndex, len);

        if (isEmpty(a)) {
            return isEmpty(replacement) ? EMPTY_STRING_ARRAY : replacement.clone();
        } else if (isEmpty(replacement)) {
            return deleteRange(a, fromIndex, toIndex);
        }

        final String[] result = new String[len - (toIndex - fromIndex) + replacement.length];

        if (fromIndex > 0) {
            copy(a, 0, result, 0, fromIndex);
        }

        copy(replacement, 0, result, fromIndex, replacement.length);

        if (toIndex < len) {
            copy(a, toIndex, result, fromIndex + replacement.length, len - toIndex);
        }

        return result;
    }

    /**
     * Return a new array.
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param replacement
     * @return a new array.
     * @throws IllegalArgumentException if the specified {@code Array} is <code>null</code>.
     */
    public static <T> T[] replaceRange(final T[] a, final int fromIndex, final int toIndex, final T[] replacement) throws IllegalArgumentException {
        checkArgNotNull(a, "a");

        final int len = len(a);

        checkFromToIndex(fromIndex, toIndex, len);

        if (isEmpty(a)) {
            return isEmpty(replacement) ? a : replacement.clone();
        } else if (isEmpty(replacement)) {
            return deleteRange(a, fromIndex, toIndex);
        }

        final T[] result = (T[]) newArray(a.getClass().getComponentType(), len - (toIndex - fromIndex) + replacement.length);

        if (fromIndex > 0) {
            copy(a, 0, result, 0, fromIndex);
        }

        copy(replacement, 0, result, fromIndex, replacement.length);

        if (toIndex < len) {
            copy(a, toIndex, result, fromIndex + replacement.length, len - toIndex);
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param replacement
     * @return {@code true} if the specified {@code List} is updated.
     * @throws IllegalArgumentException if the specified <code>c</code> is <code>null</code>.
     */
    public static <T> boolean replaceRange(final List<T> c, final int fromIndex, final int toIndex, final Collection<? extends T> replacement)
            throws IllegalArgumentException {
        checkArgNotNull(c, "c");

        final int size = size(c);

        checkFromToIndex(fromIndex, toIndex, size);

        if (isEmpty(replacement)) {
            if (fromIndex == toIndex) {
                return false;
            }

            return deleteRange(c, fromIndex, toIndex);
        }

        final List<T> endList = toIndex < size ? new ArrayList<>(c.subList(toIndex, size)) : null;

        if (fromIndex < size) {
            deleteRange(c, fromIndex, size);
        }

        c.addAll(replacement);

        if (notEmpty(endList)) {
            c.addAll(endList);
        }

        return true;
    }

    /**
     * Returns a new String.
     *
     * @param str
     * @param fromIndex
     * @param toIndex
     * @param replacement
     * @return
     */
    @SuppressWarnings("deprecation")
    public static String replaceRange(final String str, final int fromIndex, final int toIndex, final String replacement) {
        final int len = len(str);

        checkFromToIndex(fromIndex, toIndex, len);

        if (Strings.isEmpty(str)) {
            return Strings.isEmpty(replacement) ? str : replacement;
        } else if (Strings.isEmpty(replacement)) {
            return deleteRange(str, fromIndex, toIndex);
        }

        final char[] a = InternalUtil.getCharsForReadOnly(str);
        final char[] tmp = replaceRange(a, fromIndex, toIndex, InternalUtil.getCharsForReadOnly(replacement));

        return InternalUtil.newString(tmp, true);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
     */
    public static void moveRange(final boolean[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
        final int len = len(a);
        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len);

        if (fromIndex == toIndex || fromIndex == newPositionStartIndex) {
            return;
        }

        final boolean[] rangeTmp = copyOfRange(a, fromIndex, toIndex);

        // move ahead
        if (newPositionStartIndex < fromIndex) {
            copy(a, newPositionStartIndex, a, toIndex - (fromIndex - newPositionStartIndex), fromIndex - newPositionStartIndex);
        } else {
            copy(a, toIndex, a, fromIndex, newPositionStartIndex - fromIndex);
        }

        copy(rangeTmp, 0, a, newPositionStartIndex, rangeTmp.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
     */
    public static void moveRange(final char[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
        final int len = len(a);
        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len);

        if (fromIndex == toIndex || fromIndex == newPositionStartIndex) {
            return;
        }

        final char[] rangeTmp = copyOfRange(a, fromIndex, toIndex);

        // move ahead
        if (newPositionStartIndex < fromIndex) {
            copy(a, newPositionStartIndex, a, toIndex - (fromIndex - newPositionStartIndex), fromIndex - newPositionStartIndex);
        } else {
            copy(a, toIndex, a, fromIndex, newPositionStartIndex - fromIndex);
        }

        copy(rangeTmp, 0, a, newPositionStartIndex, rangeTmp.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
     */
    public static void moveRange(final byte[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
        final int len = len(a);
        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len);

        if (fromIndex == toIndex || fromIndex == newPositionStartIndex) {
            return;
        }

        final byte[] rangeTmp = copyOfRange(a, fromIndex, toIndex);

        // move ahead
        if (newPositionStartIndex < fromIndex) {
            copy(a, newPositionStartIndex, a, toIndex - (fromIndex - newPositionStartIndex), fromIndex - newPositionStartIndex);
        } else {
            copy(a, toIndex, a, fromIndex, newPositionStartIndex - fromIndex);
        }

        copy(rangeTmp, 0, a, newPositionStartIndex, rangeTmp.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
     */
    public static void moveRange(final short[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
        final int len = len(a);
        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len);

        if (fromIndex == toIndex || fromIndex == newPositionStartIndex) {
            return;
        }

        final short[] rangeTmp = copyOfRange(a, fromIndex, toIndex);

        // move ahead
        if (newPositionStartIndex < fromIndex) {
            copy(a, newPositionStartIndex, a, toIndex - (fromIndex - newPositionStartIndex), fromIndex - newPositionStartIndex);
        } else {
            copy(a, toIndex, a, fromIndex, newPositionStartIndex - fromIndex);
        }

        copy(rangeTmp, 0, a, newPositionStartIndex, rangeTmp.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
     */
    public static void moveRange(final int[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
        final int len = len(a);
        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len);

        if (fromIndex == toIndex || fromIndex == newPositionStartIndex) {
            return;
        }

        final int[] rangeTmp = copyOfRange(a, fromIndex, toIndex);

        // move ahead
        if (newPositionStartIndex < fromIndex) {
            copy(a, newPositionStartIndex, a, toIndex - (fromIndex - newPositionStartIndex), fromIndex - newPositionStartIndex);
        } else {
            copy(a, toIndex, a, fromIndex, newPositionStartIndex - fromIndex);
        }

        copy(rangeTmp, 0, a, newPositionStartIndex, rangeTmp.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
     */
    public static void moveRange(final long[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
        final int len = len(a);
        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len);

        if (fromIndex == toIndex || fromIndex == newPositionStartIndex) {
            return;
        }

        final long[] rangeTmp = copyOfRange(a, fromIndex, toIndex);

        // move ahead
        if (newPositionStartIndex < fromIndex) {
            copy(a, newPositionStartIndex, a, toIndex - (fromIndex - newPositionStartIndex), fromIndex - newPositionStartIndex);
        } else {
            copy(a, toIndex, a, fromIndex, newPositionStartIndex - fromIndex);
        }

        copy(rangeTmp, 0, a, newPositionStartIndex, rangeTmp.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
     */
    public static void moveRange(final float[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
        final int len = len(a);
        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len);

        if (fromIndex == toIndex || fromIndex == newPositionStartIndex) {
            return;
        }

        final float[] rangeTmp = copyOfRange(a, fromIndex, toIndex);

        // move ahead
        if (newPositionStartIndex < fromIndex) {
            copy(a, newPositionStartIndex, a, toIndex - (fromIndex - newPositionStartIndex), fromIndex - newPositionStartIndex);
        } else {
            copy(a, toIndex, a, fromIndex, newPositionStartIndex - fromIndex);
        }

        copy(rangeTmp, 0, a, newPositionStartIndex, rangeTmp.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
     */
    public static void moveRange(final double[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
        final int len = len(a);
        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len);

        if (fromIndex == toIndex || fromIndex == newPositionStartIndex) {
            return;
        }

        final double[] rangeTmp = copyOfRange(a, fromIndex, toIndex);

        // move ahead
        if (newPositionStartIndex < fromIndex) {
            copy(a, newPositionStartIndex, a, toIndex - (fromIndex - newPositionStartIndex), fromIndex - newPositionStartIndex);
        } else {
            copy(a, toIndex, a, fromIndex, newPositionStartIndex - fromIndex);
        }

        copy(rangeTmp, 0, a, newPositionStartIndex, rangeTmp.length);
    }

    /**
     *
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
     */
    public static <T> void moveRange(final T[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
        final int len = len(a);
        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len);

        if (fromIndex == toIndex || fromIndex == newPositionStartIndex) {
            return;
        }

        final T[] rangeTmp = copyOfRange(a, fromIndex, toIndex);

        // move ahead
        if (newPositionStartIndex < fromIndex) {
            copy(a, newPositionStartIndex, a, toIndex - (fromIndex - newPositionStartIndex), fromIndex - newPositionStartIndex);
        } else {
            copy(a, toIndex, a, fromIndex, newPositionStartIndex - fromIndex);
        }

        copy(rangeTmp, 0, a, newPositionStartIndex, rangeTmp.length);
    }

    /**
     *
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param newPositionStartIndex must in the range: [0, list.size() - (toIndex - fromIndex)]
     * @return {@code true} if the specified {@code List} is updated.
     */
    public static <T> boolean moveRange(final List<T> c, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
        final int size = size(c);
        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, size);

        if (fromIndex == toIndex || fromIndex == newPositionStartIndex) {
            return false;
        }

        final T[] tmp = (T[]) c.toArray();

        moveRange(tmp, fromIndex, toIndex, newPositionStartIndex);
        c.clear();
        c.addAll(Arrays.asList(tmp));

        return true;
    }

    /**
     *
     *
     * @param str
     * @param fromIndex
     * @param toIndex
     * @param newPositionStartIndex must in the range: [0, String.length - (toIndex - fromIndex)]
     * @return the specified String if it's {@code null} or empty.
     */
    @SuppressWarnings("deprecation")
    public static String moveRange(final String str, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
        final int len = len(str);
        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len);

        if (fromIndex == toIndex || fromIndex == newPositionStartIndex) {
            return str;
        }

        final char[] a = str.toCharArray();

        moveRange(a, fromIndex, toIndex, newPositionStartIndex);

        return InternalUtil.newString(a, true);
    }

    private static void checkIndexAndStartPositionForMoveRange(final int fromIndex, final int toIndex, final int newPositionStartIndex, final int len) {
        checkFromToIndex(fromIndex, toIndex, len);

        if (newPositionStartIndex < 0 || newPositionStartIndex > (len - (toIndex - fromIndex))) {
            throw new IndexOutOfBoundsException("newPositionStartIndex " + newPositionStartIndex + " is out-of-bounds: [0, " + (len - (toIndex - fromIndex))
                    + "=(array.length - (toIndex - fromIndex))]");
        }
    }
    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static boolean[] copyThenMoveRange(final boolean[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final boolean[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }
    //
    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static char[] copyThenMoveRange(final char[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final char[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }
    //
    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static byte[] copyThenMoveRange(final byte[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final byte[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }
    //
    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static short[] copyThenMoveRange(final short[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final short[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }
    //
    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static int[] copyThenMoveRange(final int[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final int[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }
    //
    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static long[] copyThenMoveRange(final long[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final long[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }
    //
    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static double[] copyThenMoveRange(final double[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final double[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }
    //
    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static <T> T[] copyThenMoveRange(final T[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final T[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }

    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static boolean[] copyThenMoveRange(final boolean[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final boolean[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }
    //
    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static char[] copyThenMoveRange(final char[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final char[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }
    //
    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static byte[] copyThenMoveRange(final byte[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final byte[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }
    //
    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static short[] copyThenMoveRange(final short[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final short[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }
    //
    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static int[] copyThenMoveRange(final int[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final int[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }
    //
    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static long[] copyThenMoveRange(final long[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final long[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }
    //
    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static double[] copyThenMoveRange(final double[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final double[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }
    //
    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static <T> T[] copyThenMoveRange(final T[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final T[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }

    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static boolean[] copyThenMoveRange(final boolean[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final boolean[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }
    //
    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static char[] copyThenMoveRange(final char[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final char[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }
    //
    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static byte[] copyThenMoveRange(final byte[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final byte[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }
    //
    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static short[] copyThenMoveRange(final short[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final short[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }
    //
    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static int[] copyThenMoveRange(final int[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final int[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }
    //
    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static long[] copyThenMoveRange(final long[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final long[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }
    //
    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static double[] copyThenMoveRange(final double[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final double[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }
    //
    //    /**
    //     * Return a new array copy.
    //     *
    //     * @param a
    //     * @param fromIndex
    //     * @param toIndex
    //     * @param newPositionStartIndex must in the range: [0, array.length - (toIndex - fromIndex)]
    //     * @return a new array.
    //     */
    //    public static <T> T[] copyThenMoveRange(final T[] a, final int fromIndex, final int toIndex, final int newPositionStartIndex) {
    //        checkIndexAndStartPositionForMoveRange(fromIndex, toIndex, newPositionStartIndex, len(a));
    //
    //        final T[] copy = isEmpty(a) ? a : a.clone();
    //
    //        moveRange(copy, fromIndex, toIndex, newPositionStartIndex);
    //
    //        return copy;
    //    }

    /**
     *
     *
     * @param <T>
     * @param c
     * @param startInclusive
     * @param endExclusive
     * @return
     */
    public static <T> List<T> skipRange(final Collection<? extends T> c, final int startInclusive, final int endExclusive) {
        return skipRange(c, startInclusive, endExclusive, IntFunctions.ofList());
    }

    /**
     *
     *
     * @param <T>
     * @param <C>
     * @param c
     * @param startInclusive
     * @param endExclusive
     * @param supplier
     * @return
     */
    public static <T, C extends Collection<T>> C skipRange(final Collection<? extends T> c, final int startInclusive, final int endExclusive,
            final IntFunction<C> supplier) {
        final int size = size(c);

        checkFromToIndex(startInclusive, endExclusive, size);

        final C result = supplier.apply(size - (endExclusive - startInclusive));

        if (c instanceof List) {
            final List<T> list = (List<T>) c;

            if (startInclusive > 0) {
                result.addAll(list.subList(0, startInclusive));
            }

            if (endExclusive < size) {
                result.addAll(list.subList(endExclusive, size));
            }
        } else {
            final Iterator<? extends T> iter = c.iterator();

            for (int i = 0; i < startInclusive; i++) {
                result.add(iter.next());
            }

            if (endExclusive < size) {
                int idx = startInclusive;

                while (idx++ < endExclusive) {
                    iter.next();
                }

                while (iter.hasNext()) {
                    result.add(iter.next());
                }
            }
        }

        return result;
    }

    // Primitive/Object array converters
    // ----------------------------------------------------------------------

    /**
     * Checks for duplicates.
     *
     * @param a
     * @return
     */
    public static boolean hasDuplicates(final char[] a) {
        return hasDuplicates(a, false);
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @param isSorted
     * @return
     */
    public static boolean hasDuplicates(final char[] a, final boolean isSorted) {
        if (isEmpty(a)) {
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
     * @return
     */
    static boolean hasDuplicates(final char[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || toIndex - fromIndex < 2) {
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
            final Set<Character> set = newHashSet(toIndex - fromIndex);

            for (int i = fromIndex; i < toIndex; i++) {
                if (!set.add(a[i])) {
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
     * @return
     */
    public static boolean hasDuplicates(final byte[] a) {
        return hasDuplicates(a, false);
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @param isSorted
     * @return
     */
    public static boolean hasDuplicates(final byte[] a, final boolean isSorted) {
        if (isEmpty(a)) {
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
     * @return
     */
    static boolean hasDuplicates(final byte[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || toIndex - fromIndex < 2) {
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
            final Set<Byte> set = newHashSet(toIndex - fromIndex);

            for (int i = fromIndex; i < toIndex; i++) {
                if (!set.add(a[i])) {
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
     * @return
     */
    public static boolean hasDuplicates(final short[] a) {
        return hasDuplicates(a, false);
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @param isSorted
     * @return
     */
    public static boolean hasDuplicates(final short[] a, final boolean isSorted) {
        if (isEmpty(a)) {
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
     * @return
     */
    static boolean hasDuplicates(final short[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || toIndex - fromIndex < 2) {
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
            final Set<Short> set = newHashSet(toIndex - fromIndex);

            for (int i = fromIndex; i < toIndex; i++) {
                if (!set.add(a[i])) {
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
     * @return
     */
    public static boolean hasDuplicates(final int[] a) {
        return hasDuplicates(a, false);
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @param isSorted
     * @return
     */
    public static boolean hasDuplicates(final int[] a, final boolean isSorted) {
        if (isEmpty(a)) {
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
     * @return
     */
    static boolean hasDuplicates(final int[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || toIndex - fromIndex < 2) {
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
            final Set<Integer> set = newHashSet(toIndex - fromIndex);

            for (int i = fromIndex; i < toIndex; i++) {
                if (!set.add(a[i])) {
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
     * @return
     */
    public static boolean hasDuplicates(final long[] a) {
        return hasDuplicates(a, false);
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @param isSorted
     * @return
     */
    public static boolean hasDuplicates(final long[] a, final boolean isSorted) {
        if (isEmpty(a)) {
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
     * @return
     */
    static boolean hasDuplicates(final long[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || toIndex - fromIndex < 2) {
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
            final Set<Long> set = newHashSet(toIndex - fromIndex);

            for (int i = fromIndex; i < toIndex; i++) {
                if (!set.add(a[i])) {
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
     * @return
     */
    public static boolean hasDuplicates(final float[] a) {
        return hasDuplicates(a, false);
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @param isSorted
     * @return
     */
    public static boolean hasDuplicates(final float[] a, final boolean isSorted) {
        if (isEmpty(a)) {
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
     * @return
     */
    static boolean hasDuplicates(final float[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || toIndex - fromIndex < 2) {
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
            final Set<Float> set = newHashSet(toIndex - fromIndex);

            for (int i = fromIndex; i < toIndex; i++) {
                if (!set.add(a[i])) {
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
     * @return
     */
    public static boolean hasDuplicates(final double[] a) {
        return hasDuplicates(a, false);
    }

    /**
     * Checks for duplicates.
     *
     * @param a
     * @param isSorted
     * @return
     */
    public static boolean hasDuplicates(final double[] a, final boolean isSorted) {
        if (isEmpty(a)) {
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
     * @return
     */
    static boolean hasDuplicates(final double[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || toIndex - fromIndex < 2) {
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
            final Set<Double> set = newHashSet(toIndex - fromIndex);

            for (int i = fromIndex; i < toIndex; i++) {
                if (!set.add(a[i])) {
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
     * @return
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
     * @return
     */
    public static <T> boolean hasDuplicates(final T[] a, final boolean isSorted) {
        if (isEmpty(a)) {
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
     * @return
     */
    static <T> boolean hasDuplicates(final T[] a, final int fromIndex, final int toIndex, final boolean isSorted) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || toIndex - fromIndex < 2) {
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
            final Set<Object> set = newHashSet(toIndex - fromIndex);

            for (int i = fromIndex; i < toIndex; i++) {
                if (!set.add(hashKey(a[i]))) {
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
     * @return
     */
    public static boolean hasDuplicates(final Collection<?> c) {
        return hasDuplicates(c, false);
    }

    /**
     * Checks for duplicates.
     *
     * @param c
     * @param isSorted
     * @return
     */
    public static boolean hasDuplicates(final Collection<?> c, final boolean isSorted) {
        if (isEmpty(c) || c.size() == 1) {
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
            final Set<Object> set = newHashSet(c.size());

            for (Object e : c) {
                if (!set.add(hashKey(e))) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     *
     *
     * @param <T>
     * @param c
     * @param objsToKeep
     * @return
     */
    public static <T> boolean retainAll(final Collection<T> c, final Collection<? extends T> objsToKeep) {
        if (isEmpty(c)) {
            return false;
        } else if (isEmpty(objsToKeep)) {
            c.clear();
            return true;
        }

        if (c instanceof HashSet && !(objsToKeep instanceof Set) && (c.size() > 9 || objsToKeep.size() > 9)) {
            return c.retainAll(newHashSet(objsToKeep));
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
        return obj == null || !obj.getClass().isArray() ? obj : Wrapper.of(obj);
    }

    /**
     *
     * @param a
     * @return a long number
     */
    @SafeVarargs
    public static int sum(final char... a) {
        if (isEmpty(a)) {
            return 0;
        }

        return sum(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static int sum(final char[] a, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || fromIndex == toIndex) {
            return 0;
        }

        int sum = 0;

        for (int i = fromIndex; i < toIndex; i++) {
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
        if (isEmpty(a)) {
            return 0;
        }

        return sum(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static int sum(final byte[] a, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || fromIndex == toIndex) {
            return 0;
        }

        int sum = 0;

        for (int i = fromIndex; i < toIndex; i++) {
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
        if (isEmpty(a)) {
            return 0;
        }

        return sum(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static int sum(final short[] a, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || fromIndex == toIndex) {
            return 0;
        }

        int sum = 0;

        for (int i = fromIndex; i < toIndex; i++) {
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
        if (isEmpty(a)) {
            return 0;
        }

        return sum(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static int sum(final int[] a, final int fromIndex, final int toIndex) {
        return Numbers.toIntExact(sumToLong(a, fromIndex, toIndex));
    }

    /**
     *
     * @param a
     * @return a long number
     */
    @SafeVarargs
    public static long sumToLong(final int... a) {
        if (isEmpty(a)) {
            return 0;
        }

        return sumToLong(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static long sumToLong(final int[] a, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || fromIndex == toIndex) {
            return 0;
        }

        long sum = 0;

        for (int i = fromIndex; i < toIndex; i++) {
            sum += a[i];
        }

        return Numbers.toIntExact(sum);
    }

    /**
     *
     * @param a
     * @return a long number
     */
    @SafeVarargs
    public static long sum(final long... a) {
        if (isEmpty(a)) {
            return 0L;
        }

        return sum(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static long sum(final long[] a, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || fromIndex == toIndex) {
            return 0;
        }

        long sum = 0;

        for (int i = fromIndex; i < toIndex; i++) {
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
        if (isEmpty(a)) {
            return 0f;
        }

        return sum(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static float sum(final float[] a, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || fromIndex == toIndex) {
            return 0f;
        }

        final KahanSummation summation = new KahanSummation();

        for (int i = fromIndex; i < toIndex; i++) {
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
        if (isEmpty(a)) {
            return 0d;
        }

        return sum(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static double sum(final double[] a, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || fromIndex == toIndex) {
            return 0d;
        }

        final KahanSummation summation = new KahanSummation();

        for (int i = fromIndex; i < toIndex; i++) {
            summation.add(a[i]);
        }

        return summation.sum();
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param a
     * @return a double number
     */
    @SafeVarargs
    public static double average(final char... a) {
        if (isEmpty(a)) {
            return 0d;
        }

        return average(a, 0, a.length);
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static double average(final char[] a, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || fromIndex == toIndex) {
            return 0d;
        }

        return ((double) sum(a, fromIndex, toIndex)) / (toIndex - fromIndex);
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param a
     * @return a double number
     */
    @SafeVarargs
    public static double average(final byte... a) {
        if (isEmpty(a)) {
            return 0d;
        }

        return average(a, 0, a.length);
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static double average(final byte[] a, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || fromIndex == toIndex) {
            return 0d;
        }

        return ((double) sum(a, fromIndex, toIndex)) / (toIndex - fromIndex);
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param a
     * @return a double number
     */
    @SafeVarargs
    public static double average(final short... a) {
        if (isEmpty(a)) {
            return 0d;
        }

        return average(a, 0, a.length);
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static double average(final short[] a, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || fromIndex == toIndex) {
            return 0d;
        }

        return ((double) sum(a, fromIndex, toIndex)) / (toIndex - fromIndex);
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param a
     * @return a double number
     */
    @SafeVarargs
    public static double average(final int... a) {
        if (isEmpty(a)) {
            return 0d;
        }

        return average(a, 0, a.length);
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static double average(final int[] a, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || fromIndex == toIndex) {
            return 0d;
        }

        long sum = 0;

        for (int i = fromIndex; i < toIndex; i++) {
            sum += a[i];
        }

        return ((double) sum) / (toIndex - fromIndex);
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param a
     * @return a double number
     */
    @SafeVarargs
    public static double average(final long... a) {
        if (isEmpty(a)) {
            return 0d;
        }

        return average(a, 0, a.length);
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static double average(final long[] a, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || fromIndex == toIndex) {
            return 0d;
        }

        return ((double) sum(a, fromIndex, toIndex)) / (toIndex - fromIndex);
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param a
     * @return a double number
     */
    @SafeVarargs
    public static double average(final float... a) {
        if (isEmpty(a)) {
            return 0d;
        }

        return average(a, 0, a.length);
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static double average(final float[] a, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || fromIndex == toIndex) {
            return 0d;
        }

        final KahanSummation summation = new KahanSummation();

        for (int i = fromIndex; i < toIndex; i++) {
            summation.add(a[i]);
        }

        return summation.average().orElseZero();
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param a
     * @return a double number
     */
    @SafeVarargs
    public static double average(final double... a) {
        if (isEmpty(a)) {
            return 0d;
        }

        return average(a, 0, a.length);
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static double average(final double[] a, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (isEmpty(a) || fromIndex == toIndex) {
            return 0d;
        }

        final KahanSummation summation = new KahanSummation();

        for (int i = fromIndex; i < toIndex; i++) {
            summation.add(a[i]);
        }

        return summation.average().orElseZero();
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
        if (isEmpty(a)) {
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

        long sum = 0;

        for (int i = fromIndex; i < toIndex; i++) {
            sum += func.applyAsInt(a[i]);
        }

        return Numbers.toIntExact(sum);
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

        return Numbers.toIntExact(sum);
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Number> int sumInt(final Iterable<? extends T> c) {
        return sumInt(c, Fn.numToInt());
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
    public static <T, E extends Exception> int sumInt(final Iterable<? extends T> c, final Throwables.ToIntFunction<? super T, E> func) throws E {
        return Numbers.toIntExact(sumIntToLong(c, func));
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Number> long sumIntToLong(final Iterable<? extends T> c) {
        return sumIntToLong(c, Fn.numToInt());
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
    public static <T, E extends Exception> long sumIntToLong(final Iterable<? extends T> c, final Throwables.ToIntFunction<? super T, E> func) throws E {
        if (c == null) {
            return 0;
        }

        long sum = 0;

        for (T e : c) {
            sum += func.applyAsInt(e);
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
        if (isEmpty(a)) {
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
     * @param c
     * @return
     */
    public static <T extends Number> long sumLong(final Iterable<? extends T> c) {
        return sumLong(c, Fn.numToLong());
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
    public static <T, E extends Exception> long sumLong(final Iterable<? extends T> c, final Throwables.ToLongFunction<? super T, E> func) throws E {
        if (c == null) {
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
        if (isEmpty(a)) {
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
     * @param c
     * @return
     */
    public static <T extends Number> double sumDouble(final Iterable<? extends T> c) {
        return sumDouble(c, Fn.numToDouble());
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
    public static <T, E extends Exception> double sumDouble(final Iterable<? extends T> c, final Throwables.ToDoubleFunction<? super T, E> func) throws E {
        if (c == null) {
            return 0D;
        }

        final Iterator<? extends T> iter = c.iterator();
        final KahanSummation summation = new KahanSummation();

        while (iter.hasNext()) {
            summation.add(func.applyAsDouble(iter.next()));
        }

        return summation.sum();
    }

    /**
     *
     * @param c
     * @return
     */
    public static BigInteger sumBigInteger(final Iterable<? extends BigInteger> c) {
        return sumBigInteger(c, Fn.identity());
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
    public static <T, E extends Exception> BigInteger sumBigInteger(final Iterable<? extends T> c, final Throwables.Function<? super T, BigInteger, E> func)
            throws E {
        if (c == null) {
            return BigInteger.ZERO;
        }

        final Iterator<? extends T> iter = c.iterator();
        BigInteger result = BigInteger.ZERO;
        BigInteger next = null;

        while (iter.hasNext()) {
            next = func.apply(iter.next());

            if (next != null) {
                result = result.add(next);
            }
        }

        return result;
    }

    /**
     *
     * @param c
     * @return
     */
    public static BigDecimal sumBigDecimal(final Iterable<? extends BigDecimal> c) {
        return sumBigDecimal(c, Fn.identity());
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
    public static <T, E extends Exception> BigDecimal sumBigDecimal(final Iterable<? extends T> c, final Throwables.Function<? super T, BigDecimal, E> func)
            throws E {
        if (c == null) {
            return BigDecimal.ZERO;
        }

        final Iterator<? extends T> iter = c.iterator();
        BigDecimal result = BigDecimal.ZERO;
        BigDecimal next = null;

        while (iter.hasNext()) {
            next = func.apply(iter.next());

            if (next != null) {
                result = result.add(next);
            }
        }

        return result;
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T extends Number> double averageInt(final T[] a) {
        return averageInt(a, Fn.numToInt());
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> double averageInt(final T[] a, final int fromIndex, final int toIndex) {
        return averageInt(a, fromIndex, toIndex, Fn.numToInt());
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> double averageInt(final T[] a, final Throwables.ToIntFunction<? super T, E> func) throws E {
        if (isEmpty(a)) {
            return 0d;
        }

        return averageInt(a, 0, a.length, func);
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
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
    public static <T, E extends Exception> double averageInt(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.ToIntFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return 0d;
        }

        long sum = 0;

        for (int i = fromIndex; i < toIndex; i++) {
            sum += func.applyAsInt(a[i]);
        }

        return ((double) sum) / (toIndex - fromIndex);
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> double averageInt(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        return averageInt(c, fromIndex, toIndex, Fn.numToInt());
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
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
    public static <T, E extends Exception> double averageInt(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToIntFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (fromIndex == toIndex) {
            return 0;
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

        return ((double) sum) / (toIndex - fromIndex);
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Number> double averageInt(final Iterable<? extends T> c) {
        return averageInt(c, Fn.numToInt());
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> double averageInt(final Iterable<? extends T> c, final Throwables.ToIntFunction<? super T, E> func) throws E {
        if (c == null) {
            return 0D;
        }

        long sum = 0;
        long count = 0;

        for (T e : c) {
            sum += func.applyAsInt(e);
            count++;
        }

        return count == 0 ? 0D : ((double) sum) / count;
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param a
     * @return
     */
    public static <T extends Number> double averageLong(final T[] a) {
        return averageLong(a, Fn.numToLong());
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> double averageLong(final T[] a, final int fromIndex, final int toIndex) {
        return averageLong(a, fromIndex, toIndex, Fn.numToLong());
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> double averageLong(final T[] a, final Throwables.ToLongFunction<? super T, E> func) throws E {
        if (isEmpty(a)) {
            return 0d;
        }

        return averageLong(a, 0, a.length, func);
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
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
    public static <T, E extends Exception> double averageLong(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.ToLongFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return 0d;
        }

        return ((double) sumLong(a, fromIndex, toIndex, func)) / (toIndex - fromIndex);
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> double averageLong(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        return averageLong(c, fromIndex, toIndex, Fn.numToLong());
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
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
    public static <T, E extends Exception> double averageLong(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToLongFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (fromIndex == toIndex) {
            return 0d;
        }

        return ((double) sumLong(c, fromIndex, toIndex, func)) / (toIndex - fromIndex);
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T extends Number> double averageLong(final Iterable<? extends T> c) {
        return averageLong(c, Fn.numToLong());
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param func
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> double averageLong(final Iterable<? extends T> c, final Throwables.ToLongFunction<? super T, E> func) throws E {
        if (c == null) {
            return 0D;
        }

        long sum = 0;
        long count = 0;

        for (T e : c) {
            sum += func.applyAsLong(e);
            count++;
        }

        return count == 0 ? 0D : ((double) sum) / count;
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param a
     * @return
     * @see Iterables#averageDouble(Object[])
     */
    public static <T extends Number> double averageDouble(final T[] a) {
        return averageDouble(a, Fn.numToDouble());
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> double averageDouble(final T[] a, final int fromIndex, final int toIndex) {
        return averageDouble(a, fromIndex, toIndex, Fn.numToDouble());
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param func
     * @return
     * @throws E the e
     * @see Iterables#averageDouble(Object[], Throwables.ToDoubleFunction)
     */
    public static <T, E extends Exception> double averageDouble(final T[] a, final Throwables.ToDoubleFunction<? super T, E> func) throws E {
        if (isEmpty(a)) {
            return 0d;
        }

        return averageDouble(a, 0, a.length, func);
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     * @see Iterables#averageDouble(Object[], int, int, Throwables.ToDoubleFunction)
     */
    public static <T, E extends Exception> double averageDouble(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.ToDoubleFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return 0d;
        }

        return Iterables.averageDouble(a, fromIndex, toIndex, func).orElseZero();
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     */
    public static <T extends Number> double averageDouble(final Collection<? extends T> c, final int fromIndex, final int toIndex) {
        return averageDouble(c, fromIndex, toIndex, Fn.numToDouble());
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param func
     * @return
     * @throws E the e
     * @see Iterables#averageDouble(Collection, int, int, Throwables.ToDoubleFunction)
     */
    public static <T, E extends Exception> double averageDouble(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToDoubleFunction<? super T, E> func) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (fromIndex == toIndex) {
            return 0d;
        }

        return Iterables.averageDouble(c, fromIndex, toIndex, func).orElseZero();
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param c
     * @return
     * @see Iterables#averageDouble(Collection)
     */
    public static <T extends Number> double averageDouble(final Iterable<? extends T> c) {
        return averageDouble(c, Fn.numToDouble());
    }

    /**
     * Returns {@code 0} if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param func
     * @return
     * @throws E the e
     * @see Iterables#averageDouble(Collection, Throwables.ToDoubleFunction)
     */
    public static <T, E extends Exception> double averageDouble(final Iterable<? extends T> c, final Throwables.ToDoubleFunction<? super T, E> func) throws E {
        if (c == null) {
            return 0d;
        }

        return Iterables.averageDouble(c, func).orElseZero();
    }

    /**
     *
     * @param c
     * @return
     */
    public static BigDecimal averageBigInteger(final Iterable<? extends BigInteger> c) {
        return averageBigInteger(c, Fn.identity());
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
    public static <T, E extends Exception> BigDecimal averageBigInteger(final Iterable<? extends T> c, final Throwables.Function<? super T, BigInteger, E> func)
            throws E {
        if (c == null) {
            return BigDecimal.ZERO;
        }

        final Iterator<? extends T> iter = c.iterator();
        BigInteger sum = BigInteger.ZERO;
        long cnt = 0;
        BigInteger next = null;

        while (iter.hasNext()) {
            next = func.apply(iter.next());

            if (next != null) {
                sum = sum.add(next);
                cnt++;
            }
        }

        return cnt == 0 ? BigDecimal.ZERO : new BigDecimal(sum).divide(BigDecimal.valueOf(cnt));
    }

    /**
     *
     * @param c
     * @return
     */
    public static BigDecimal averageBigDecimal(final Iterable<? extends BigDecimal> c) {
        return averageBigDecimal(c, Fn.identity());
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
    public static <T, E extends Exception> BigDecimal averageBigDecimal(final Iterable<? extends T> c, final Throwables.Function<? super T, BigDecimal, E> func)
            throws E {
        if (c == null) {
            return BigDecimal.ZERO;
        }

        final Iterator<? extends T> iter = c.iterator();
        BigDecimal sum = BigDecimal.ZERO;
        long cnt = 0;
        BigDecimal next = null;

        while (iter.hasNext()) {
            next = func.apply(iter.next());

            if (next != null) {
                sum = sum.add(next);
                cnt++;
            }
        }

        return cnt == 0 ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(cnt));
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
        return min(a, b, (Comparator<T>) NULL_MAX_COMPARATOR);
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
        if (cmp == null) {
            return ((Comparator<T>) NULL_MAX_COMPARATOR).compare(a, b) <= 0 ? a : b;
        } else {
            return cmp.compare(a, b) <= 0 ? a : b;
        }
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
        return min(a, b, c, (Comparator<T>) NULL_MAX_COMPARATOR);
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
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    @SafeVarargs
    public static char min(final char... a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty"); //NOSONAR

        if (isEmpty(a)) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        return min(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static char min(final char[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        char min = a[fromIndex];
        for (int i = fromIndex + 1; i < toIndex; i++) {
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
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    @SafeVarargs
    public static byte min(final byte... a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return min(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static byte min(final byte[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        byte min = a[fromIndex];
        for (int i = fromIndex + 1; i < toIndex; i++) {
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
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    @SafeVarargs
    public static short min(final short... a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return min(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static short min(final short[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        short min = a[fromIndex];
        for (int i = fromIndex + 1; i < toIndex; i++) {
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
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    @SafeVarargs
    public static int min(final int... a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return min(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static int min(final int[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        int min = a[fromIndex];
        for (int i = fromIndex + 1; i < toIndex; i++) {
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
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    @SafeVarargs
    public static long min(final long... a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return min(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static long min(final long[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        long min = a[fromIndex];
        for (int i = fromIndex + 1; i < toIndex; i++) {
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
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see IEEE754rUtil#min(float[]) IEEE754rUtils for a version of this method
     *      that handles NaN differently
     */
    @SafeVarargs
    public static float min(final float... a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return min(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static float min(final float[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        float min = a[fromIndex];
        for (int i = fromIndex + 1; i < toIndex; i++) {
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
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see IEEE754rUtil#min(double[]) IEEE754rUtils for a version of this
     *      method that handles NaN differently
     */
    @SafeVarargs
    public static double min(final double... a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return min(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static double min(final double[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        double min = a[fromIndex];
        for (int i = fromIndex + 1; i < toIndex; i++) {
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
     * @param a
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see Iterables#min(Comparable[])
     */
    public static <T extends Comparable<? super T>> T min(final T[] a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return min(a, 0, a.length);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static <T extends Comparable<? super T>> T min(final T[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        return min(a, fromIndex, toIndex, (Comparator<T>) NULL_MAX_COMPARATOR);
    }

    /**
     * Returns the minimum element in the array.
     *
     * @param <T>
     * @param a an {@code Array} which must not be null or empty
     * @param cmp
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see Iterables#min(Object[], Comparator)
     */
    public static <T> T min(final T[] a, final Comparator<? super T> cmp) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return min(a, 0, a.length, cmp);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param cmp
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static <T> T min(final T[] a, final int fromIndex, final int toIndex, Comparator<? super T> cmp) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        cmp = cmp == null ? (Comparator<T>) NULL_MAX_COMPARATOR : cmp;

        T candidate = a[fromIndex];
        for (int i = fromIndex + 1; i < toIndex; i++) {
            if (cmp.compare(a[i], candidate) < 0) {
                candidate = a[i];
            }

            if (candidate == null && cmp == NULL_MIN_COMPARATOR) { // NOSONAR
                return null;
            }
        }

        return candidate;
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see Iterables#min(Collection)
     */
    public static <T extends Comparable<? super T>> T min(final Collection<? extends T> c, final int fromIndex, final int toIndex)
            throws IllegalArgumentException {
        checkArgNotEmpty(c, "The spcified collection can not be null or empty");

        return min(c, fromIndex, toIndex, (Comparator<T>) NULL_MAX_COMPARATOR);
    }

    /**
     * Returns the minimum element in the collection.
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param cmp
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static <T> T min(final Collection<? extends T> c, final int fromIndex, final int toIndex, Comparator<? super T> cmp)
            throws IllegalArgumentException {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (isEmpty(c) || toIndex - fromIndex < 1 || fromIndex >= c.size()) {
            throw new IllegalArgumentException("The size of collection can not be null or empty");
        }

        cmp = cmp == null ? (Comparator<T>) NULL_MAX_COMPARATOR : cmp;

        T candidate = null;
        T e = null;

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;
            candidate = list.get(fromIndex);

            for (int i = fromIndex + 1; i < toIndex; i++) {
                e = list.get(i);

                if (cmp.compare(e, candidate) < 0) {
                    candidate = e;
                }

                if (candidate == null && cmp == NULL_MIN_COMPARATOR) { // NOSONAR
                    return null;
                }
            }
        } else {
            final Iterator<? extends T> it = c.iterator();

            for (int i = 0; i < toIndex; i++) {
                if (i < fromIndex) {
                    it.next();
                } else if (i == fromIndex) {
                    candidate = it.next();
                } else {
                    e = it.next();

                    if (cmp.compare(e, candidate) < 0) {
                        candidate = e;
                    }

                    if (candidate == null && cmp == NULL_MIN_COMPARATOR) { // NOSONAR
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
     * @param c
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see Iterables#min(Collection)
     */
    public static <T extends Comparable<? super T>> T min(final Iterable<? extends T> c) throws IllegalArgumentException {
        return min(c, (Comparator<T>) NULL_MAX_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param cmp
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see Iterables#min(Collection, Comparator)
     */
    public static <T> T min(final Iterable<? extends T> c, Comparator<? super T> cmp) throws IllegalArgumentException {
        if (c instanceof Collection) {
            final Collection<T> coll = (Collection<T>) c;
            return min(coll, 0, coll.size(), cmp);
        }

        cmp = cmp == null ? (Comparator<T>) NULL_MAX_COMPARATOR : cmp;
        final Iterator<? extends T> iter = Iterables.iterateNonEmpty(c, "The spcified Collection/Iterable can not be null or empty"); //NOSONAR

        T candidate = iter.next();
        T e = null;

        while (iter.hasNext()) {
            e = iter.next();

            if (cmp.compare(e, candidate) < 0) {
                candidate = e;
            }

            if (candidate == null && cmp == NULL_MIN_COMPARATOR) { // NOSONAR
                return null;
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
        if (isEmpty(a)) {
            return new ArrayList<>();
        }

        cmp = cmp == null ? (Comparator<T>) NULL_MAX_COMPARATOR : cmp;

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
    public static <T extends Comparable<? super T>> List<T> minAll(final Iterable<? extends T> c) {
        return minAll(c, NULL_MAX_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param cmp
     * @return
     */
    public static <T> List<T> minAll(final Iterable<? extends T> c, Comparator<? super T> cmp) {
        if (c == null) {
            return new ArrayList<>();
        }

        return minAll(c.iterator(), cmp);
    }

    /**
     *
     * @param <T>
     * @param iter
     * @return
     * @see Iterables#min(Iterator)
     */
    public static <T extends Comparable<? super T>> List<T> minAll(final Iterator<? extends T> iter) {
        return maxAll(iter, NULL_MAX_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param iter
     * @param cmp
     * @return
     * @see Iterables#min(Iterator, Comparator)
     */
    public static <T> List<T> minAll(final Iterator<? extends T> iter, Comparator<? super T> cmp) {
        cmp = cmp == null ? (Comparator<T>) NULL_MAX_COMPARATOR : cmp;

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
     * Returns the minimum value extracted from the specified array {@code a} by {@code valueExtractor}, or {@code defaultValue} if {@code a} is null or empty.
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param a
     * @param defaultValue
     * @return
     * @throws E
     */
    @Beta
    public static <T, R extends Comparable<? super R>, E extends Exception> R minOrDefaultIfEmpty(final T[] a,
            final Throwables.Function<? super T, ? extends R, E> valueExtractor, final R defaultValue) throws E {
        if (isEmpty(a)) {
            return defaultValue;
        }

        R candicate = valueExtractor.apply(a[0]);
        R next = null;

        for (int i = 1, len = a.length; i < len; i++) {
            next = valueExtractor.apply(a[i]);

            if (candicate == null || (next != null && (next.compareTo(candicate) < 0))) {
                candicate = next;
            }
        }

        return candicate;
    }

    /**
     * Returns the minimum value extracted from the specified collection {@code c} by {@code valueExtractor}, or {@code defaultValue} if {@code c} is null or empty.
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param c
     * @param defaultValue
     * @return
     * @throws E
     */
    public static <T, R extends Comparable<? super R>, E extends Exception> R minOrDefaultIfEmpty(final Iterable<? extends T> c,
            final Throwables.Function<? super T, ? extends R, E> valueExtractor, final R defaultValue) throws E {
        if (c == null) {
            return defaultValue;
        }

        return minOrDefaultIfEmpty(c.iterator(), valueExtractor, defaultValue);
    }

    /**
     * Returns the minimum value extracted from the specified iterator {@code iter} by {@code valueExtractor}, or {@code defaultValue} if {@code iter} is null or empty.
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param iter
     * @param defaultValue
     * @return
     * @throws E
     */
    public static <T, R extends Comparable<? super R>, E extends Exception> R minOrDefaultIfEmpty(final Iterator<? extends T> iter,
            final Throwables.Function<? super T, ? extends R, E> valueExtractor, final R defaultValue) throws E {
        if (iter == null || iter.hasNext() == false) {
            return defaultValue;
        }

        R candicate = valueExtractor.apply(iter.next());
        R next = null;

        while (iter.hasNext()) {
            next = valueExtractor.apply(iter.next());

            if (candicate == null || (next != null && (next.compareTo(candicate) < 0))) {
                candicate = next;
            }
        }

        return candicate;
    }

    /**
     * Returns the minimum {@code int} value extracted from the specified array {@code a} by {@code valueExtractor}, or {@code defaultValue} if {@code a} is null or empty.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param defaultValue
     * @return
     * @throws E
     */
    @Beta
    public static <T, E extends Exception> int minIntOrDefaultIfEmpty(final T[] a, final Throwables.ToIntFunction<? super T, E> valueExtractor,
            final int defaultValue) throws E {
        if (isEmpty(a)) {
            return defaultValue;
        }

        int candicate = valueExtractor.applyAsInt(a[0]);
        int next = 0;

        for (int i = 1, len = a.length; i < len; i++) {
            next = valueExtractor.applyAsInt(a[i]);

            if (next < candicate) {
                candicate = next;
            }
        }

        return candicate;
    }

    /**
     * Returns the minimum {@code int} value extracted from the specified iterable {@code c} by {@code valueExtractor}, or {@code defaultValue} if {@code a} is null or empty.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param defaultValue
     * @return
     * @throws E
     */
    @Beta
    public static <T, E extends Exception> int minIntOrDefaultIfEmpty(final Iterable<? extends T> c,
            final Throwables.ToIntFunction<? super T, E> valueExtractor, final int defaultValue) throws E {
        if (c == null) {
            return defaultValue;
        }

        return minIntOrDefaultIfEmpty(c.iterator(), valueExtractor, defaultValue);
    }

    /**
     * Returns the minimum {@code int} value extracted from the specified iterator {@code iter} by {@code valueExtractor}, or {@code defaultValue} if {@code a} is null or empty.
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param defaultValue
     * @return
     * @throws E
     */
    @Beta
    public static <T, E extends Exception> int minIntOrDefaultIfEmpty(final Iterator<? extends T> iter,
            final Throwables.ToIntFunction<? super T, E> valueExtractor, final int defaultValue) throws E {
        if (iter == null || iter.hasNext() == false) {
            return defaultValue;
        }

        int candicate = valueExtractor.applyAsInt(iter.next());
        int next = 0;

        while (iter.hasNext()) {
            next = valueExtractor.applyAsInt(iter.next());

            if (next < candicate) {
                candicate = next;
            }
        }

        return candicate;
    }

    /**
     * Returns the minimum {@code long} value extracted from the specified array {@code a} by {@code valueExtractor}, or {@code defaultValue} if {@code a} is null or empty.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param defaultValue
     * @return
     * @throws E
     */
    @Beta
    public static <T, E extends Exception> long minLongOrDefaultIfEmpty(final T[] a, final Throwables.ToLongFunction<? super T, E> valueExtractor,
            final long defaultValue) throws E {
        if (isEmpty(a)) {
            return defaultValue;
        }

        long candicate = valueExtractor.applyAsLong(a[0]);
        long next = 0;

        for (int i = 1, len = a.length; i < len; i++) {
            next = valueExtractor.applyAsLong(a[i]);

            if (next < candicate) {
                candicate = next;
            }
        }

        return candicate;
    }

    /**
     * Returns the minimum {@code long} value extracted from the specified iterable {@code c} by {@code valueExtractor}, or {@code defaultValue} if {@code a} is null or empty.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param defaultValue
     * @return
     * @throws E
     */
    @Beta
    public static <T, E extends Exception> long minLongOrDefaultIfEmpty(final Iterable<? extends T> c,
            final Throwables.ToLongFunction<? super T, E> valueExtractor, final long defaultValue) throws E {
        if (c == null) {
            return defaultValue;
        }

        return minLongOrDefaultIfEmpty(c.iterator(), valueExtractor, defaultValue);
    }

    /**
     * Returns the minimum {@code long} value extracted from the specified iterator {@code iter} by {@code valueExtractor}, or {@code defaultValue} if {@code a} is null or empty.
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param defaultValue
     * @return
     * @throws E
     */
    @Beta
    public static <T, E extends Exception> long minLongOrDefaultIfEmpty(final Iterator<? extends T> iter,
            final Throwables.ToLongFunction<? super T, E> valueExtractor, final long defaultValue) throws E {
        if (iter == null || iter.hasNext() == false) {
            return defaultValue;
        }

        long candicate = valueExtractor.applyAsLong(iter.next());
        long next = 0;

        while (iter.hasNext()) {
            next = valueExtractor.applyAsLong(iter.next());

            if (next < candicate) {
                candicate = next;
            }
        }

        return candicate;
    }

    /**
     * Returns the minimum {@code double} value extracted from the specified array {@code a} by {@code valueExtractor}, or {@code defaultValue} if {@code a} is null or empty.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param defaultValue
     * @return
     * @throws E
     */
    @Beta
    public static <T, E extends Exception> double minDoubleOrDefaultIfEmpty(final T[] a, final Throwables.ToDoubleFunction<? super T, E> valueExtractor,
            final double defaultValue) throws E {
        if (isEmpty(a)) {
            return defaultValue;
        }

        double candicate = valueExtractor.applyAsDouble(a[0]);
        double next = 0;

        for (int i = 1, len = a.length; i < len; i++) {
            next = valueExtractor.applyAsDouble(a[i]);

            if (compare(next, candicate) < 0) {
                candicate = next;
            }
        }

        return candicate;
    }

    /**
     * Returns the minimum {@code double} value extracted from the specified iterable {@code c} by {@code valueExtractor}, or {@code defaultValue} if {@code a} is null or empty.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param defaultValue
     * @return
     * @throws E
     */
    @Beta
    public static <T, E extends Exception> double minDoubleOrDefaultIfEmpty(final Iterable<? extends T> c,
            final Throwables.ToDoubleFunction<? super T, E> valueExtractor, final double defaultValue) throws E {
        if (c == null) {
            return defaultValue;
        }

        return minDoubleOrDefaultIfEmpty(c.iterator(), valueExtractor, defaultValue);
    }

    /**
     * Returns the minimum {@code double} value extracted from the specified iterator {@code iter} by {@code valueExtractor}, or {@code defaultValue} if {@code a} is null or empty.
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param defaultValue
     * @return
     * @throws E
     */
    @Beta
    public static <T, E extends Exception> double minDoubleOrDefaultIfEmpty(final Iterator<? extends T> iter,
            final Throwables.ToDoubleFunction<? super T, E> valueExtractor, final double defaultValue) throws E {
        if (iter == null || iter.hasNext() == false) {
            return defaultValue;
        }

        double candicate = valueExtractor.applyAsDouble(iter.next());
        double next = 0;

        while (iter.hasNext()) {
            next = valueExtractor.applyAsDouble(iter.next());

            if (compare(next, candicate) < 0) {
                candicate = next;
            }
        }

        return candicate;
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     * @throws IllegalArgumentException if {@code a} is null or empty.
     * @see Iterables#minMax(Comparable[])
     */
    public static <T extends Comparable<? super T>> Pair<T, T> minMax(final T[] a) throws IllegalArgumentException {
        return minMax(a, NATURAL_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param cmp
     * @return
     * @throws IllegalArgumentException if {@code a} is null or empty.
     * @see Iterables#minMax(Object[], Comparator)
     */
    public static <T> Pair<T, T> minMax(final T[] a, Comparator<? super T> cmp) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        if (a.length == 1) {
            return Pair.of(a[0], a[0]);
        }

        cmp = cmp == null ? (Comparator<T>) NATURAL_COMPARATOR : cmp;

        T min = a[0];
        T max = a[0];
        int cp = 0;

        for (int i = 1, len = a.length; i < len; i++) {
            cp = cmp.compare(a[i], min);

            if (cp < 0) {
                min = a[i];
            } else if ((cp > 0) && (cmp.compare(a[i], max) > 0)) {
                max = a[i];
            }
        }

        return Pair.of(min, max);
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     * @throws IllegalArgumentException if {@code c} is null or empty.
     * @see Iterables#minMax(Iterable)
     */
    public static <T extends Comparable<? super T>> Pair<T, T> minMax(final Iterable<? extends T> c) throws IllegalArgumentException {
        return minMax(c, NATURAL_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param cmp
     * @return
     * @throws IllegalArgumentException if {@code c} is null or empty.
     * @see Iterables#minMax(Iterable, Comparator)
     */
    public static <T> Pair<T, T> minMax(final Iterable<? extends T> c, Comparator<? super T> cmp) throws IllegalArgumentException {
        checkArgNotNull(c, "The spcified iterable can not be null or empty");

        return minMax(c.iterator(), cmp);
    }

    /**
     *
     * @param <T>
     * @param iter
     * @return
     * @throws IllegalArgumentException if {@code iter} is null or empty.
     * @see Iterables#minMax(Iterator)
     */
    public static <T extends Comparable<? super T>> Pair<T, T> minMax(final Iterator<? extends T> iter) throws IllegalArgumentException {
        return minMax(iter, NATURAL_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param iter
     * @param cmp
     * @return
     * @throws IllegalArgumentException if {@code iter} is null or empty.
     * @see Iterables#minMax(Iterator, Comparator)
     */
    public static <T> Pair<T, T> minMax(final Iterator<? extends T> iter, Comparator<? super T> cmp) throws IllegalArgumentException {
        checkArgument(iter != null && iter.hasNext(), "The spcified iterator can not be null or empty");

        cmp = cmp == null ? (Comparator<T>) NATURAL_COMPARATOR : cmp;

        T next = iter.next();
        T min = next;
        T max = next;
        int cp = 0;

        while (iter.hasNext()) {
            next = iter.next();

            cp = cmp.compare(next, min);

            if (cp < 0) {
                min = next;
            } else if ((cp > 0) && (cmp.compare(next, max) > 0)) {
                max = next;
            }
        }

        return Pair.of(min, max);
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
        return max(a, b, (Comparator<T>) NULL_MIN_COMPARATOR);
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
        if (cmp == null) {
            return ((Comparator<T>) NULL_MIN_COMPARATOR).compare(a, b) >= 0 ? a : b;
        } else {
            return cmp.compare(a, b) >= 0 ? a : b;
        }
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
        return max(a, b, c, (Comparator<T>) NULL_MIN_COMPARATOR);
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
     * @param a an {@code Array} which must not be null or empty
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    @SafeVarargs
    public static char max(final char... a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return max(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static char max(final char[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        char max = a[fromIndex];
        for (int i = fromIndex + 1; i < toIndex; i++) {
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
     * @param a an {@code Array} which must not be null or empty
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    @SafeVarargs
    public static byte max(final byte... a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return max(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static byte max(final byte[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        byte max = a[fromIndex];
        for (int i = fromIndex + 1; i < toIndex; i++) {
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
     * @param a an {@code Array} which must not be null or empty
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    @SafeVarargs
    public static short max(final short... a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return max(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static short max(final short[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        short max = a[fromIndex];
        for (int i = fromIndex + 1; i < toIndex; i++) {
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
     * @param a an {@code Array} which must not be null or empty
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    @SafeVarargs
    public static int max(final int... a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return max(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static int max(final int[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        int max = a[fromIndex];
        for (int i = fromIndex + 1; i < toIndex; i++) {
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
     * @param a an {@code Array} which must not be null or empty
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    @SafeVarargs
    public static long max(final long... a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return max(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static long max(final long[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        long max = a[fromIndex];
        for (int i = fromIndex + 1; i < toIndex; i++) {
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
     * @param a an {@code Array} which must not be null or empty
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see IEEE754rUtil#max(float[]) IEEE754rUtils for a version of this method
     *      that handles NaN differently
     */
    @SafeVarargs
    public static float max(final float... a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return max(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static float max(final float[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        float max = a[fromIndex];
        for (int i = fromIndex + 1; i < toIndex; i++) {
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
     * @param a an {@code Array} which must not be null or empty
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see IEEE754rUtil#max(double[]) IEEE754rUtils for a version of this
     *      method that handles NaN differently
     */
    @SafeVarargs
    public static double max(final double... a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return max(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static double max(final double[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        double max = a[fromIndex];
        for (int i = fromIndex + 1; i < toIndex; i++) {
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
     * @param a an {@code Array} which must not be null or empty
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see Iterables#max(Comparable[])
     */
    public static <T extends Comparable<? super T>> T max(final T[] a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return max(a, 0, a.length);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static <T extends Comparable<? super T>> T max(final T[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        return max(a, fromIndex, toIndex, (Comparator<T>) NULL_MIN_COMPARATOR);
    }

    /**
     * Returns the maximum element in the array.
     *
     * @param <T>
     * @param a an {@code Array} which must not be null or empty
     * @param cmp
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see Iterables#max(Object[], Comparator)
     */
    public static <T> T max(final T[] a, final Comparator<? super T> cmp) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return max(a, 0, a.length, cmp);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param cmp
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static <T> T max(final T[] a, final int fromIndex, final int toIndex, Comparator<? super T> cmp) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        cmp = cmp == null ? (Comparator<T>) NULL_MIN_COMPARATOR : cmp;

        T candidate = a[fromIndex];
        for (int i = fromIndex + 1; i < toIndex; i++) {
            if (cmp.compare(a[i], candidate) > 0) {
                candidate = a[i];
            }

            if (candidate == null && cmp == NULL_MAX_COMPARATOR) { // NOSONAR
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
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see Iterables#max(Collection)
     */
    public static <T extends Comparable<? super T>> T max(final Iterable<? extends T> c) throws IllegalArgumentException {
        return max(c, (Comparator<T>) NULL_MIN_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param cmp
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see Iterables#max(Collection, Comparator)
     */
    public static <T> T max(final Iterable<? extends T> c, Comparator<? super T> cmp) throws IllegalArgumentException {
        if (c instanceof Collection) {
            final Collection<T> coll = (Collection<T>) c;
            return max(coll, 0, coll.size(), cmp);
        }

        cmp = cmp == null ? (Comparator<T>) NULL_MIN_COMPARATOR : cmp;
        final Iterator<? extends T> iter = Iterables.iterateNonEmpty(c, "The spcified Collection/Iterable can not be null or empty");

        T candidate = iter.next();
        T e = null;

        while (iter.hasNext()) {
            e = iter.next();

            if (cmp.compare(e, candidate) > 0) {
                candidate = e;
            }

            if (candidate == null && cmp == NULL_MAX_COMPARATOR) { // NOSONAR
                return null;
            }
        }

        return candidate;
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static <T extends Comparable<? super T>> T max(final Collection<? extends T> c, final int fromIndex, final int toIndex)
            throws IllegalArgumentException {
        checkArgNotEmpty(c, "The spcified collection can not be null or empty");

        return max(c, fromIndex, toIndex, (Comparator<T>) NULL_MIN_COMPARATOR);
    }

    /**
     * Returns the maximum element in the collection.
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param cmp
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static <T> T max(final Collection<? extends T> c, final int fromIndex, final int toIndex, Comparator<? super T> cmp)
            throws IllegalArgumentException {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if (isEmpty(c) || toIndex - fromIndex < 1 || fromIndex >= c.size()) {
            throw new IllegalArgumentException("The size of collection can not be null or empty");
        }

        cmp = cmp == null ? (Comparator<T>) NULL_MIN_COMPARATOR : cmp;

        T candidate = null;
        T e = null;

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;
            candidate = list.get(fromIndex);

            for (int i = fromIndex + 1; i < toIndex; i++) {
                e = list.get(i);

                if (cmp.compare(e, candidate) > 0) {
                    candidate = e;
                }

                if (candidate == null && cmp == NULL_MAX_COMPARATOR) { // NOSONAR
                    return null;
                }
            }
        } else {
            final Iterator<? extends T> it = c.iterator();

            for (int i = 0; i < toIndex; i++) {
                if (i < fromIndex) {
                    it.next();
                    continue;
                } else if (i == fromIndex) {
                    candidate = it.next();
                } else {
                    e = it.next();

                    if (cmp.compare(e, candidate) > 0) {
                        candidate = e;
                    }
                }

                if (candidate == null && cmp == NULL_MAX_COMPARATOR) { // NOSONAR
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
        if (isEmpty(a)) {
            return new ArrayList<>();
        }

        cmp = cmp == null ? (Comparator<T>) NULL_MIN_COMPARATOR : cmp;

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
    public static <T extends Comparable<? super T>> List<T> maxAll(final Iterable<? extends T> c) {
        return maxAll(c, NULL_MIN_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param cmp
     * @return
     */
    public static <T> List<T> maxAll(final Iterable<? extends T> c, Comparator<? super T> cmp) {
        if (c == null) {
            return new ArrayList<>();
        }

        return maxAll(c.iterator(), cmp);
    }

    /**
     *
     * @param <T>
     * @param iter
     * @return
     * @see Iterables#maxAll(Iterator)
     */
    public static <T extends Comparable<? super T>> List<T> maxAll(final Iterator<? extends T> iter) {
        return maxAll(iter, NULL_MIN_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param iter
     * @param cmp
     * @return
     * @see Iterables#maxAll(Iterator, Comparator)
     */
    public static <T> List<T> maxAll(final Iterator<? extends T> iter, Comparator<? super T> cmp) {
        cmp = cmp == null ? (Comparator<T>) NULL_MIN_COMPARATOR : cmp;

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
     * Returns the maximum value extracted from the specified array {@code a} by {@code valueExtractor}, or {@code defaultValue} if {@code a} is null or empty.
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param a
     * @param defaultValue
     * @return
     * @throws E
     */
    @Beta
    public static <T, R extends Comparable<? super R>, E extends Exception> R maxOrDefaultIfEmpty(final T[] a,
            final Throwables.Function<? super T, ? extends R, E> valueExtractor, final R defaultValue) throws E {
        if (isEmpty(a)) {
            return defaultValue;
        }

        R candicate = valueExtractor.apply(a[0]);
        R next = null;

        for (int i = 1, len = a.length; i < len; i++) {
            next = valueExtractor.apply(a[i]);

            if (candicate == null || (next != null && (next.compareTo(candicate) > 0))) {
                candicate = next;
            }
        }

        return candicate;
    }

    /**
     * Returns the maximum value extracted from the specified collection {@code c} by {@code valueExtractor}, or {@code defaultValue} if {@code c} is null or empty.
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param c
     * @param defaultValue
     * @return
     * @throws E
     */
    public static <T, R extends Comparable<? super R>, E extends Exception> R maxOrDefaultIfEmpty(final Iterable<? extends T> c,
            final Throwables.Function<? super T, ? extends R, E> valueExtractor, final R defaultValue) throws E {
        if (c == null) {
            return defaultValue;
        }

        return maxOrDefaultIfEmpty(c.iterator(), valueExtractor, defaultValue);
    }

    /**
     * Returns the maximum value extracted from the specified iterator {@code iter} by {@code valueExtractor}, or {@code defaultValue} if {@code iter} is null or empty.
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param iter
     * @param defaultValue
     * @return
     * @throws E
     */
    public static <T, R extends Comparable<? super R>, E extends Exception> R maxOrDefaultIfEmpty(final Iterator<? extends T> iter,
            final Throwables.Function<? super T, ? extends R, E> valueExtractor, final R defaultValue) throws E {
        if (iter == null || iter.hasNext() == false) {
            return defaultValue;
        }

        R candicate = valueExtractor.apply(iter.next());
        R next = null;

        while (iter.hasNext()) {
            next = valueExtractor.apply(iter.next());

            if (candicate == null || (next != null && (next.compareTo(candicate) > 0))) {
                candicate = next;
            }
        }

        return candicate;
    }

    /**
     * Returns the maximum {@code int} value extracted from the specified array {@code a} by {@code valueExtractor}, or {@code defaultValue} if {@code a} is null or empty.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param defaultValue
     * @return
     * @throws E
     */
    @Beta
    public static <T, E extends Exception> int maxIntOrDefaultIfEmpty(final T[] a, final Throwables.ToIntFunction<? super T, E> valueExtractor,
            final int defaultValue) throws E {
        if (isEmpty(a)) {
            return defaultValue;
        }

        int candicate = valueExtractor.applyAsInt(a[0]);
        int next = 0;

        for (int i = 1, len = a.length; i < len; i++) {
            next = valueExtractor.applyAsInt(a[i]);

            if (next > candicate) {
                candicate = next;
            }
        }

        return candicate;
    }

    /**
     * Returns the maximum {@code int} value extracted from the specified iterable {@code c} by {@code valueExtractor}, or {@code defaultValue} if {@code a} is null or empty.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param defaultValue
     * @return
     * @throws E
     */
    @Beta
    public static <T, E extends Exception> int maxIntOrDefaultIfEmpty(final Iterable<? extends T> c,
            final Throwables.ToIntFunction<? super T, E> valueExtractor, final int defaultValue) throws E {
        if (c == null) {
            return defaultValue;
        }

        return maxIntOrDefaultIfEmpty(c.iterator(), valueExtractor, defaultValue);
    }

    /**
     * Returns the maximum {@code int} value extracted from the specified iterator {@code iter} by {@code valueExtractor}, or {@code defaultValue} if {@code a} is null or empty.
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param defaultValue
     * @return
     * @throws E
     */
    @Beta
    public static <T, E extends Exception> int maxIntOrDefaultIfEmpty(final Iterator<? extends T> iter,
            final Throwables.ToIntFunction<? super T, E> valueExtractor, final int defaultValue) throws E {
        if (iter == null || iter.hasNext() == false) {
            return defaultValue;
        }

        int candicate = valueExtractor.applyAsInt(iter.next());
        int next = 0;

        while (iter.hasNext()) {
            next = valueExtractor.applyAsInt(iter.next());

            if (next > candicate) {
                candicate = next;
            }
        }

        return candicate;
    }

    /**
     * Returns the maximum {@code long} value extracted from the specified array {@code a} by {@code valueExtractor}, or {@code defaultValue} if {@code a} is null or empty.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param defaultValue
     * @return
     * @throws E
     */
    @Beta
    public static <T, E extends Exception> long maxLongOrDefaultIfEmpty(final T[] a, final Throwables.ToLongFunction<? super T, E> valueExtractor,
            final long defaultValue) throws E {
        if (isEmpty(a)) {
            return defaultValue;
        }

        long candicate = valueExtractor.applyAsLong(a[0]);
        long next = 0;

        for (int i = 1, len = a.length; i < len; i++) {
            next = valueExtractor.applyAsLong(a[i]);

            if (next > candicate) {
                candicate = next;
            }
        }

        return candicate;
    }

    /**
     * Returns the maximum {@code long} value extracted from the specified iterable {@code c} by {@code valueExtractor}, or {@code defaultValue} if {@code a} is null or empty.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param defaultValue
     * @return
     * @throws E
     */
    @Beta
    public static <T, E extends Exception> long maxLongOrDefaultIfEmpty(final Iterable<? extends T> c,
            final Throwables.ToLongFunction<? super T, E> valueExtractor, final long defaultValue) throws E {
        if (c == null) {
            return defaultValue;
        }

        return maxLongOrDefaultIfEmpty(c.iterator(), valueExtractor, defaultValue);
    }

    /**
     * Returns the maximum {@code long} value extracted from the specified iterator {@code iter} by {@code valueExtractor}, or {@code defaultValue} if {@code a} is null or empty.
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param defaultValue
     * @return
     * @throws E
     */
    @Beta
    public static <T, E extends Exception> long maxLongOrDefaultIfEmpty(final Iterator<? extends T> iter,
            final Throwables.ToLongFunction<? super T, E> valueExtractor, final long defaultValue) throws E {
        if (iter == null || iter.hasNext() == false) {
            return defaultValue;
        }

        long candicate = valueExtractor.applyAsLong(iter.next());
        long next = 0;

        while (iter.hasNext()) {
            next = valueExtractor.applyAsLong(iter.next());

            if (next > candicate) {
                candicate = next;
            }
        }

        return candicate;
    }

    /**
     * Returns the maximum {@code double} value extracted from the specified array {@code a} by {@code valueExtractor}, or {@code defaultValue} if {@code a} is null or empty.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param defaultValue
     * @return
     * @throws E
     */
    @Beta
    public static <T, E extends Exception> double maxDoubleOrDefaultIfEmpty(final T[] a, final Throwables.ToDoubleFunction<? super T, E> valueExtractor,
            final double defaultValue) throws E {
        if (isEmpty(a)) {
            return defaultValue;
        }

        double candicate = valueExtractor.applyAsDouble(a[0]);
        double next = 0;

        for (int i = 1, len = a.length; i < len; i++) {
            next = valueExtractor.applyAsDouble(a[i]);

            if (compare(next, candicate) > 0) {
                candicate = next;
            }
        }

        return candicate;
    }

    /**
     * Returns the maximum {@code double} value extracted from the specified iterable {@code c} by {@code valueExtractor}, or {@code defaultValue} if {@code a} is null or empty.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param defaultValue
     * @return
     * @throws E
     */
    @Beta
    public static <T, E extends Exception> double maxDoubleOrDefaultIfEmpty(final Iterable<? extends T> c,
            final Throwables.ToDoubleFunction<? super T, E> valueExtractor, final double defaultValue) throws E {
        if (c == null) {
            return defaultValue;
        }

        return maxDoubleOrDefaultIfEmpty(c.iterator(), valueExtractor, defaultValue);
    }

    /**
     * Returns the maximum {@code double} value extracted from the specified iterator {@code iter} by {@code valueExtractor}, or {@code defaultValue} if {@code a} is null or empty.
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param defaultValue
     * @return
     * @throws E
     */
    @Beta
    public static <T, E extends Exception> double maxDoubleOrDefaultIfEmpty(final Iterator<? extends T> iter,
            final Throwables.ToDoubleFunction<? super T, E> valueExtractor, final double defaultValue) throws E {
        if (iter == null || iter.hasNext() == false) {
            return defaultValue;
        }

        double candicate = valueExtractor.applyAsDouble(iter.next());
        double next = 0;

        while (iter.hasNext()) {
            next = valueExtractor.applyAsDouble(iter.next());

            if (compare(next, candicate) > 0) {
                candicate = next;
            }
        }

        return candicate;
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
        return (T) median(a, b, c, NATURAL_COMPARATOR);
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
        cmp = cmp == null ? NATURAL_COMPARATOR : cmp;

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
     * @param a an {@code Array} which must not be null or empty
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see #median(int...)
     */
    @SafeVarargs
    public static char median(final char... a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return median(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static char median(final char[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        checkFromToIndex(fromIndex, toIndex, a.length);

        final int len = toIndex - fromIndex;

        if (len == 1) {
            return a[fromIndex];
        } else if (len == 2) {
            return min(a[fromIndex], a[fromIndex + 1]);
        } else if (len == 3) {
            return median(a[fromIndex], a[fromIndex + 1], a[fromIndex + 2]);
        } else {
            return kthLargest(a, fromIndex, toIndex, len / 2 + 1);
        }
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param a an {@code Array} which must not be null or empty
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see #median(int...)
     */
    @SafeVarargs
    public static byte median(final byte... a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return median(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static byte median(final byte[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        checkFromToIndex(fromIndex, toIndex, a.length);

        final int len = toIndex - fromIndex;

        if (len == 1) {
            return a[fromIndex];
        } else if (len == 2) {
            return min(a[fromIndex], a[fromIndex + 1]);
        } else if (len == 3) {
            return median(a[fromIndex], a[fromIndex + 1], a[fromIndex + 2]);
        } else {
            return kthLargest(a, fromIndex, toIndex, len / 2 + 1);
        }
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param a an {@code Array} which must not be null or empty
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see #median(int...)
     */
    @SafeVarargs
    public static short median(final short... a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return median(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static short median(final short[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        checkFromToIndex(fromIndex, toIndex, a.length);

        final int len = toIndex - fromIndex;

        if (len == 1) {
            return a[fromIndex];
        } else if (len == 2) {
            return min(a[fromIndex], a[fromIndex + 1]);
        } else if (len == 3) {
            return median(a[fromIndex], a[fromIndex + 1], a[fromIndex + 2]);
        } else {
            return kthLargest(a, fromIndex, toIndex, len / 2 + 1);
        }
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param a an {@code Array} which must not be null or empty
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see #median(int...)
     */
    @SafeVarargs
    public static int median(final int... a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return median(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static int median(final int[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        checkFromToIndex(fromIndex, toIndex, a.length);

        final int len = toIndex - fromIndex;

        if (len == 1) {
            return a[fromIndex];
        } else if (len == 2) {
            return min(a[fromIndex], a[fromIndex + 1]);
        } else if (len == 3) {
            return median(a[fromIndex], a[fromIndex + 1], a[fromIndex + 2]);
        } else {
            return kthLargest(a, fromIndex, toIndex, len / 2 + 1);
        }
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param a an {@code Array} which must not be null or empty
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see #median(int...)
     */
    @SafeVarargs
    public static long median(final long... a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return median(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static long median(final long[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        checkFromToIndex(fromIndex, toIndex, a.length);

        final int len = toIndex - fromIndex;

        if (len == 1) {
            return a[fromIndex];
        } else if (len == 2) {
            return min(a[fromIndex], a[fromIndex + 1]);
        } else if (len == 3) {
            return median(a[fromIndex], a[fromIndex + 1], a[fromIndex + 2]);
        } else {
            return kthLargest(a, fromIndex, toIndex, len / 2 + 1);
        }
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param a an {@code Array} which must not be null or empty
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see #median(int...)
     */
    @SafeVarargs
    public static float median(final float... a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return median(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static float median(final float[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        checkFromToIndex(fromIndex, toIndex, a.length);

        final int len = toIndex - fromIndex;

        if (len == 1) {
            return a[fromIndex];
        } else if (len == 2) {
            return min(a[fromIndex], a[fromIndex + 1]);
        } else if (len == 3) {
            return median(a[fromIndex], a[fromIndex + 1], a[fromIndex + 2]);
        } else {
            return kthLargest(a, fromIndex, toIndex, len / 2 + 1);
        }
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param a an {@code Array} which must not be null or empty
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see #median(int...)
     */
    @SafeVarargs
    public static double median(final double... a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return median(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static double median(final double[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        checkFromToIndex(fromIndex, toIndex, a.length);

        final int len = toIndex - fromIndex;

        if (len == 1) {
            return a[fromIndex];
        } else if (len == 2) {
            return min(a[fromIndex], a[fromIndex + 1]);
        } else if (len == 3) {
            return median(a[fromIndex], a[fromIndex + 1], a[fromIndex + 2]);
        } else {
            return kthLargest(a, fromIndex, toIndex, len / 2 + 1);
        }
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param <T>
     * @param a an {@code Array} which must not be null or empty
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see #median(int...)
     */
    public static <T extends Comparable<? super T>> T median(final T[] a) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return median(a, 0, a.length);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static <T extends Comparable<? super T>> T median(final T[] a, final int fromIndex, final int toIndex) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        return (T) median(a, fromIndex, toIndex, NATURAL_COMPARATOR);
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param <T>
     * @param a an {@code Array} which must not be null or empty
     * @param cmp
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see #median(int...)
     */
    public static <T> T median(final T[] a, Comparator<? super T> cmp) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return median(a, 0, a.length, cmp);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param cmp
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static <T> T median(final T[] a, final int fromIndex, final int toIndex, Comparator<? super T> cmp) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        checkFromToIndex(fromIndex, toIndex, a.length);

        cmp = cmp == null ? NATURAL_COMPARATOR : cmp;

        final int len = toIndex - fromIndex;

        return kthLargest(a, fromIndex, toIndex, len / 2 + 1, cmp);
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param <T>
     * @param c
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see #median(int...)
     */
    public static <T extends Comparable<? super T>> T median(final Collection<? extends T> c) throws IllegalArgumentException {
        checkArgNotEmpty(c, "The spcified collection can not be null or empty");

        return median(c, 0, c.size());
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static <T extends Comparable<? super T>> T median(final Collection<? extends T> c, final int fromIndex, final int toIndex)
            throws IllegalArgumentException {
        return (T) median(c, fromIndex, toIndex, NATURAL_COMPARATOR);
    }

    /**
     * Returns the <code>length / 2 + 1</code> largest value in the specified array.
     *
     * @param <T>
     * @param c
     * @param cmp
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     * @see #median(int...)
     */
    public static <T> T median(final Collection<? extends T> c, Comparator<? super T> cmp) throws IllegalArgumentException {
        checkArgNotEmpty(c, "The spcified collection can not be null or empty");

        return median(c, 0, c.size(), cmp);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param cmp
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or {@code fromIndex == toIndex}.
     */
    public static <T> T median(final Collection<? extends T> c, final int fromIndex, final int toIndex, Comparator<? super T> cmp)
            throws IllegalArgumentException {
        if (isEmpty(c) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The length of collection can not be null or empty"); //NOSONAR
        }

        checkFromToIndex(fromIndex, toIndex, c.size());

        cmp = cmp == null ? NATURAL_COMPARATOR : cmp;

        final int len = toIndex - fromIndex;

        return kthLargest(c, fromIndex, toIndex, len / 2 + 1, cmp);
    }

    /**
     *
     * @param a
     * @param k
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}, or {@code toIndex - fromIndex < k}.
     */
    public static char kthLargest(final char[] a, final int k) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return kthLargest(a, 0, a.length, k);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param k
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}, or {@code toIndex - fromIndex < k}.
     */
    public static char kthLargest(final char[] a, final int fromIndex, final int toIndex, int k) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);
        checkArgument(k > 0 && k <= toIndex - fromIndex, "'k' (%s) is out of range %s", k, toIndex - fromIndex); //NOSONAR

        final int len = toIndex - fromIndex;

        if (k == 1) {
            return max(a, fromIndex, toIndex);
        } else if (k == len) {
            return min(a, fromIndex, toIndex);
        }

        Queue<Character> queue = null;

        if (k <= len / 2) {
            queue = new PriorityQueue<>(k);

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    if (a[i] > queue.peek().charValue()) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }
        } else {
            k = len - k + 1;

            queue = new PriorityQueue<>(k, (o1, o2) -> o2.compareTo(o1));

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    if (a[i] < queue.peek().charValue()) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }
        }

        return queue.peek();
    }

    /**
     *
     * @param a
     * @param k
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}, or {@code toIndex - fromIndex < k}.
     */
    public static byte kthLargest(final byte[] a, final int k) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return kthLargest(a, 0, a.length, k);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param k
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}, or {@code toIndex - fromIndex < k}.
     */
    public static byte kthLargest(final byte[] a, final int fromIndex, final int toIndex, int k) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);
        checkArgument(k > 0 && k <= toIndex - fromIndex, "'k' (%s) is out of range %s", k, toIndex - fromIndex);

        final int len = toIndex - fromIndex;

        if (k == 1) {
            return max(a, fromIndex, toIndex);
        } else if (k == len) {
            return min(a, fromIndex, toIndex);
        }

        Queue<Byte> queue = null;

        if (k <= len / 2) {
            queue = new PriorityQueue<>(k);

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    if (a[i] > queue.peek().byteValue()) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }
        } else {
            k = len - k + 1;

            queue = new PriorityQueue<>(k, (o1, o2) -> o2.compareTo(o1));

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    if (a[i] < queue.peek().byteValue()) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }
        }

        return queue.peek();
    }

    /**
     *
     * @param a
     * @param k
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}, or {@code toIndex - fromIndex < k}.
     */
    public static short kthLargest(final short[] a, final int k) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return kthLargest(a, 0, a.length, k);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param k
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}, or {@code toIndex - fromIndex < k}.
     */
    public static short kthLargest(final short[] a, final int fromIndex, final int toIndex, int k) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);
        checkArgument(k > 0 && k <= toIndex - fromIndex, "'k' (%s) is out of range %s", k, toIndex - fromIndex);

        final int len = toIndex - fromIndex;

        if (k == 1) {
            return max(a, fromIndex, toIndex);
        } else if (k == len) {
            return min(a, fromIndex, toIndex);
        }

        Queue<Short> queue = null;

        if (k <= len / 2) {
            queue = new PriorityQueue<>(k);

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    if (a[i] > queue.peek().shortValue()) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }
        } else {
            k = len - k + 1;

            queue = new PriorityQueue<>(k, (o1, o2) -> o2.compareTo(o1));

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    if (a[i] < queue.peek().shortValue()) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }
        }

        return queue.peek();
    }

    /**
     *
     * @param a
     * @param k
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}, or {@code toIndex - fromIndex < k}.
     */
    public static int kthLargest(final int[] a, final int k) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return kthLargest(a, 0, a.length, k);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param k
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}, or {@code toIndex - fromIndex < k}.
     */
    public static int kthLargest(final int[] a, final int fromIndex, final int toIndex, int k) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);
        checkArgument(k > 0 && k <= toIndex - fromIndex, "'k' (%s) is out of range %s", k, toIndex - fromIndex);

        final int len = toIndex - fromIndex;

        if (k == 1) {
            return max(a, fromIndex, toIndex);
        } else if (k == len) {
            return min(a, fromIndex, toIndex);
        }

        Queue<Integer> queue = null;

        if (k <= len / 2) {
            queue = new PriorityQueue<>(k);

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    if (a[i] > queue.peek().intValue()) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }
        } else {
            k = len - k + 1;

            queue = new PriorityQueue<>(k, (o1, o2) -> o2.compareTo(o1));

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    if (a[i] < queue.peek().intValue()) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }
        }

        return queue.peek();
    }

    /**
     *
     * @param a
     * @param k
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}, or {@code toIndex - fromIndex < k}.
     */
    public static long kthLargest(final long[] a, final int k) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return kthLargest(a, 0, a.length, k);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param k
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}, or {@code toIndex - fromIndex < k}.
     */
    public static long kthLargest(final long[] a, final int fromIndex, final int toIndex, int k) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);
        checkArgument(k > 0 && k <= toIndex - fromIndex, "'k' (%s) is out of range %s", k, toIndex - fromIndex);

        final int len = toIndex - fromIndex;

        if (k == 1) {
            return max(a, fromIndex, toIndex);
        } else if (k == len) {
            return min(a, fromIndex, toIndex);
        }

        Queue<Long> queue = null;

        if (k <= len / 2) {
            queue = new PriorityQueue<>(k);

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    if (a[i] > queue.peek().longValue()) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }
        } else {
            k = len - k + 1;

            queue = new PriorityQueue<>(k, (o1, o2) -> o2.compareTo(o1));

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    if (a[i] < queue.peek().longValue()) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }
        }

        return queue.peek();
    }

    /**
     *
     * @param a
     * @param k
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}, or {@code toIndex - fromIndex < k}.
     */
    public static float kthLargest(final float[] a, final int k) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return kthLargest(a, 0, a.length, k);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param k
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}, or {@code toIndex - fromIndex < k}.
     */
    public static float kthLargest(final float[] a, final int fromIndex, final int toIndex, int k) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);
        checkArgument(k > 0 && k <= toIndex - fromIndex, "'k' (%s) is out of range %s", k, toIndex - fromIndex);

        final int len = toIndex - fromIndex;

        if (k == 1) {
            return max(a, fromIndex, toIndex);
        } else if (k == len) {
            return min(a, fromIndex, toIndex);
        }

        Queue<Float> queue = null;

        if (k <= len / 2) {
            queue = new PriorityQueue<>(k);

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    if (Float.compare(a[i], queue.peek()) > 0) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }
        } else {
            k = len - k + 1;

            queue = new PriorityQueue<>(k, (o1, o2) -> o2.compareTo(o1));

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    if (Float.compare(a[i], queue.peek()) < 0) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }
        }

        return queue.peek();
    }

    /**
     *
     * @param a
     * @param k
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}, or {@code toIndex - fromIndex < k}.
     */
    public static double kthLargest(final double[] a, final int k) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return kthLargest(a, 0, a.length, k);
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param k
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}, or {@code toIndex - fromIndex < k}.
     */
    public static double kthLargest(final double[] a, final int fromIndex, final int toIndex, int k) throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);
        checkArgument(k > 0 && k <= toIndex - fromIndex, "'k' (%s) is out of range %s", k, toIndex - fromIndex);

        final int len = toIndex - fromIndex;

        if (k == 1) {
            return max(a, fromIndex, toIndex);
        } else if (k == len) {
            return min(a, fromIndex, toIndex);
        }

        Queue<Double> queue = null;

        if (k <= len / 2) {
            queue = new PriorityQueue<>(k);

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    if (Double.compare(a[i], queue.peek()) > 0) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }
        } else {
            k = len - k + 1;

            queue = new PriorityQueue<>(k, (o1, o2) -> o2.compareTo(o1));

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    if (Double.compare(a[i], queue.peek()) < 0) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }
        }

        return queue.peek();
    }

    /**
     *
     * @param <T>
     * @param a
     * @param k
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}, or {@code toIndex - fromIndex < k}.
     */
    public static <T extends Comparable<? super T>> T kthLargest(final T[] a, final int k) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return kthLargest(a, 0, a.length, k);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param k
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}, or {@code toIndex - fromIndex < k}.
     */
    public static <T extends Comparable<? super T>> T kthLargest(final T[] a, final int fromIndex, final int toIndex, final int k)
            throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        return kthLargest(a, fromIndex, toIndex, k, (Comparator<T>) NULL_MIN_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param k
     * @param cmp
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}, or {@code toIndex - fromIndex < k}.
     */
    public static <T> T kthLargest(final T[] a, final int k, final Comparator<? super T> cmp) throws IllegalArgumentException {
        checkArgNotEmpty(a, "The spcified array can not be null or empty");

        return kthLargest(a, 0, a.length, k, cmp);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param k
     * @param cmp
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}, or {@code toIndex - fromIndex < k}.
     */
    public static <T> T kthLargest(final T[] a, final int fromIndex, final int toIndex, int k, final Comparator<? super T> cmp)
            throws IllegalArgumentException {
        if (isEmpty(a) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The spcified array can not be null or empty");
        }

        checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);
        checkArgument(k > 0 && k <= toIndex - fromIndex, "'k' (%s) is out of range %s", k, toIndex - fromIndex);

        final Comparator<? super T> comparator = cmp == null ? NULL_MIN_COMPARATOR : cmp;
        final int len = toIndex - fromIndex;

        if (k == 1) {
            return max(a, fromIndex, toIndex, comparator);
        } else if (k == len) {
            return min(a, fromIndex, toIndex, comparator);
        }

        Queue<T> queue = null;

        if (k <= len / 2) {
            queue = new PriorityQueue<>(k, comparator);

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    if (comparator.compare(a[i], queue.peek()) > 0) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }
        } else {
            k = len - k + 1;

            queue = new PriorityQueue<>(k, (o1, o2) -> comparator.compare(o2, o1));

            for (int i = fromIndex; i < toIndex; i++) {
                if (queue.size() < k) {
                    queue.add(a[i]);
                } else {
                    if (comparator.compare(a[i], queue.peek()) < 0) {
                        queue.remove();
                        queue.add(a[i]);
                    }
                }
            }
        }

        return queue.peek();
    }

    /**
     *
     * @param <T>
     * @param c
     * @param k
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}, or {@code toIndex - fromIndex < k}.
     */
    public static <T extends Comparable<? super T>> T kthLargest(final Collection<? extends T> c, final int k) throws IllegalArgumentException {
        checkArgNotEmpty(c, "The spcified collection can not be null or empty");

        return kthLargest(c, 0, c.size(), k);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param k
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}, or {@code toIndex - fromIndex < k}.
     */
    public static <T extends Comparable<? super T>> T kthLargest(final Collection<? extends T> c, final int fromIndex, final int toIndex, final int k)
            throws IllegalArgumentException {
        if (isEmpty(c) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The length of collection can not be null or empty");
        }

        return kthLargest(c, fromIndex, toIndex, k, (Comparator<T>) NULL_MIN_COMPARATOR);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param k
     * @param cmp
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}, or {@code toIndex - fromIndex < k}.
     */
    public static <T> T kthLargest(final Collection<? extends T> c, final int k, final Comparator<? super T> cmp) throws IllegalArgumentException {
        checkArgNotEmpty(c, "The spcified collection can not be null or empty");

        return kthLargest(c, 0, c.size(), k, cmp);
    }

    /**
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param k
     * @param cmp
     * @return
     * @throws IllegalArgumentException if the specified {@code Array/Collection} is {@code null} or empty, or its length/size is less than {@code k}, or {@code toIndex - fromIndex < k}.
     */
    public static <T> T kthLargest(final Collection<? extends T> c, final int fromIndex, final int toIndex, int k, final Comparator<? super T> cmp)
            throws IllegalArgumentException {
        if (isEmpty(c) || toIndex - fromIndex < 1) {
            throw new IllegalArgumentException("The length of collection can not be null or empty");
        }

        checkFromToIndex(fromIndex, toIndex, c == null ? 0 : c.size());
        checkArgument(k > 0 && k <= toIndex - fromIndex, "'k' (%s) is out of range %s", k, toIndex - fromIndex);

        final Comparator<? super T> comparator = cmp == null ? NULL_MIN_COMPARATOR : cmp;
        final int len = toIndex - fromIndex;

        if (k == 1) {
            return max(c, fromIndex, toIndex, comparator);
        } else if (k == len) {
            return min(c, fromIndex, toIndex, comparator);
        }

        final Iterator<? extends T> iter = c.iterator();
        Queue<T> queue = null;

        if (k <= len / 2) {
            queue = new PriorityQueue<>(k);
            int cursor = 0;

            while (cursor < fromIndex && iter.hasNext()) {
                cursor++;
                iter.next();
            }

            T e = null;
            while (cursor < toIndex && iter.hasNext()) {
                e = iter.next();

                if (queue.size() < k) {
                    queue.add(e);
                } else {
                    if (comparator.compare(e, queue.peek()) > 0) {
                        queue.remove();
                        queue.add(e);
                    }
                }

                cursor++;
            }
        } else {
            k = len - k + 1;

            queue = new PriorityQueue<>(k, (o1, o2) -> comparator.compare(o2, o1));

            int cursor = 0;

            while (cursor < fromIndex && iter.hasNext()) {
                cursor++;
                iter.next();
            }

            T e = null;
            while (cursor < toIndex && iter.hasNext()) {
                e = iter.next();

                if (queue.size() < k) {
                    queue.add(e);
                } else {
                    if (comparator.compare(e, queue.peek()) < 0) {
                        queue.remove();
                        queue.add(e);
                    }
                }

                cursor++;
            }
        }

        return queue.peek();
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

        final Comparator<? super Short> comparator = cmp == null ? NULL_MIN_COMPARATOR : cmp;
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

        final Comparator<? super Integer> comparator = cmp == null ? NULL_MIN_COMPARATOR : cmp;
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

        final Comparator<? super Long> comparator = cmp == null ? NULL_MIN_COMPARATOR : cmp;
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

        final Comparator<? super Float> comparator = cmp == null ? NULL_MIN_COMPARATOR : cmp;
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

        final Comparator<? super Double> comparator = cmp == null ? NULL_MIN_COMPARATOR : cmp;
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
        return top(a, n, NULL_MIN_COMPARATOR);
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
        return top(a, fromIndex, toIndex, n, NULL_MIN_COMPARATOR);
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
    @SuppressWarnings("deprecation")
    public static <T> List<T> top(final T[] a, final int fromIndex, final int toIndex, final int n, final Comparator<? super T> cmp) {
        checkArgNotNegative(n, "n");

        if (n == 0) {
            return new ArrayList<>();
        } else if (n >= toIndex - fromIndex) {
            return toList(a, fromIndex, toIndex);
        }

        final Comparator<? super T> comparator = cmp == null ? NULL_MIN_COMPARATOR : cmp;
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

        return InternalUtil.createList((T[]) heap.toArray(EMPTY_OBJECT_ARRAY));
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
    @SuppressWarnings("deprecation")
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

        final Comparator<? super T> comparator = cmp == null ? NULL_MIN_COMPARATOR : cmp;
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

        return InternalUtil.createList((T[]) heap.toArray(EMPTY_OBJECT_ARRAY));
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
        return top(a, n, NULL_MIN_COMPARATOR, keepEncounterOrder);
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
        return top(a, fromIndex, toIndex, n, NULL_MIN_COMPARATOR, keepEncounterOrder);
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

        final Comparator<Indexed<T>> comparator = createComparatorForIndexedObject(cmp);

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

        sort(arrayOfIndexed, (Comparator<Indexed<T>>) (o1, o2) -> o1.index() - o2.index());

        final List<T> res = new ArrayList<>(arrayOfIndexed.length);

        for (Indexed<T> element : arrayOfIndexed) {
            res.add(element.value());
        }

        return res;
    }

    @SuppressWarnings("rawtypes")
    private static <T> Comparator<Indexed<T>> createComparatorForIndexedObject(final Comparator<? super T> cmp) {
        Comparator<Indexed<T>> pairCmp = null;

        if (cmp != null) {
            final Comparator<? super T> cmp2 = cmp;
            pairCmp = (a, b) -> cmp2.compare(a.value(), b.value());
        } else {
            final Comparator<Indexed<Comparable>> tmp = (a, b) -> compare(a.value(), b.value());
            pairCmp = (Comparator) tmp;
        }

        return pairCmp;
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
        return top(c, n, NULL_MIN_COMPARATOR, keepEncounterOrder);
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
     *
     * @param <T>
     * @param c
     * @param fromIndex
     * @param toIndex
     * @param n
     * @param keepEncounterOrder
     * @return
     */
    public static <T extends Comparable<? super T>> List<T> top(final Collection<? extends T> c, final int fromIndex, final int toIndex, final int n,
            final boolean keepEncounterOrder) {
        return top(c, fromIndex, toIndex, n, NULL_MIN_COMPARATOR, keepEncounterOrder);
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

        final Comparator<Indexed<T>> comparator = createComparatorForIndexedObject(cmp);

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

        sort(arrayOfIndexed, (Comparator<Indexed<T>>) (o1, o2) -> o1.index() - o2.index());

        final List<T> res = new ArrayList<>(arrayOfIndexed.length);

        for (Indexed<T> element : arrayOfIndexed) {
            res.add(element.value());
        }

        return res;
    }

    /**
     * Returns the elements at: <code>Percentage</code> * length of the specified array.
     *
     * @param sortedArray
     * @return
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is {@code null} or empty.
     */
    public static Map<Percentage, Character> percentiles(final char[] sortedArray) throws IllegalArgumentException {
        checkArgNotEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty"); //NOSONAR

        final int len = sortedArray.length;
        final Map<Percentage, Character> m = newLinkedHashMap(Percentage.values().length);

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
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is {@code null} or empty.
     */
    public static Map<Percentage, Byte> percentiles(final byte[] sortedArray) throws IllegalArgumentException {
        checkArgNotEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty");

        final int len = sortedArray.length;
        final Map<Percentage, Byte> m = newLinkedHashMap(Percentage.values().length);

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
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is {@code null} or empty.
     */
    public static Map<Percentage, Short> percentiles(final short[] sortedArray) throws IllegalArgumentException {
        checkArgNotEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty");

        final int len = sortedArray.length;
        final Map<Percentage, Short> m = newLinkedHashMap(Percentage.values().length);

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
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is {@code null} or empty.
     */
    public static Map<Percentage, Integer> percentiles(final int[] sortedArray) throws IllegalArgumentException {
        checkArgNotEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty");

        final int len = sortedArray.length;
        final Map<Percentage, Integer> m = newLinkedHashMap(Percentage.values().length);

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
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is {@code null} or empty.
     */
    public static Map<Percentage, Long> percentiles(final long[] sortedArray) throws IllegalArgumentException {
        checkArgNotEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty");

        final int len = sortedArray.length;
        final Map<Percentage, Long> m = newLinkedHashMap(Percentage.values().length);

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
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is {@code null} or empty.
     */
    public static Map<Percentage, Float> percentiles(final float[] sortedArray) throws IllegalArgumentException {
        checkArgNotEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty");

        final int len = sortedArray.length;
        final Map<Percentage, Float> m = newLinkedHashMap(Percentage.values().length);

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
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is {@code null} or empty.
     */
    public static Map<Percentage, Double> percentiles(final double[] sortedArray) throws IllegalArgumentException {
        checkArgNotEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty");

        final int len = sortedArray.length;
        final Map<Percentage, Double> m = newLinkedHashMap(Percentage.values().length);

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
     * @throws IllegalArgumentException if the specified <code>sortedArray</code> is {@code null} or empty.
     */
    public static <T> Map<Percentage, T> percentiles(final T[] sortedArray) throws IllegalArgumentException {
        checkArgNotEmpty(sortedArray, "The spcified 'sortedArray' can not be null or empty");

        final int len = sortedArray.length;
        final Map<Percentage, T> m = newLinkedHashMap(Percentage.values().length);

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
     * @throws IllegalArgumentException if the specified <code>sortedList</code> is {@code null} or empty.
     */
    public static <T> Map<Percentage, T> percentiles(final List<T> sortedList) throws IllegalArgumentException {
        checkArgNotEmpty(sortedList, "The spcified 'sortedList' can not be null or empty");

        final int size = sortedList.size();
        final Map<Percentage, T> m = newLinkedHashMap(Percentage.values().length);

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
        checkArgument(step != 0, "The input parameter 'step' can not be zero"); //NOSONAR

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

        if (isEmpty(a)) {
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

        if (isEmpty(a) || fromIndex == toIndex) {
            return;
        }

        if (fromIndex <= toIndex) {
            for (int i = fromIndex; i < toIndex; i++) {
                action.accept(a[i]);
            }
        } else {
            for (int i = min(a.length - 1, fromIndex); i > toIndex; i--) {
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
    public static <T, E extends Exception> void forEach(final Iterable<? extends T> c, final Throwables.Consumer<? super T, E> action) throws E {
        checkArgNotNull(action);

        if (c == null) {
            return;
        }

        for (T e : c) {
            action.accept(e);
        }
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEach(final Iterator<? extends T> iter, final Throwables.Consumer<? super T, E> action) throws E {
        checkArgNotNull(action);

        if (iter == null) {
            return;
        }

        while (iter.hasNext()) {
            action.accept(iter.next());
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

        if ((isEmpty(c) && fromIndex == 0 && toIndex == 0) || fromIndex == toIndex) {
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
            if (fromIndex <= toIndex) {
                final Iterator<? extends T> iter = c.iterator();
                int idx = 0;

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
                final Iterator<T> descendingIterator = getDescendingIteratorIfPossible(c);

                if (descendingIterator != null) {
                    int idx = c.size() - 1;

                    while (idx > fromIndex && descendingIterator.hasNext()) {
                        descendingIterator.next();
                        idx--;
                    }

                    while (descendingIterator.hasNext()) {
                        action.accept(descendingIterator.next());

                        if (--idx <= toIndex) {
                            break;
                        }
                    }
                } else {
                    final Iterator<? extends T> iter = c.iterator();
                    int idx = 0;

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

        if (isEmpty(map)) {
            return;
        }

        forEach(map.entrySet(), action);
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
    public static <K, V, E extends Exception> void forEach(final Map<K, V> map, final Throwables.BiConsumer<? super K, ? super V, E> action) throws E {
        checkArgNotNull(action);

        if (isEmpty(map)) {
            return;
        }

        for (Map.Entry<K, V> entry : map.entrySet()) {
            action.accept(entry.getKey(), entry.getValue());
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
    public static <T, E extends Exception> void forEachIndexed(final T[] a, final Throwables.IndexedConsumer<? super T, E> action) throws E {
        checkArgNotNull(action);

        if (isEmpty(a)) {
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

        if (isEmpty(a) || fromIndex == toIndex) {
            return;
        }

        if (fromIndex <= toIndex) {
            for (int i = fromIndex; i < toIndex; i++) {
                action.accept(i, a[i]);
            }
        } else {
            for (int i = min(a.length - 1, fromIndex); i > toIndex; i--) {
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
    public static <T, E extends Exception> void forEachIndexed(final Iterable<? extends T> c, final Throwables.IndexedConsumer<? super T, E> action) throws E {
        checkArgNotNull(action);

        if (c == null) {
            return;
        }

        int idx = 0;

        for (T e : c) {
            action.accept(idx++, e);
        }
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachIndexed(final Iterator<? extends T> iter, final Throwables.IndexedConsumer<? super T, E> action)
            throws E {
        checkArgNotNull(action);

        if (iter == null) {
            return;
        }

        int idx = 0;

        while (iter.hasNext()) {
            action.accept(idx++, iter.next());
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

        if ((isEmpty(c) && fromIndex == 0 && toIndex == 0) || fromIndex == toIndex) {
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
            if (fromIndex < toIndex) {
                final Iterator<? extends T> iter = c.iterator();
                int idx = 0;

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
                final Iterator<T> descendingIterator = getDescendingIteratorIfPossible(c);

                if (descendingIterator != null) {
                    int idx = c.size() - 1;

                    while (idx > fromIndex && descendingIterator.hasNext()) {
                        descendingIterator.next();
                        idx--;
                    }

                    while (descendingIterator.hasNext()) {
                        action.accept(idx, descendingIterator.next());

                        if (--idx <= toIndex) {
                            break;
                        }
                    }
                } else {
                    final Iterator<? extends T> iter = c.iterator();
                    int idx = 0;

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

        if (isEmpty(map)) {
            return;
        }

        forEachIndexed(map.entrySet(), action);
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
    public static <K, V, E extends Exception> void forEachIndexed(final Map<K, V> map, final Throwables.IndexedBiConsumer<? super K, ? super V, E> action)
            throws E {
        checkArgNotNull(action);

        if (isEmpty(map)) {
            return;
        }

        int idx = 0;

        for (Map.Entry<K, V> entry : map.entrySet()) {
            action.accept(idx++, entry.getKey(), entry.getValue());
        }
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
            final Throwables.Function<? super T, ? extends Iterable<U>, E> flatMapper, final Throwables.BiConsumer<? super T, ? super U, E2> action)
            throws E, E2 {
        checkArgNotNull(flatMapper);
        checkArgNotNull(action);

        if (isEmpty(a)) {
            return;
        }

        for (T t : a) {
            final Iterable<U> c2 = flatMapper.apply(t);

            if (c2 != null) {
                for (U u : c2) {
                    action.accept(t, u);
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
    public static <T, U, E extends Exception, E2 extends Exception> void forEach(final Iterable<? extends T> c,
            final Throwables.Function<? super T, ? extends Iterable<U>, E> flatMapper, final Throwables.BiConsumer<? super T, ? super U, E2> action)
            throws E, E2 {
        checkArgNotNull(flatMapper);
        checkArgNotNull(action);

        if (c == null) {
            return;
        }

        for (T t : c) {
            final Iterable<U> c2 = flatMapper.apply(t);

            if (c2 != null) {
                for (U u : c2) {
                    action.accept(t, u);
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
     * @param iter
     * @param flatMapper
     * @param action
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, U, E extends Exception, E2 extends Exception> void forEach(final Iterator<? extends T> iter,
            final Throwables.Function<? super T, ? extends Iterable<U>, E> flatMapper, final Throwables.BiConsumer<? super T, ? super U, E2> action)
            throws E, E2 {
        checkArgNotNull(flatMapper);
        checkArgNotNull(action);

        if (iter == null) {
            return;
        }

        T t = null;

        while (iter.hasNext()) {
            t = iter.next();

            final Iterable<U> c2 = flatMapper.apply(t);

            if (c2 != null) {
                for (U u : c2) {
                    action.accept(t, u);
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
            final Throwables.Function<? super T, ? extends Iterable<T2>, E> flatMapper,
            final Throwables.Function<? super T2, ? extends Iterable<T3>, E2> flatMapper2,
            final Throwables.TriConsumer<? super T, ? super T2, ? super T3, E3> action) throws E, E2, E3 {
        checkArgNotNull(flatMapper);
        checkArgNotNull(flatMapper2);
        checkArgNotNull(action);

        if (isEmpty(a)) {
            return;
        }

        for (T t : a) {
            final Iterable<T2> c2 = flatMapper.apply(t);

            if (c2 != null) {
                for (T2 t2 : c2) {
                    final Iterable<T3> c3 = flatMapper2.apply(t2);

                    if (c3 != null) {
                        for (T3 t3 : c3) {
                            action.accept(t, t2, t3);
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
    public static <T, T2, T3, E extends Exception, E2 extends Exception, E3 extends Exception> void forEach(final Iterable<? extends T> c,
            final Throwables.Function<? super T, ? extends Iterable<T2>, E> flatMapper,
            final Throwables.Function<? super T2, ? extends Iterable<T3>, E2> flatMapper2,
            final Throwables.TriConsumer<? super T, ? super T2, ? super T3, E3> action) throws E, E2, E3 {
        checkArgNotNull(flatMapper);
        checkArgNotNull(flatMapper2);
        checkArgNotNull(action);

        if (c == null) {
            return;
        }

        for (T t : c) {
            final Iterable<T2> c2 = flatMapper.apply(t);

            if (c2 != null) {
                for (T2 t2 : c2) {
                    final Iterable<T3> c3 = flatMapper2.apply(t2);

                    if (c3 != null) {
                        for (T3 t3 : c3) {
                            action.accept(t, t2, t3);
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
     * @param iter
     * @param flatMapper
     * @param flatMapper2
     * @param action
     * @throws E the e
     * @throws E2 the e2
     * @throws E3 the e3
     */
    public static <T, T2, T3, E extends Exception, E2 extends Exception, E3 extends Exception> void forEach(final Iterator<? extends T> iter,
            final Throwables.Function<? super T, ? extends Iterable<T2>, E> flatMapper,
            final Throwables.Function<? super T2, ? extends Iterable<T3>, E2> flatMapper2,
            final Throwables.TriConsumer<? super T, ? super T2, ? super T3, E3> action) throws E, E2, E3 {
        checkArgNotNull(flatMapper);
        checkArgNotNull(flatMapper2);
        checkArgNotNull(action);

        if (iter == null) {
            return;
        }

        T t = null;

        while (iter.hasNext()) {
            t = iter.next();

            final Iterable<T2> c2 = flatMapper.apply(t);

            if (c2 != null) {
                for (T2 t2 : c2) {
                    final Iterable<T3> c3 = flatMapper2.apply(t2);

                    if (c3 != null) {
                        for (T3 t3 : c3) {
                            action.accept(t, t2, t3);
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

        if (isEmpty(a) || isEmpty(b)) {
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
    public static <A, B, E extends Exception> void forEach(final Iterable<A> a, final Iterable<B> b,
            final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
        checkArgNotNull(action);

        if (a == null || b == null) {
            return;
        }

        final Iterator<A> iterA = a.iterator();
        final Iterator<B> iterB = b.iterator();

        forEach(iterA, iterB, action);
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
    public static <A, B, E extends Exception> void forEach(final Iterator<A> a, final Iterator<B> b,
            final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
        checkArgNotNull(action);

        if (a == null || b == null) {
            return;
        }

        while (a.hasNext() && b.hasNext()) {
            action.accept(a.next(), b.next());
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

        if (isEmpty(a) || isEmpty(b) || isEmpty(c)) {
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
    public static <A, B, C, E extends Exception> void forEach(final Iterable<A> a, final Iterable<B> b, final Iterable<C> c,
            final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
        checkArgNotNull(action);

        if (a == null || b == null || c == null) {
            return;
        }

        final Iterator<A> iterA = a.iterator();
        final Iterator<B> iterB = b.iterator();
        final Iterator<C> iterC = c.iterator();

        forEach(iterA, iterB, iterC, action);
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
    public static <A, B, C, E extends Exception> void forEach(final Iterator<A> a, final Iterator<B> b, final Iterator<C> c,
            final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
        checkArgNotNull(action);

        if (a == null || b == null || c == null) {
            return;
        }

        while (a.hasNext() && b.hasNext() && c.hasNext()) {
            action.accept(a.next(), b.next(), c.next());
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
    public static <A, B, E extends Exception> void forEach(final Iterable<A> a, final Iterable<B> b, final A valueForNoneA, final B valueForNoneB,
            final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
        checkArgNotNull(action);

        final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a.iterator();
        final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b.iterator();

        forEach(iterA, iterB, valueForNoneA, valueForNoneB, action);
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
    public static <A, B, E extends Exception> void forEach(final Iterator<A> a, final Iterator<B> b, final A valueForNoneA, final B valueForNoneB,
            final Throwables.BiConsumer<? super A, ? super B, E> action) throws E {
        checkArgNotNull(action);

        final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a;
        final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b;

        A nextA = null;
        B nextB = null;

        while (iterA.hasNext() || iterB.hasNext()) {
            nextA = iterA.hasNext() ? iterA.next() : valueForNoneA;
            nextB = iterB.hasNext() ? iterB.next() : valueForNoneB;

            action.accept(nextA, nextB);
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
    public static <A, B, C, E extends Exception> void forEach(final Iterable<A> a, final Iterable<B> b, final Iterable<C> c, final A valueForNoneA,
            final B valueForNoneB, final C valueForNoneC, final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
        checkArgNotNull(action);

        final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a.iterator();
        final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b.iterator();
        final Iterator<C> iterC = c == null ? ObjIterator.<C> empty() : c.iterator();

        forEach(iterA, iterB, iterC, valueForNoneA, valueForNoneB, valueForNoneC, action);
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
    public static <A, B, C, E extends Exception> void forEach(final Iterator<A> a, final Iterator<B> b, final Iterator<C> c, final A valueForNoneA,
            final B valueForNoneB, final C valueForNoneC, final Throwables.TriConsumer<? super A, ? super B, ? super C, E> action) throws E {
        checkArgNotNull(action);

        final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a;
        final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b;
        final Iterator<C> iterC = b == null ? ObjIterator.<C> empty() : c;

        A nextA = null;
        B nextB = null;
        C nextC = null;

        while (iterA.hasNext() || iterB.hasNext() || iterC.hasNext()) {
            nextA = iterA.hasNext() ? iterA.next() : valueForNoneA;
            nextB = iterB.hasNext() ? iterB.next() : valueForNoneB;
            nextC = iterC.hasNext() ? iterC.next() : valueForNoneC;

            action.accept(nextA, nextB, nextC);
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

        if (isEmpty(a)) {
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
    public static <T, E extends Exception> void forEachNonNull(final Iterable<? extends T> c, final Throwables.Consumer<? super T, E> action) throws E {
        checkArgNotNull(action);

        if (c == null) {
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
            final Throwables.Function<? super T, ? extends Iterable<U>, E> flatMapper, final Throwables.BiConsumer<? super T, ? super U, E2> action)
            throws E, E2 {
        checkArgNotNull(flatMapper);
        checkArgNotNull(action);

        if (isEmpty(a)) {
            return;
        }

        for (T e : a) {
            if (e != null) {
                final Iterable<U> c2 = flatMapper.apply(e);

                if (c2 != null) {
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
    public static <T, U, E extends Exception, E2 extends Exception> void forEachNonNull(final Iterable<? extends T> c,
            final Throwables.Function<? super T, ? extends Iterable<U>, E> flatMapper, final Throwables.BiConsumer<? super T, ? super U, E2> action)
            throws E, E2 {
        checkArgNotNull(flatMapper);
        checkArgNotNull(action);

        if (c == null) {
            return;
        }

        for (T e : c) {
            if (e != null) {
                final Iterable<U> c2 = flatMapper.apply(e);

                if (c2 != null) {
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
     * @param iter
     * @param flatMapper
     * @param action
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, U, E extends Exception, E2 extends Exception> void forEachNonNull(final Iterator<? extends T> iter,
            final Throwables.Function<? super T, ? extends Iterable<U>, E> flatMapper, final Throwables.BiConsumer<? super T, ? super U, E2> action)
            throws E, E2 {
        checkArgNotNull(flatMapper);
        checkArgNotNull(action);

        if (iter == null) {
            return;
        }

        T e = null;

        while (iter.hasNext()) {
            e = iter.next();

            if (e != null) {
                final Iterable<U> c2 = flatMapper.apply(e);

                if (c2 != null) {
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
            final Throwables.Function<? super T, ? extends Iterable<T2>, E> flatMapper,
            final Throwables.Function<? super T2, ? extends Iterable<T3>, E2> flatMapper2,
            final Throwables.TriConsumer<? super T, ? super T2, ? super T3, E3> action) throws E, E2, E3 {
        checkArgNotNull(flatMapper);
        checkArgNotNull(flatMapper2);
        checkArgNotNull(action);

        if (isEmpty(a)) {
            return;
        }

        for (T e : a) {
            if (e != null) {
                final Iterable<T2> c2 = flatMapper.apply(e);

                if (c2 != null) {
                    for (T2 t2 : c2) {
                        if (t2 != null) {
                            final Iterable<T3> c3 = flatMapper2.apply(t2);

                            if (c3 != null) {
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
    public static <T, T2, T3, E extends Exception, E2 extends Exception, E3 extends Exception> void forEachNonNull(final Iterable<? extends T> c,
            final Throwables.Function<? super T, ? extends Iterable<T2>, E> flatMapper,
            final Throwables.Function<? super T2, ? extends Iterable<T3>, E2> flatMapper2,
            final Throwables.TriConsumer<? super T, ? super T2, ? super T3, E3> action) throws E, E2, E3 {
        checkArgNotNull(flatMapper);
        checkArgNotNull(flatMapper2);
        checkArgNotNull(action);

        if (c == null) {
            return;
        }

        for (T e : c) {
            if (e != null) {
                final Iterable<T2> c2 = flatMapper.apply(e);

                if (c2 != null) {
                    for (T2 t2 : c2) {
                        if (t2 != null) {
                            final Iterable<T3> c3 = flatMapper2.apply(t2);

                            if (c3 != null) {
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
     * @param iter
     * @param flatMapper
     * @param flatMapper2
     * @param action
     * @throws E the e
     * @throws E2 the e2
     * @throws E3 the e3
     */
    public static <T, T2, T3, E extends Exception, E2 extends Exception, E3 extends Exception> void forEachNonNull(final Iterator<? extends T> iter,
            final Throwables.Function<? super T, ? extends Iterable<T2>, E> flatMapper,
            final Throwables.Function<? super T2, ? extends Iterable<T3>, E2> flatMapper2,
            final Throwables.TriConsumer<? super T, ? super T2, ? super T3, E3> action) throws E, E2, E3 {
        checkArgNotNull(flatMapper);
        checkArgNotNull(flatMapper2);
        checkArgNotNull(action);

        if (iter == null) {
            return;
        }

        T e = null;

        while (iter.hasNext()) {
            e = iter.next();

            if (e != null) {
                final Iterable<T2> c2 = flatMapper.apply(e);

                if (c2 != null) {
                    for (T2 t2 : c2) {
                        if (t2 != null) {
                            final Iterable<T3> c3 = flatMapper2.apply(t2);

                            if (c3 != null) {
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
     * For each pair.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachPair(final T[] a, final Throwables.BiConsumer<? super T, ? super T, E> action) throws E {
        forEachPair(a, 1, action);
    }

    /**
     * For each pair.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param increment
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachPair(final T[] a, final int increment, final Throwables.BiConsumer<? super T, ? super T, E> action)
            throws E {
        checkArgNotNull(action);
        final int windowSize = 2;
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment); //NOSONAR

        if (isEmpty(a)) {
            return;
        }

        final Iterator<? extends T> iter = ObjIterator.of(a);
        forEachPair(iter, increment, action);
    }

    /**
     * For each pair.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachPair(final Iterable<? extends T> c, final Throwables.BiConsumer<? super T, ? super T, E> action)
            throws E {
        forEachPair(c, 1, action);
    }

    /**
     * For each pair.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param increment
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachPair(final Iterable<? extends T> c, final int increment,
            final Throwables.BiConsumer<? super T, ? super T, E> action) throws E {
        checkArgNotNull(action);
        final int windowSize = 2;
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        if (c == null) {
            return;
        }

        final Iterator<? extends T> iter = c.iterator();
        forEachPair(iter, increment, action);
    }

    /**
     * For each pair.
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachPair(final Iterator<? extends T> iter, final Throwables.BiConsumer<? super T, ? super T, E> action)
            throws E {
        forEachPair(iter, 1, action);
    }

    /**
     * For each pair.
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param increment
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachPair(final Iterator<? extends T> iter, final int increment,
            final Throwables.BiConsumer<? super T, ? super T, E> action) throws E {
        checkArgNotNull(action);
        final int windowSize = 2;
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        if (iter == null) {
            return;
        }

        boolean isFirst = true;
        T prev = null;

        while (iter.hasNext()) {
            if (increment > windowSize && !isFirst) {
                int skipNum = increment - windowSize;

                while (skipNum-- > 0 && iter.hasNext()) {
                    iter.next();
                }

                if (!iter.hasNext()) {
                    break;
                }
            }

            if (increment == 1) {
                action.accept(isFirst ? iter.next() : prev, (prev = (iter.hasNext() ? iter.next() : null)));
            } else {
                action.accept(iter.next(), iter.hasNext() ? iter.next() : null);
            }

            isFirst = false;
        }
    }

    /**
     * For each triple.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachTriple(final T[] a, final Throwables.TriConsumer<? super T, ? super T, ? super T, E> action) throws E {
        forEachTriple(a, 1, action);
    }

    /**
     * For each triple.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param increment
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachTriple(final T[] a, final int increment,
            final Throwables.TriConsumer<? super T, ? super T, ? super T, E> action) throws E {
        checkArgNotNull(action);
        final int windowSize = 3;
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        if (isEmpty(a)) {
            return;
        }

        final Iterator<? extends T> iter = ObjIterator.of(a);
        forEachTriple(iter, increment, action);
    }

    /**
     * For each triple.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachTriple(final Iterable<? extends T> c,
            final Throwables.TriConsumer<? super T, ? super T, ? super T, E> action) throws E {
        forEachTriple(c, 1, action);
    }

    /**
     * For each triple.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param increment
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachTriple(final Iterable<? extends T> c, final int increment,
            final Throwables.TriConsumer<? super T, ? super T, ? super T, E> action) throws E {
        checkArgNotNull(action);
        final int windowSize = 3;
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        if (c == null) {
            return;
        }

        final Iterator<? extends T> iter = c.iterator();
        forEachTriple(iter, increment, action);
    }

    /**
     * For each triple.
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachTriple(final Iterator<? extends T> iter,
            final Throwables.TriConsumer<? super T, ? super T, ? super T, E> action) throws E {
        forEachTriple(iter, 1, action);
    }

    /**
     * For each triple.
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param increment
     * @param action
     * @throws E the e
     */
    public static <T, E extends Exception> void forEachTriple(final Iterator<? extends T> iter, final int increment,
            final Throwables.TriConsumer<? super T, ? super T, ? super T, E> action) throws E {
        checkArgNotNull(action);
        final int windowSize = 3;
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        if (iter == null) {
            return;
        }

        boolean isFirst = true;
        T prev = null;
        T prev2 = null;

        while (iter.hasNext()) {
            if (increment > windowSize && !isFirst) {
                int skipNum = increment - windowSize;

                while (skipNum-- > 0 && iter.hasNext()) {
                    iter.next();
                }

                if (!iter.hasNext()) {
                    break;
                }
            }

            if (increment == 1) {
                action.accept(isFirst ? iter.next() : prev2, (prev2 = (isFirst ? (iter.hasNext() ? iter.next() : null) : prev)),
                        (prev = (iter.hasNext() ? iter.next() : null)));
            } else if (increment == 2) {
                action.accept(isFirst ? iter.next() : prev, iter.hasNext() ? iter.next() : null, (prev = (iter.hasNext() ? iter.next() : null)));
            } else {
                action.accept(iter.next(), iter.hasNext() ? iter.next() : null, iter.hasNext() ? iter.next() : null);
            }

            isFirst = false;
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
    public static <E extends Exception> boolean[] filter(final boolean[] a, final Throwables.BooleanPredicate<E> filter) throws E {
        checkArgNotNull(filter, "filter"); //NOSONAR

        if (isEmpty(a)) {
            return EMPTY_BOOLEAN_ARRAY;
        }

        return filter(a, 0, a.length, filter);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param max
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> boolean[] filter(final boolean[] a, final int max, final Throwables.BooleanPredicate<E> filter) throws E {
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
            return EMPTY_BOOLEAN_ARRAY;
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
    public static <E extends Exception> boolean[] filter(final boolean[] a, final int fromIndex, final int toIndex, final Throwables.BooleanPredicate<E> filter)
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
    public static <E extends Exception> boolean[] filter(final boolean[] a, final int fromIndex, final int toIndex, final Throwables.BooleanPredicate<E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter, "filter");

        if (isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_BOOLEAN_ARRAY;
        }

        boolean[] result = new boolean[(toIndex - fromIndex) / 2];
        int len = result.length;
        int count = 0;

        for (int i = fromIndex, cnt = 0; i < toIndex && cnt < max; i++) {
            if (filter.test(a[i])) {
                if (count == len) {
                    result = copyOf(result, toIndex - fromIndex);
                    len = result.length;
                }

                result[count++] = a[i];
            }
        }

        return result.length == count ? result : copyOfRange(result, 0, count);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> char[] filter(final char[] a, final Throwables.CharPredicate<E> filter) throws E {
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
            return EMPTY_CHAR_ARRAY;
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
    public static <E extends Exception> char[] filter(final char[] a, final Throwables.CharPredicate<E> filter, final int max) throws E {
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
            return EMPTY_CHAR_ARRAY;
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
    public static <E extends Exception> char[] filter(final char[] a, final int fromIndex, final int toIndex, final Throwables.CharPredicate<E> filter)
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
    public static <E extends Exception> char[] filter(final char[] a, final int fromIndex, final int toIndex, final Throwables.CharPredicate<E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter, "filter");

        if (isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_CHAR_ARRAY;
        }

        char[] result = new char[(toIndex - fromIndex) / 2];
        int len = result.length;
        int count = 0;

        for (int i = fromIndex, cnt = 0; i < toIndex && cnt < max; i++) {
            if (filter.test(a[i])) {
                if (count == len) {
                    result = copyOf(result, toIndex - fromIndex);
                    len = result.length;
                }

                result[count++] = a[i];
            }
        }

        return result.length == count ? result : copyOfRange(result, 0, count);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> byte[] filter(final byte[] a, final Throwables.BytePredicate<E> filter) throws E {
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
            return EMPTY_BYTE_ARRAY;
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
    public static <E extends Exception> byte[] filter(final byte[] a, final Throwables.BytePredicate<E> filter, final int max) throws E {
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
            return EMPTY_BYTE_ARRAY;
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
    public static <E extends Exception> byte[] filter(final byte[] a, final int fromIndex, final int toIndex, final Throwables.BytePredicate<E> filter)
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
    public static <E extends Exception> byte[] filter(final byte[] a, final int fromIndex, final int toIndex, final Throwables.BytePredicate<E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter, "filter");

        if (isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_BYTE_ARRAY;
        }

        byte[] result = new byte[(toIndex - fromIndex) / 2];
        int len = result.length;
        int count = 0;

        for (int i = fromIndex, cnt = 0; i < toIndex && cnt < max; i++) {
            if (filter.test(a[i])) {
                if (count == len) {
                    result = copyOf(result, toIndex - fromIndex);
                    len = result.length;
                }

                result[count++] = a[i];
            }
        }

        return result.length == count ? result : copyOfRange(result, 0, count);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> short[] filter(final short[] a, final Throwables.ShortPredicate<E> filter) throws E {
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
            return EMPTY_SHORT_ARRAY;
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
    public static <E extends Exception> short[] filter(final short[] a, final Throwables.ShortPredicate<E> filter, final int max) throws E {
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
            return EMPTY_SHORT_ARRAY;
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
    public static <E extends Exception> short[] filter(final short[] a, final int fromIndex, final int toIndex, final Throwables.ShortPredicate<E> filter)
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
    public static <E extends Exception> short[] filter(final short[] a, final int fromIndex, final int toIndex, final Throwables.ShortPredicate<E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter, "filter");

        if (isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_SHORT_ARRAY;
        }

        short[] result = new short[(toIndex - fromIndex) / 2];
        int len = result.length;
        int count = 0;

        for (int i = fromIndex, cnt = 0; i < toIndex && cnt < max; i++) {
            if (filter.test(a[i])) {
                if (count == len) {
                    result = copyOf(result, toIndex - fromIndex);
                    len = result.length;
                }

                result[count++] = a[i];
            }
        }

        return result.length == count ? result : copyOfRange(result, 0, count);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> int[] filter(final int[] a, final Throwables.IntPredicate<E> filter) throws E {
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
            return EMPTY_INT_ARRAY;
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
    public static <E extends Exception> int[] filter(final int[] a, final Throwables.IntPredicate<E> filter, final int max) throws E {
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
            return EMPTY_INT_ARRAY;
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
    public static <E extends Exception> int[] filter(final int[] a, final int fromIndex, final int toIndex, final Throwables.IntPredicate<E> filter) throws E {
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
    public static <E extends Exception> int[] filter(final int[] a, final int fromIndex, final int toIndex, final Throwables.IntPredicate<E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter, "filter");

        if (isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_INT_ARRAY;
        }

        int[] result = new int[(toIndex - fromIndex) / 2];
        int len = result.length;
        int count = 0;

        for (int i = fromIndex, cnt = 0; i < toIndex && cnt < max; i++) {
            if (filter.test(a[i])) {
                if (count == len) {
                    result = copyOf(result, toIndex - fromIndex);
                    len = result.length;
                }

                result[count++] = a[i];
            }
        }

        return result.length == count ? result : copyOfRange(result, 0, count);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> long[] filter(final long[] a, final Throwables.LongPredicate<E> filter) throws E {
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
            return EMPTY_LONG_ARRAY;
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
    public static <E extends Exception> long[] filter(final long[] a, final Throwables.LongPredicate<E> filter, final int max) throws E {
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
            return EMPTY_LONG_ARRAY;
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
    public static <E extends Exception> long[] filter(final long[] a, final int fromIndex, final int toIndex, final Throwables.LongPredicate<E> filter)
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
    public static <E extends Exception> long[] filter(final long[] a, final int fromIndex, final int toIndex, final Throwables.LongPredicate<E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter, "filter");

        if (isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_LONG_ARRAY;
        }

        long[] result = new long[(toIndex - fromIndex) / 2];
        int len = result.length;
        int count = 0;

        for (int i = fromIndex, cnt = 0; i < toIndex && cnt < max; i++) {
            if (filter.test(a[i])) {
                if (count == len) {
                    result = copyOf(result, toIndex - fromIndex);
                    len = result.length;
                }

                result[count++] = a[i];
            }
        }

        return result.length == count ? result : copyOfRange(result, 0, count);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> float[] filter(final float[] a, final Throwables.FloatPredicate<E> filter) throws E {
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
            return EMPTY_FLOAT_ARRAY;
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
    public static <E extends Exception> float[] filter(final float[] a, final Throwables.FloatPredicate<E> filter, final int max) throws E {
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
            return EMPTY_FLOAT_ARRAY;
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
    public static <E extends Exception> float[] filter(final float[] a, final int fromIndex, final int toIndex, final Throwables.FloatPredicate<E> filter)
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
    public static <E extends Exception> float[] filter(final float[] a, final int fromIndex, final int toIndex, final Throwables.FloatPredicate<E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter, "filter");

        if (isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_FLOAT_ARRAY;
        }

        float[] result = new float[(toIndex - fromIndex) / 2];
        int len = result.length;
        int count = 0;

        for (int i = fromIndex, cnt = 0; i < toIndex && cnt < max; i++) {
            if (filter.test(a[i])) {
                if (count == len) {
                    result = copyOf(result, toIndex - fromIndex);
                    len = result.length;
                }

                result[count++] = a[i];
            }
        }

        return result.length == count ? result : copyOfRange(result, 0, count);
    }

    /**
     *
     * @param <E>
     * @param a
     * @param filter
     * @return
     * @throws E the e
     */
    public static <E extends Exception> double[] filter(final double[] a, final Throwables.DoublePredicate<E> filter) throws E {
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
            return EMPTY_DOUBLE_ARRAY;
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
    public static <E extends Exception> double[] filter(final double[] a, final Throwables.DoublePredicate<E> filter, final int max) throws E {
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
            return EMPTY_DOUBLE_ARRAY;
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
    public static <E extends Exception> double[] filter(final double[] a, final int fromIndex, final int toIndex, final Throwables.DoublePredicate<E> filter)
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
    public static <E extends Exception> double[] filter(final double[] a, final int fromIndex, final int toIndex, final Throwables.DoublePredicate<E> filter,
            final int max) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(filter, "filter");

        if (isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_DOUBLE_ARRAY;
        }

        double[] result = new double[(toIndex - fromIndex) / 2];
        int len = result.length;
        int count = 0;

        for (int i = fromIndex, cnt = 0; i < toIndex && cnt < max; i++) {
            if (filter.test(a[i])) {
                if (count == len) {
                    result = copyOf(result, toIndex - fromIndex);
                    len = result.length;
                }

                result[count++] = a[i];
            }
        }

        return result.length == count ? result : copyOfRange(result, 0, count);
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
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
            return new ArrayList<>();
        }

        return filter(a, filter, Integer.MAX_VALUE);
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
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
            return supplier.apply(0);
        }

        return filter(a, filter, Integer.MAX_VALUE, supplier);
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
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
            return new ArrayList<>();
        }

        return filter(a, 0, a.length, filter, max);
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
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
            return supplier.apply(0);
        }

        return filter(a, 0, a.length, filter, max, supplier);
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
        return filter(a, fromIndex, toIndex, filter, max, Factory.ofList());
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
        checkArgNotNull(filter, "filter");

        if (isEmpty(a) || fromIndex == toIndex) {
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
     * @param <E>
     * @param c
     * @param filter
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> filter(final Iterable<? extends T> c, final Throwables.Predicate<? super T, E> filter) throws E {
        return filter(c, filter, Integer.MAX_VALUE);
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
    public static <T, R extends Collection<T>, E extends Exception> R filter(final Iterable<? extends T> c, final Throwables.Predicate<? super T, E> filter,
            final IntFunction<R> supplier) throws E {
        return filter(c, filter, Integer.MAX_VALUE, supplier);
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
    public static <T, E extends Exception> List<T> filter(final Iterable<? extends T> c, final Throwables.Predicate<? super T, E> filter, final int max)
            throws E {
        return filter(c, filter, max, Factory.ofList());
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
    public static <T, R extends Collection<T>, E extends Exception> R filter(final Iterable<? extends T> c, final Throwables.Predicate<? super T, E> filter,
            final int max, final IntFunction<R> supplier) throws E {
        checkArgNotNull(filter, "filter");

        if (c == null) {
            return supplier.apply(0);
        }

        final R result = supplier.apply(getMinSize(c));
        int count = 0;

        for (T e : c) {
            if (filter.test(e)) {
                result.add(e);

                if (++count == max) {
                    break;
                }
            }
        }

        return result;
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
        return filter(c, fromIndex, toIndex, filter, max, Factory.ofList());
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
        checkArgNotNull(filter, "filter");

        if ((isEmpty(c) && fromIndex == 0 && toIndex == 0) || fromIndex == toIndex) {
            return supplier.apply(0);
        }

        final R result = supplier.apply(min(9, max, (toIndex - fromIndex)));

        if ((isEmpty(c) && fromIndex == 0 && toIndex == 0) || (fromIndex == toIndex && fromIndex < c.size())) {
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
                if (idx++ < fromIndex) {
                    continue;
                }

                if (filter.test(e)) {
                    result.add(e);

                    if (++cnt >= max) {
                        break;
                    }
                }

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return result;
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param iter
    //     * @param filter
    //     * @return
    //     * @see {@code Iterators.filter(Iterator, Predicate)}
    //     */
    //    public static <T, E extends Exception> List<T> filter(final Iterator<? extends T> iter, final Throwables.Predicate<? super T, E> filter) throws E {
    //        return filter(iter, 0, Integer.MAX_VALUE, filter);
    //    }
    //
    //    public static <T, E extends Exception> List<T> filter(final Iterator<? extends T> iter, final Throwables.Predicate<? super T, E> filter, final int max)
    //            throws E {
    //        return filter(iter, 0, Integer.MAX_VALUE, filter, max);
    //    }
    //
    //    public static <T, E extends Exception> List<T> filter(final Iterator<? extends T> iter, final int fromIndex, final int toIndex,
    //            final Throwables.Predicate<? super T, E> filter) throws E {
    //        checkFromToIndex(fromIndex, toIndex, Integer.MAX_VALUE);
    //        checkArgNotNull(filter, "filter");
    //
    //        if (iter == null || fromIndex == toIndex) {
    //            return new ArrayList<>(0);
    //        }
    //
    //        final List<T> result = new ArrayList<>(min(9, toIndex - fromIndex));
    //        int idx = 0;
    //        T e = null;
    //
    //        while (iter.hasNext()) {
    //            e = iter.next();
    //
    //            if (idx++ < fromIndex) {
    //                continue;
    //            }
    //
    //            if (filter.test(e)) {
    //                result.add(e);
    //            }
    //
    //            if (idx >= toIndex) {
    //                break;
    //            }
    //        }
    //
    //        return result;
    //    }
    //
    //    public static <T, E extends Exception> List<T> filter(final Iterator<? extends T> iter, final int fromIndex, final int toIndex,
    //            final Throwables.Predicate<? super T, E> filter, final int max) throws E {
    //        checkFromToIndex(fromIndex, toIndex, Integer.MAX_VALUE);
    //        checkArgNotNull(filter, "filter");
    //
    //        if (iter == null || fromIndex == toIndex) {
    //            return new ArrayList<>(0);
    //        }
    //
    //        final List<T> result = new ArrayList<>(min(9, max, (toIndex - fromIndex)));
    //        int idx = 0;
    //        int cnt = 0;
    //        T e = null;
    //
    //        while (iter.hasNext()) {
    //            e = iter.next();
    //
    //            if (idx++ < fromIndex) {
    //                continue;
    //            }
    //
    //            if (filter.test(e)) {
    //                result.add(e);
    //
    //                if (++cnt >= max) {
    //                    break;
    //                }
    //            }
    //
    //            if (idx >= toIndex) {
    //                break;
    //            }
    //        }
    //
    //        return result;
    //    }

    /**
     * Map to boolean.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> boolean[] mapToBoolean(final T[] a, final Throwables.ToBooleanFunction<? super T, E> mapper) throws E {
        checkArgNotNull(mapper);

        if (isEmpty(a)) {
            return EMPTY_BOOLEAN_ARRAY;
        }

        return mapToBoolean(a, 0, a.length, mapper);
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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> boolean[] mapToBoolean(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.ToBooleanFunction<? super T, E> mapper) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(mapper);

        if (isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_BOOLEAN_ARRAY;
        }

        final boolean[] result = new boolean[toIndex - fromIndex];

        for (int i = fromIndex; i < toIndex; i++) {
            result[i - fromIndex] = mapper.applyAsBoolean(a[i]);
        }

        return result;
    }

    /**
     * Map to boolean.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> boolean[] mapToBoolean(final Collection<? extends T> c, final Throwables.ToBooleanFunction<? super T, E> mapper)
            throws E {
        checkArgNotNull(mapper);

        if (isEmpty(c)) {
            return EMPTY_BOOLEAN_ARRAY;
        }

        return mapToBoolean(c, 0, c.size(), mapper);
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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> boolean[] mapToBoolean(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToBooleanFunction<? super T, E> mapper) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(mapper);

        if ((isEmpty(c) && fromIndex == 0 && toIndex == 0) || fromIndex == toIndex) {
            return EMPTY_BOOLEAN_ARRAY;
        }

        final boolean[] result = new boolean[toIndex - fromIndex];

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                result[i - fromIndex] = mapper.applyAsBoolean(list.get(i));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                result[idx - fromIndex] = mapper.applyAsBoolean(e);

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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> char[] mapToChar(final T[] a, final Throwables.ToCharFunction<? super T, E> mapper) throws E {
        checkArgNotNull(mapper);

        if (isEmpty(a)) {
            return EMPTY_CHAR_ARRAY;
        }

        return mapToChar(a, 0, a.length, mapper);
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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> char[] mapToChar(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.ToCharFunction<? super T, E> mapper) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(mapper);

        if (isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_CHAR_ARRAY;
        }

        final char[] result = new char[toIndex - fromIndex];

        for (int i = fromIndex; i < toIndex; i++) {
            result[i - fromIndex] = mapper.applyAsChar(a[i]);
        }

        return result;
    }

    /**
     * Map to char.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> char[] mapToChar(final Collection<? extends T> c, final Throwables.ToCharFunction<? super T, E> mapper) throws E {
        checkArgNotNull(mapper);

        if (isEmpty(c)) {
            return EMPTY_CHAR_ARRAY;
        }

        return mapToChar(c, 0, c.size(), mapper);
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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> char[] mapToChar(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToCharFunction<? super T, E> mapper) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(mapper);

        if ((isEmpty(c) && fromIndex == 0 && toIndex == 0) || fromIndex == toIndex) {
            return EMPTY_CHAR_ARRAY;
        }

        final char[] result = new char[toIndex - fromIndex];

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                result[i - fromIndex] = mapper.applyAsChar(list.get(i));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                result[idx - fromIndex] = mapper.applyAsChar(e);

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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> byte[] mapToByte(final T[] a, final Throwables.ToByteFunction<? super T, E> mapper) throws E {
        checkArgNotNull(mapper);

        if (isEmpty(a)) {
            return EMPTY_BYTE_ARRAY;
        }

        return mapToByte(a, 0, a.length, mapper);
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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> byte[] mapToByte(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.ToByteFunction<? super T, E> mapper) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(mapper);

        if (isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_BYTE_ARRAY;
        }

        final byte[] result = new byte[toIndex - fromIndex];

        for (int i = fromIndex; i < toIndex; i++) {
            result[i - fromIndex] = mapper.applyAsByte(a[i]);
        }

        return result;
    }

    /**
     * Map to byte.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> byte[] mapToByte(final Collection<? extends T> c, final Throwables.ToByteFunction<? super T, E> mapper) throws E {
        checkArgNotNull(mapper);

        if (isEmpty(c)) {
            return EMPTY_BYTE_ARRAY;
        }

        return mapToByte(c, 0, c.size(), mapper);
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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> byte[] mapToByte(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToByteFunction<? super T, E> mapper) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(mapper);

        if ((isEmpty(c) && fromIndex == 0 && toIndex == 0) || fromIndex == toIndex) {
            return EMPTY_BYTE_ARRAY;
        }

        final byte[] result = new byte[toIndex - fromIndex];

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                result[i - fromIndex] = mapper.applyAsByte(list.get(i));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                result[idx - fromIndex] = mapper.applyAsByte(e);

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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> short[] mapToShort(final T[] a, final Throwables.ToShortFunction<? super T, E> mapper) throws E {
        checkArgNotNull(mapper);

        if (isEmpty(a)) {
            return EMPTY_SHORT_ARRAY;
        }

        return mapToShort(a, 0, a.length, mapper);
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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> short[] mapToShort(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.ToShortFunction<? super T, E> mapper) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(mapper);

        if (isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_SHORT_ARRAY;
        }

        final short[] result = new short[toIndex - fromIndex];

        for (int i = fromIndex; i < toIndex; i++) {
            result[i - fromIndex] = mapper.applyAsShort(a[i]);
        }

        return result;
    }

    /**
     * Map to short.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> short[] mapToShort(final Collection<? extends T> c, final Throwables.ToShortFunction<? super T, E> mapper) throws E {
        checkArgNotNull(mapper);

        if (isEmpty(c)) {
            return EMPTY_SHORT_ARRAY;
        }

        return mapToShort(c, 0, c.size(), mapper);
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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> short[] mapToShort(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToShortFunction<? super T, E> mapper) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(mapper);

        if ((isEmpty(c) && fromIndex == 0 && toIndex == 0) || fromIndex == toIndex) {
            return EMPTY_SHORT_ARRAY;
        }

        final short[] result = new short[toIndex - fromIndex];

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                result[i - fromIndex] = mapper.applyAsShort(list.get(i));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                result[idx - fromIndex] = mapper.applyAsShort(e);

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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> int[] mapToInt(final T[] a, final Throwables.ToIntFunction<? super T, E> mapper) throws E {
        checkArgNotNull(mapper);

        if (isEmpty(a)) {
            return EMPTY_INT_ARRAY;
        }

        return mapToInt(a, 0, a.length, mapper);
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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> int[] mapToInt(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.ToIntFunction<? super T, E> mapper) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(mapper);

        if (isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_INT_ARRAY;
        }

        final int[] result = new int[toIndex - fromIndex];

        for (int i = fromIndex; i < toIndex; i++) {
            result[i - fromIndex] = mapper.applyAsInt(a[i]);
        }

        return result;
    }

    /**
     * Map to int.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> int[] mapToInt(final Collection<? extends T> c, final Throwables.ToIntFunction<? super T, E> mapper) throws E {
        checkArgNotNull(mapper);

        if (isEmpty(c)) {
            return EMPTY_INT_ARRAY;
        }

        return mapToInt(c, 0, c.size(), mapper);
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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> int[] mapToInt(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToIntFunction<? super T, E> mapper) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(mapper);

        if ((isEmpty(c) && fromIndex == 0 && toIndex == 0) || fromIndex == toIndex) {
            return EMPTY_INT_ARRAY;
        }

        final int[] result = new int[toIndex - fromIndex];

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                result[i - fromIndex] = mapper.applyAsInt(list.get(i));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                result[idx - fromIndex] = mapper.applyAsInt(e);

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return result;
    }

    /**
     *
     * @param <E>
     * @param a
     * @param mapper
     * @return
     * @throws E the e
     */
    @Beta
    public static <E extends Exception> int[] mapToInt(final long[] a, final Throwables.LongToIntFunction<E> mapper) throws E {
        if (a == null) {
            return EMPTY_INT_ARRAY;
        }

        final int len = len(a);
        final int[] result = new int[len];

        for (int i = 0; i < len; i++) {
            result[i] = mapper.applyAsInt(a[i]);
        }

        return result;
    }

    /**
     *
     * @param <E>
     * @param a
     * @param mapper
     * @return
     * @throws E the e
     */
    @Beta
    public static <E extends Exception> int[] mapToInt(final double[] a, final Throwables.DoubleToIntFunction<E> mapper) throws E {
        if (a == null) {
            return EMPTY_INT_ARRAY;
        }

        final int len = len(a);
        final int[] result = new int[len];

        for (int i = 0; i < len; i++) {
            result[i] = mapper.applyAsInt(a[i]);
        }

        return result;
    }

    /**
     * Map to long.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> long[] mapToLong(final T[] a, final Throwables.ToLongFunction<? super T, E> mapper) throws E {
        checkArgNotNull(mapper);

        if (isEmpty(a)) {
            return EMPTY_LONG_ARRAY;
        }

        return mapToLong(a, 0, a.length, mapper);
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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> long[] mapToLong(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.ToLongFunction<? super T, E> mapper) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(mapper);

        if (isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_LONG_ARRAY;
        }

        final long[] result = new long[toIndex - fromIndex];

        for (int i = fromIndex; i < toIndex; i++) {
            result[i - fromIndex] = mapper.applyAsLong(a[i]);
        }

        return result;
    }

    /**
     * Map to long.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> long[] mapToLong(final Collection<? extends T> c, final Throwables.ToLongFunction<? super T, E> mapper) throws E {
        checkArgNotNull(mapper);

        if (isEmpty(c)) {
            return EMPTY_LONG_ARRAY;
        }

        return mapToLong(c, 0, c.size(), mapper);
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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> long[] mapToLong(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToLongFunction<? super T, E> mapper) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(mapper);

        if ((isEmpty(c) && fromIndex == 0 && toIndex == 0) || fromIndex == toIndex) {
            return EMPTY_LONG_ARRAY;
        }

        final long[] result = new long[toIndex - fromIndex];

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                result[i - fromIndex] = mapper.applyAsLong(list.get(i));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                result[idx - fromIndex] = mapper.applyAsLong(e);

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return result;
    }

    /**
     *
     * @param <E>
     * @param a
     * @param mapper
     * @return
     * @throws E the e
     */
    @Beta
    public static <E extends Exception> long[] mapToLong(final int[] a, final Throwables.IntToLongFunction<E> mapper) throws E {
        if (a == null) {
            return EMPTY_LONG_ARRAY;
        }

        final int len = len(a);
        final long[] result = new long[len];

        for (int i = 0; i < len; i++) {
            result[i] = mapper.applyAsLong(a[i]);
        }

        return result;
    }

    /**
     *
     * @param <E>
     * @param a
     * @param mapper
     * @return
     * @throws E the e
     */
    @Beta
    public static <E extends Exception> long[] mapToLong(final double[] a, final Throwables.DoubleToLongFunction<E> mapper) throws E {
        if (a == null) {
            return EMPTY_LONG_ARRAY;
        }

        final int len = len(a);
        final long[] result = new long[len];

        for (int i = 0; i < len; i++) {
            result[i] = mapper.applyAsLong(a[i]);
        }

        return result;
    }

    /**
     * Map to float.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> float[] mapToFloat(final T[] a, final Throwables.ToFloatFunction<? super T, E> mapper) throws E {
        checkArgNotNull(mapper);

        if (isEmpty(a)) {
            return EMPTY_FLOAT_ARRAY;
        }

        return mapToFloat(a, 0, a.length, mapper);
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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> float[] mapToFloat(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.ToFloatFunction<? super T, E> mapper) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(mapper);

        if (isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_FLOAT_ARRAY;
        }

        final float[] result = new float[toIndex - fromIndex];

        for (int i = fromIndex; i < toIndex; i++) {
            result[i - fromIndex] = mapper.applyAsFloat(a[i]);
        }

        return result;
    }

    /**
     * Map to float.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> float[] mapToFloat(final Collection<? extends T> c, final Throwables.ToFloatFunction<? super T, E> mapper) throws E {
        checkArgNotNull(mapper);

        if (isEmpty(c)) {
            return EMPTY_FLOAT_ARRAY;
        }

        return mapToFloat(c, 0, c.size(), mapper);
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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> float[] mapToFloat(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToFloatFunction<? super T, E> mapper) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(mapper);

        if ((isEmpty(c) && fromIndex == 0 && toIndex == 0) || fromIndex == toIndex) {
            return EMPTY_FLOAT_ARRAY;
        }

        final float[] result = new float[toIndex - fromIndex];

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                result[i - fromIndex] = mapper.applyAsFloat(list.get(i));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                result[idx - fromIndex] = mapper.applyAsFloat(e);

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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> double[] mapToDouble(final T[] a, final Throwables.ToDoubleFunction<? super T, E> mapper) throws E {
        checkArgNotNull(mapper);

        if (isEmpty(a)) {
            return EMPTY_DOUBLE_ARRAY;
        }

        return mapToDouble(a, 0, a.length, mapper);
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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> double[] mapToDouble(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.ToDoubleFunction<? super T, E> mapper) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(mapper);

        if (isEmpty(a) || fromIndex == toIndex) {
            return EMPTY_DOUBLE_ARRAY;
        }

        final double[] result = new double[toIndex - fromIndex];

        for (int i = fromIndex; i < toIndex; i++) {
            result[i - fromIndex] = mapper.applyAsDouble(a[i]);
        }

        return result;
    }

    /**
     * Map to double.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> double[] mapToDouble(final Collection<? extends T> c, final Throwables.ToDoubleFunction<? super T, E> mapper)
            throws E {
        checkArgNotNull(mapper);

        if (isEmpty(c)) {
            return EMPTY_DOUBLE_ARRAY;
        }

        return mapToDouble(c, 0, c.size(), mapper);
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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> double[] mapToDouble(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.ToDoubleFunction<? super T, E> mapper) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(mapper);

        if ((isEmpty(c) && fromIndex == 0 && toIndex == 0) || fromIndex == toIndex) {
            return EMPTY_DOUBLE_ARRAY;
        }

        final double[] result = new double[toIndex - fromIndex];

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                result[i - fromIndex] = mapper.applyAsDouble(list.get(i));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                result[idx - fromIndex] = mapper.applyAsDouble(e);

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return result;
    }

    /**
    *
    * @param <E>
    * @param a
    * @param mapper
    * @return
    * @throws E the e
    */
    @Beta
    public static <E extends Exception> double[] mapToDouble(final int[] a, final Throwables.IntToDoubleFunction<E> mapper) throws E {
        if (a == null) {
            return EMPTY_DOUBLE_ARRAY;
        }

        final int len = len(a);
        final double[] result = new double[len];

        for (int i = 0; i < len; i++) {
            result[i] = mapper.applyAsDouble(a[i]);
        }

        return result;
    }

    /**
     *
     * @param <E>
     * @param a
     * @param mapper
     * @return
     * @throws E the e
     */
    @Beta
    public static <E extends Exception> double[] mapToDouble(final long[] a, final Throwables.LongToDoubleFunction<E> mapper) throws E {
        if (a == null) {
            return EMPTY_DOUBLE_ARRAY;
        }

        final int len = len(a);
        final double[] result = new double[len];

        for (int i = 0; i < len; i++) {
            result[i] = mapper.applyAsDouble(a[i]);
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param a
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> map(final T[] a, final Throwables.Function<? super T, ? extends R, E> mapper) throws E {
        checkArgNotNull(mapper);

        if (isEmpty(a)) {
            return new ArrayList<>();
        }

        return map(a, 0, a.length, mapper);
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param a
     * @param mapper
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C map(final T[] a, final Throwables.Function<? super T, ? extends R, E> mapper,
            final IntFunction<? extends C> supplier) throws E {
        checkArgNotNull(mapper);

        if (isEmpty(a)) {
            return supplier.apply(0);
        }

        return map(a, 0, a.length, mapper, supplier);
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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> map(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ? extends R, E> mapper) throws E {
        return map(a, fromIndex, toIndex, mapper, Factory.ofList());
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
     * @param mapper
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C map(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ? extends R, E> mapper, final IntFunction<? extends C> supplier) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(mapper);

        if (isEmpty(a) || fromIndex == toIndex) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);

        for (int i = fromIndex; i < toIndex; i++) {
            result.add(mapper.apply(a[i]));
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param c
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> map(final Iterable<? extends T> c, final Throwables.Function<? super T, ? extends R, E> mapper) throws E {
        return map(c, mapper, Factory.ofList());
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param c
     * @param mapper
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C map(final Iterable<? extends T> c,
            final Throwables.Function<? super T, ? extends R, E> mapper, final IntFunction<? extends C> supplier) throws E {
        checkArgNotNull(mapper);

        if (c == null) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(getSizeOrDefault(c, 0));

        for (T e : c) {
            result.add(mapper.apply(e));
        }

        return result;
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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> map(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ? extends R, E> mapper) throws E {
        return map(c, fromIndex, toIndex, mapper, Factory.ofList());
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
     * @param mapper
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C map(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ? extends R, E> mapper, final IntFunction<? extends C> supplier) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(mapper);

        if ((isEmpty(c) && fromIndex == 0 && toIndex == 0) || fromIndex == toIndex) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(toIndex - fromIndex);

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(mapper.apply(list.get(i)));
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                result.add(mapper.apply(e));

                if (idx >= toIndex) {
                    break;
                }
            }
        }

        return result;
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param <U>
    //     * @param iter
    //     * @param mapper
    //     * @return
    //     * @see {@code Iterators.map(Iterator, Function)}
    //     */
    //    public static <T, R, E extends Exception> List<R> map(final Iterator<? extends T> iter, final Throwables.Function<? super T, ? extends R, E> mapper)
    //            throws E {
    //        return map(iter, 0, Integer.MAX_VALUE, mapper);
    //    }
    //
    //    public static <T, R, E extends Exception> List<R> map(final Iterator<? extends T> iter, final int fromIndex, final int toIndex,
    //            final Throwables.Function<? super T, ? extends R, E> mapper) throws E {
    //        checkFromToIndex(fromIndex, toIndex, Integer.MAX_VALUE);
    //        checkArgNotNull(mapper);
    //
    //        if (iter == null || fromIndex == toIndex) {
    //            return new ArrayList<>();
    //        }
    //
    //        final List<R> result = new ArrayList<>(min(9, toIndex - fromIndex));
    //
    //        int idx = 0;
    //        T e = null;
    //
    //        while (iter.hasNext()) {
    //            e = iter.next();
    //
    //            if (idx++ < fromIndex) {
    //                continue;
    //            }
    //
    //            result.add(mapper.apply(e));
    //
    //            if (idx >= toIndex) {
    //                break;
    //            }
    //        }
    //
    //        return result;
    //    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param a
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> flatMap(final T[] a, final Throwables.Function<? super T, ? extends Collection<? extends R>, E> mapper)
            throws E {
        checkArgNotNull(mapper);

        if (isEmpty(a)) {
            return new ArrayList<>();
        }

        return flatMap(a, 0, a.length, mapper);
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param a
     * @param mapper
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C flatMap(final T[] a,
            final Throwables.Function<? super T, ? extends Collection<? extends R>, E> mapper, final IntFunction<? extends C> supplier) throws E {
        checkArgNotNull(mapper);

        if (isEmpty(a)) {
            return supplier.apply(0);
        }

        return flatMap(a, 0, a.length, mapper, supplier);
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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> flatMap(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ? extends Collection<? extends R>, E> mapper) throws E {
        return flatMap(a, fromIndex, toIndex, mapper, Factory.ofList());
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
     * @param mapper
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C flatMap(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ? extends Collection<? extends R>, E> mapper, final IntFunction<? extends C> supplier) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(mapper);

        if (isEmpty(a) || fromIndex == toIndex) {
            return supplier.apply(0);
        }

        final int len = initSizeForFlatMap(toIndex - fromIndex);
        final C result = supplier.apply(len);
        Collection<? extends R> mr = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (notEmpty(mr = mapper.apply(a[i]))) {
                result.addAll(mr);
            }
        }

        return result;
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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> flatMap(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ? extends Collection<? extends R>, E> mapper) throws E {
        return flatMap(c, fromIndex, toIndex, mapper, Factory.<R> ofList());
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
     * @param mapper
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C flatMap(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ? extends Collection<? extends R>, E> mapper, final IntFunction<? extends C> supplier) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(mapper);

        if ((isEmpty(c) && fromIndex == 0 && toIndex == 0) || fromIndex == toIndex) {
            return supplier.apply(0);
        }

        final int len = initSizeForFlatMap(toIndex - fromIndex);
        final C result = supplier.apply(len);
        Collection<? extends R> mr = null;

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                if (notEmpty(mr = mapper.apply(list.get(i)))) {
                    result.addAll(mr);
                }
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                if (notEmpty(mr = mapper.apply(e))) {
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
     * @param <E>
     * @param c
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> flatMap(final Iterable<? extends T> c,
            final Throwables.Function<? super T, ? extends Collection<? extends R>, E> mapper) throws E {
        return flatMap(c, mapper, Factory.<R> ofList());
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param c
     * @param mapper
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C flatMap(final Iterable<? extends T> c,
            final Throwables.Function<? super T, ? extends Collection<? extends R>, E> mapper, final IntFunction<? extends C> supplier) throws E {
        checkArgNotNull(mapper);

        if (c == null) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(initSizeForFlatMap(c));
        Collection<? extends R> mr = null;

        for (T e : c) {
            if (notEmpty(mr = mapper.apply(e))) {
                result.addAll(mr);
            }
        }

        return result;
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param <U>
    //     * @param iter
    //     * @param mapper
    //     * @return
    //     * @see {@code Iterators.flatMap(Iterator, Function)}
    //     */
    //    public static <T, R, E extends Exception> List<R> flatMap(final Iterator<? extends T> iter,
    //            final Throwables.Function<? super T, ? extends Collection<? extends R>, E> mapper) throws E {
    //        return flatMap(iter, 0, Integer.MAX_VALUE, mapper);
    //    }
    //
    //    public static <T, R, E extends Exception> List<R> flatMap(final Iterator<? extends T> iter, final int fromIndex, final int toIndex,
    //            final Throwables.Function<? super T, ? extends Collection<? extends R>, E> mapper) throws E {
    //        checkFromToIndex(fromIndex, toIndex, Integer.MAX_VALUE);
    //        checkArgNotNull(mapper);
    //
    //        if (iter == null || fromIndex == toIndex) {
    //            return new ArrayList<>();
    //        }
    //
    //        final List<R> result = new ArrayList<>(min(9, toIndex - fromIndex));
    //        Collection<? extends R> mr = null;
    //
    //        int idx = 0;
    //        T e = null;
    //
    //        while (iter.hasNext()) {
    //            e = iter.next();
    //
    //            if (idx++ < fromIndex) {
    //                continue;
    //            }
    //
    //            if (notEmpty(mr = mapper.apply(e))) {
    //                result.addAll(mr);
    //            }
    //
    //            if (idx >= toIndex) {
    //                break;
    //            }
    //        }
    //
    //        return result;
    //    }

    /**
     *
     *
     * @param <T>
     * @param <U>
     * @param <R>
     * @param <E>
     * @param <E2>
     * @param a
     * @param mapper
     * @param mapper2
     * @return
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, U, R, E extends Exception, E2 extends Exception> List<R> flatMap(final T[] a,
            final Throwables.Function<? super T, ? extends Collection<? extends U>, E> mapper,
            final Throwables.Function<? super U, ? extends Collection<? extends R>, E2> mapper2) throws E, E2 {

        return flatMap(a, mapper, mapper2, Factory.<R> ofList());
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <U>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param <E2>
     * @param a
     * @param mapper
     * @param mapper2
     * @param supplier
     * @return
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, U, R, C extends Collection<R>, E extends Exception, E2 extends Exception> C flatMap(final T[] a,
            final Throwables.Function<? super T, ? extends Collection<? extends U>, E> mapper,
            final Throwables.Function<? super U, ? extends Collection<? extends R>, E2> mapper2, final IntFunction<? extends C> supplier) throws E, E2 {
        checkArgNotNull(mapper);
        checkArgNotNull(mapper2);

        if (isEmpty(a)) {
            return supplier.apply(0);
        }

        final int len = a.length > MAX_ARRAY_SIZE / LOAD_FACTOR_FOR_TWO_FLAT_MAP ? MAX_ARRAY_SIZE : a.length * LOAD_FACTOR_FOR_TWO_FLAT_MAP;
        final C result = supplier.apply(len);

        for (T e : a) {
            final Collection<? extends U> c1 = mapper.apply(e);

            if (notEmpty(c1)) {
                for (U e2 : c1) {
                    final Collection<? extends R> c2 = mapper2.apply(e2);

                    if (notEmpty(c2)) {
                        result.addAll(c2);
                    }
                }
            }
        }

        return result;
    }

    /**
     *
     *
     * @param <T>
     * @param <U>
     * @param <R>
     * @param <E>
     * @param <E2>
     * @param c
     * @param mapper
     * @param mapper2
     * @return
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, U, R, E extends Exception, E2 extends Exception> List<R> flatMap(final Iterable<? extends T> c,
            final Throwables.Function<? super T, ? extends Collection<? extends U>, E> mapper,
            final Throwables.Function<? super U, ? extends Collection<? extends R>, E2> mapper2) throws E, E2 {

        return flatMap(c, mapper, mapper2, Factory.<R> ofList());
    }

    /**
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <U>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param <E2>
     * @param c
     * @param mapper
     * @param mapper2
     * @param supplier
     * @return
     * @throws E the e
     * @throws E2 the e2
     */
    public static <T, U, R, C extends Collection<R>, E extends Exception, E2 extends Exception> C flatMap(final Iterable<? extends T> c,
            final Throwables.Function<? super T, ? extends Collection<? extends U>, E> mapper,
            final Throwables.Function<? super U, ? extends Collection<? extends R>, E2> mapper2, final IntFunction<? extends C> supplier) throws E, E2 {
        checkArgNotNull(mapper);
        checkArgNotNull(mapper2);

        if (c == null) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(initSizeForFlatMap(c));

        for (T e : c) {
            final Collection<? extends U> c1 = mapper.apply(e);

            if (notEmpty(c1)) {
                for (U e2 : c1) {
                    final Collection<? extends R> c2 = mapper2.apply(e2);

                    if (notEmpty(c2)) {
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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> flatmap(final T[] a, final Throwables.Function<? super T, ? extends R[], E> mapper) throws E { //NOSONAR
        checkArgNotNull(mapper);

        if (isEmpty(a)) {
            return new ArrayList<>();
        }

        return flatmap(a, 0, a.length, mapper);
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param a
     * @param mapper
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C flatmap(final T[] a, final Throwables.Function<? super T, ? extends R[], E> mapper, //NOSONAR
            final IntFunction<? extends C> supplier) throws E {
        checkArgNotNull(mapper);

        if (isEmpty(a)) {
            return supplier.apply(0);
        }

        return flatmap(a, 0, a.length, mapper, supplier);
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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> flatmap(final T[] a, final int fromIndex, final int toIndex, //NOSONAR
            final Throwables.Function<? super T, ? extends R[], E> mapper) throws E {
        return flatmap(a, fromIndex, toIndex, mapper, Factory.ofList());
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
     * @param mapper
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C flatmap(final T[] a, final int fromIndex, final int toIndex, //NOSONAR
            final Throwables.Function<? super T, ? extends R[], E> mapper, final IntFunction<? extends C> supplier) throws E {
        checkFromToIndex(fromIndex, toIndex, len(a));
        checkArgNotNull(mapper);

        if (isEmpty(a) || fromIndex == toIndex) {
            return supplier.apply(0);
        }

        final int len = initSizeForFlatMap(toIndex - fromIndex);
        final C result = supplier.apply(len);
        R[] mr = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (notEmpty(mr = mapper.apply(a[i]))) {
                result.addAll(Arrays.asList(mr));
            }
        }

        return result;
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
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> flatmap(final Collection<? extends T> c, final int fromIndex, final int toIndex, //NOSONAR
            final Throwables.Function<? super T, ? extends R[], E> mapper) throws E {
        return flatmap(c, fromIndex, toIndex, mapper, Factory.ofList());
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
     * @param mapper
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C flatmap(final Collection<? extends T> c, final int fromIndex, final int toIndex, //NOSONAR
            final Throwables.Function<? super T, ? extends R[], E> mapper, final IntFunction<? extends C> supplier) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(mapper);

        if ((isEmpty(c) && fromIndex == 0 && toIndex == 0) || fromIndex == toIndex) {
            return supplier.apply(0);
        }

        final int len = initSizeForFlatMap(toIndex - fromIndex);
        final C result = supplier.apply(len);
        R[] mr = null;

        if (c instanceof List && c instanceof RandomAccess) {
            final List<T> list = (List<T>) c;

            for (int i = fromIndex; i < toIndex; i++) {
                if (notEmpty(mr = mapper.apply(list.get(i)))) {
                    result.addAll(Arrays.asList(mr));
                }
            }
        } else {
            int idx = 0;

            for (T e : c) {
                if (idx++ < fromIndex) {
                    continue;
                }

                if (notEmpty(mr = mapper.apply(e))) {
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
     * @param <E>
     * @param c
     * @param mapper
     * @return
     * @throws E the e
     */
    public static <T, R, E extends Exception> List<R> flatmap(final Iterable<? extends T> c, final Throwables.Function<? super T, ? extends R[], E> mapper) //NOSONAR
            throws E {
        return flatmap(c, mapper, Factory.<R> ofList());
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <C>
     * @param <E>
     * @param c
     * @param mapper
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, R, C extends Collection<R>, E extends Exception> C flatmap(final Iterable<? extends T> c, //NOSONAR
            final Throwables.Function<? super T, ? extends R[], E> mapper, final IntFunction<? extends C> supplier) throws E {
        checkArgNotNull(mapper);

        if (c == null) {
            return supplier.apply(0);
        }

        final C result = supplier.apply(initSizeForFlatMap(c));
        R[] mr = null;

        for (T e : c) {
            if (notEmpty(mr = mapper.apply(e))) {
                result.addAll(Arrays.asList(mr));
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
     * @return the list
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> takeWhile(final T[] a, final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNull(filter, "filter");

        final List<T> result = new ArrayList<>(min(9, len(a)));

        if (isEmpty(a)) {
            return result;
        }

        for (T e : a) {
            if (filter.test(e)) {
                result.add(e);
            } else {
                break;
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
     * @return the list
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> takeWhile(final Iterable<? extends T> c, final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNull(filter, "filter");

        final List<T> result = new ArrayList<>(min(9, getSizeOrDefault(c, 0)));

        if (c == null) {
            return result;
        }

        for (T e : c) {
            if (filter.test(e)) {
                result.add(e);
            } else {
                break;
            }
        }

        return result;
    }

    /**
     * Take while inclusive.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param filter
     * @return the list
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> takeWhileInclusive(final T[] a, final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNull(filter, "filter");

        final List<T> result = new ArrayList<>(min(9, len(a)));

        if (isEmpty(a)) {
            return result;
        }

        for (T e : a) {
            result.add(e);

            if (!filter.test(e)) {
                break;
            }
        }

        return result;
    }

    /**
     * Take while inclusive.
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param filter
     * @return the list
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> takeWhileInclusive(final Iterable<? extends T> c, final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNull(filter, "filter");

        final List<T> result = new ArrayList<>(min(9, getSizeOrDefault(c, 0)));

        if (c == null) {
            return result;
        }

        for (T e : c) {
            result.add(e);

            if (!filter.test(e)) {
                break;
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
     * @return the list
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> dropWhile(final T[] a, final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNull(filter, "filter");

        final List<T> result = new ArrayList<>(min(9, len(a)));

        if (isEmpty(a)) {
            return result;
        }

        final int len = a.length;
        int idx = 0;

        while (idx < len && filter.test(a[idx])) {
            idx++;
        }

        while (idx < len) {
            result.add(a[idx++]);
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param filter
     * @return the list
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> dropWhile(final Iterable<? extends T> c, final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNull(filter, "filter");

        final List<T> result = new ArrayList<>(min(9, getSizeOrDefault(c, 0)));

        if (c == null) {
            return result;
        }

        final Iterator<? extends T> iter = c.iterator();
        T e = null;

        while (iter.hasNext()) {
            e = iter.next();

            if (!filter.test(e)) {
                result.add(e);
                break;
            }
        }

        while (iter.hasNext()) {
            result.add(iter.next());
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param filter
     * @return the list
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> skipUntil(final T[] a, final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNull(filter, "filter");

        final List<T> result = new ArrayList<>(min(9, len(a)));

        if (isEmpty(a)) {
            return result;
        }

        final int len = a.length;
        int idx = 0;

        while (idx < len && !filter.test(a[idx])) {
            idx++;
        }

        while (idx < len) {
            result.add(a[idx++]);
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param filter
     * @return the list
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> skipUntil(final Iterable<? extends T> c, final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNull(filter, "filter");

        final List<T> result = new ArrayList<>(getMinSize(c));

        if (c == null) {
            return result;
        }

        final Iterator<? extends T> iter = c.iterator();
        T e = null;

        while (iter.hasNext()) {
            e = iter.next();

            if (filter.test(e)) {
                result.add(e);
                break;
            }
        }

        while (iter.hasNext()) {
            result.add(iter.next());
        }

        return result;
    }

    private static int getSizeOrDefault(final Iterable<?> c, final int defaultSize) {
        return c instanceof Collection ? ((Collection<?>) c).size() : defaultSize;
    }

    private static int getMinSize(final Iterable<?> c) {
        return min(9, getSizeOrDefault(c, 0));
    }

    private static int initSizeForFlatMap(final Iterable<?> c) {
        return c instanceof Collection ? initSizeForFlatMap(((Collection<?>) c).size()) : 0;
    }

    private static int initSizeForFlatMap(final int size) {
        return size > MAX_ARRAY_SIZE / LOAD_FACTOR_FOR_FLAT_MAP ? MAX_ARRAY_SIZE : (int) (size * LOAD_FACTOR_FOR_FLAT_MAP); // NOSONAR
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
        return removeDuplicates(a, fromIndex, toIndex);
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
        if (isEmpty(a)) {
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

        if (isEmpty(a) || fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final int initCapacity = (toIndex - fromIndex) / 2 + 1;
        final List<T> result = new ArrayList<>(initCapacity);
        final Set<Object> set = newHashSet(initCapacity);

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
    public static <T> List<T> distinct(final Iterable<? extends T> c) {
        if (c == null) {
            return new ArrayList<>();
        } else if (c instanceof Collection) {
            final Collection<T> coll = (Collection<T>) c;
            return distinct(coll, 0, coll.size());
        } else {
            final List<T> result = new ArrayList<>();
            final Set<Object> set = new HashSet<>();

            for (T e : c) {
                if (set.add(hashKey(e))) {
                    result.add(e);
                }
            }

            return result;
        }
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

        if ((isEmpty(c) && fromIndex == 0 && toIndex == 0) || fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final int initCapacity = (toIndex - fromIndex) / 2 + 1;
        final List<T> result = new ArrayList<>(initCapacity);
        final Set<Object> set = newHashSet(initCapacity);
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
        if (isEmpty(a)) {
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

        if (isEmpty(a) || fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final int initCapacity = (toIndex - fromIndex) / 2 + 1;
        final List<T> result = new ArrayList<>(initCapacity);
        final Set<Object> set = newHashSet(initCapacity);

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
     * @param <C>
     * @param <E>
     * @param a
     * @param keyMapper don't change value of the input parameter.
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, C extends Collection<T>, E extends Exception> C distinctBy(final T[] a, final Throwables.Function<? super T, ?, E> keyMapper,
            final Supplier<C> supplier) throws E {
        if (isEmpty(a)) {
            return supplier.get();
        }

        final C result = supplier.get();
        final Set<Object> set = newHashSet(len(a) / 2 + 1);

        for (T e : a) {
            if (set.add(hashKey(keyMapper.apply(e)))) {
                result.add(e);
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
     * @param fromIndex
     * @param toIndex
     * @param keyMapper don't change value of the input parameter.
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> List<T> distinctBy(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ?, E> keyMapper) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));

        if ((isEmpty(c) && fromIndex == 0 && toIndex == 0) || fromIndex == toIndex) {
            return new ArrayList<>();
        }

        final int initCapacity = (toIndex - fromIndex) / 2 + 1;
        final List<T> result = new ArrayList<>(initCapacity);
        final Set<Object> set = newHashSet(initCapacity);
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
    public static <T, E extends Exception> List<T> distinctBy(final Iterable<? extends T> c, final Throwables.Function<? super T, ?, E> keyMapper) throws E {
        return distinctBy(c, keyMapper, Suppliers.ofList());
    }

    /**
     * Distinct by the value mapped from <code>keyMapper</code>.
     *
     * Mostly it's designed for one-step operation to complete the operation in one step.
     * <code>java.util.stream.Stream</code> is preferred for multiple phases operation.
     *
     * @param <T>
     * @param <C>
     * @param <E>
     * @param c
     * @param keyMapper don't change value of the input parameter.
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, C extends Collection<T>, E extends Exception> C distinctBy(final Iterable<? extends T> c,
            final Throwables.Function<? super T, ?, E> keyMapper, final Supplier<C> supplier) throws E {
        if (c == null) {
            return supplier.get();
        }

        final C result = supplier.get();
        final Set<Object> set = newHashSet(c instanceof Collection ? ((Collection<T>) c).size() / 2 + 1 : 0);

        for (T e : c) {
            if (set.add(hashKey(keyMapper.apply(e)))) {
                result.add(e);
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
    public static <T, E extends Exception> boolean allMatch(final T[] a, final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
            return true;
        }

        for (T e : a) {
            if (!filter.test(e)) {
                return false;
            }
        }

        return true;
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
    public static <T, E extends Exception> boolean allMatch(final Iterable<? extends T> c, final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNull(filter, "filter");

        if (c == null) {
            return true;
        }

        for (T e : c) {
            if (!filter.test(e)) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param filter
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> boolean allMatch(final Iterator<? extends T> iter, final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNull(filter, "filter");

        if (iter == null) {
            return true;
        }

        while (iter.hasNext()) {
            if (!filter.test(iter.next())) {
                return false;
            }
        }

        return true;
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
    public static <T, E extends Exception> boolean anyMatch(final T[] a, final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
            return false;
        }

        for (T e : a) {
            if (filter.test(e)) {
                return true;
            }
        }

        return false;
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
    public static <T, E extends Exception> boolean anyMatch(final Iterable<? extends T> c, final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNull(filter, "filter");

        if (c == null) {
            return false;
        }

        for (T e : c) {
            if (filter.test(e)) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param filter
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> boolean anyMatch(final Iterator<? extends T> iter, final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNull(filter, "filter");

        if (iter == null) {
            return false;
        }

        while (iter.hasNext()) {
            if (filter.test(iter.next())) {
                return true;
            }
        }

        return false;
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
    public static <T, E extends Exception> boolean noneMatch(final T[] a, final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
            return true;
        }

        for (T e : a) {
            if (filter.test(e)) {
                return false;
            }
        }

        return true;
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
    public static <T, E extends Exception> boolean noneMatch(final Iterable<? extends T> c, final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNull(filter, "filter");

        if (c == null) {
            return true;
        }

        for (T e : c) {
            if (filter.test(e)) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param filter
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> boolean noneMatch(final Iterator<? extends T> iter, final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNull(filter, "filter");

        if (iter == null) {
            return true;
        }

        while (iter.hasNext()) {
            if (filter.test(iter.next())) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param atLeast
     * @param atMost
     * @param filter
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> boolean nMatch(final T[] a, final int atLeast, final int atMost, final Throwables.Predicate<? super T, E> filter)
            throws E {
        checkArgNotNegative(atLeast, "atLeast"); //NOSONAR
        checkArgNotNegative(atMost, "atMost"); //NOSONAR
        checkArgument(atLeast <= atMost, "'atLeast' must be <= 'atMost'"); //NOSONAR
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
            return atLeast == 0;
        }

        final int len = len(a);

        if (len < atLeast) {
            return false;
        }

        long cnt = 0;

        for (int i = 0; i < len; i++) {
            if (filter.test(a[i]) && (++cnt > atMost)) {
                return false;
            }
        }

        return cnt >= atLeast && cnt <= atMost;
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param atLeast
     * @param atMost
     * @param filter
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> boolean nMatch(final Iterable<? extends T> c, final int atLeast, final int atMost,
            final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNegative(atLeast, "atLeast");
        checkArgNotNegative(atMost, "atMost");
        checkArgument(atLeast <= atMost, "'atLeast' must be <= 'atMost'");
        checkArgNotNull(filter, "filter");

        if (c == null) {
            return atLeast == 0;
        }

        return nMatch(c.iterator(), atLeast, atMost, filter);
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param atLeast
     * @param atMost
     * @param filter
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> boolean nMatch(final Iterator<? extends T> iter, final int atLeast, final int atMost,
            final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNegative(atLeast, "atLeast");
        checkArgNotNegative(atMost, "atMost");
        checkArgument(atLeast <= atMost, "'atLeast' must be <= 'atMost'");
        checkArgNotNull(filter, "filter");

        if (iter == null) {
            return atLeast == 0;
        }

        long cnt = 0;

        while (iter.hasNext()) {
            if (filter.test(iter.next()) && (++cnt > atMost)) {
                return false;
            }
        }

        return cnt >= atLeast && cnt <= atMost;
    }

    /**
     *
     *
     * @param a
     * @return
     */
    public static boolean allTrue(final boolean[] a) {
        if (isEmpty(a)) {
            return true;
        }

        for (boolean b : a) {
            if (!b) {
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
    public static boolean allFalse(final boolean[] a) {
        if (isEmpty(a)) {
            return true;
        }

        for (boolean b : a) {
            if (b) {
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
    public static boolean anyTrue(final boolean[] a) {
        if (isEmpty(a)) {
            return false;
        }

        for (boolean b : a) {
            if (b) {
                return true;
            }
        }

        return false;
    }

    /**
     *
     *
     * @param a
     * @return
     */
    public static boolean anyFalse(final boolean[] a) {
        if (isEmpty(a)) {
            return false;
        }

        for (boolean b : a) {
            if (!b) {
                return true;
            }
        }

        return false;
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
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
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
        checkArgNotNull(filter, "filter");

        if (isEmpty(a) || fromIndex == toIndex) {
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
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
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
        checkArgNotNull(filter, "filter");

        if (isEmpty(a) || fromIndex == toIndex) {
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
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
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
        checkArgNotNull(filter, "filter");

        if (isEmpty(a) || fromIndex == toIndex) {
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
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
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
        checkArgNotNull(filter, "filter");

        if (isEmpty(a) || fromIndex == toIndex) {
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
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
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
        checkArgNotNull(filter, "filter");

        if (isEmpty(a) || fromIndex == toIndex) {
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
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
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
        checkArgNotNull(filter, "filter");

        if (isEmpty(a) || fromIndex == toIndex) {
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
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
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
        checkArgNotNull(filter, "filter");

        if (isEmpty(a) || fromIndex == toIndex) {
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
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
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
        checkArgNotNull(filter, "filter");

        if (isEmpty(a) || fromIndex == toIndex) {
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
        checkArgNotNull(filter, "filter");

        if (isEmpty(a)) {
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
        checkArgNotNull(filter, "filter");

        if (isEmpty(a) || fromIndex == toIndex) {
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
    public static <T, E extends Exception> int count(final Iterable<? extends T> c, final Throwables.Predicate<? super T, E> filter) throws E {
        checkArgNotNull(filter, "filter");

        if (c == null) {
            return 0;
        }

        int count = 0;

        for (T e : c) {
            if (filter.test(e)) {
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
     * @param fromIndex
     * @param toIndex
     * @param filter
     * @return
     * @throws E the e
     */
    public static <T, E extends Exception> int count(final Collection<? extends T> c, final int fromIndex, final int toIndex,
            final Throwables.Predicate<? super T, E> filter) throws E {
        checkFromToIndex(fromIndex, toIndex, size(c));
        checkArgNotNull(filter, "filter");

        if ((isEmpty(c) && fromIndex == 0 && toIndex == 0) || (fromIndex == toIndex && fromIndex < c.size())) {
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
     * @param iter
     * @return
     * @throws ArithmeticException if the total {@code count} overflows an {@code int}.
     */
    public static int count(final Iterator<?> iter) throws ArithmeticException {
        if (iter == null) {
            return 0;
        }

        long res = 0;

        while (iter.hasNext()) {
            iter.next();
            res++;
        }

        return Numbers.toIntExact(res);
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param filter
     * @return
     * @throws ArithmeticException if the total matched {@code count} overflows an {@code int}.
     * @throws E
     */
    public static <T, E extends Exception> int count(final Iterator<? extends T> iter, final Throwables.Predicate<? super T, E> filter)
            throws ArithmeticException, E {
        checkArgNotNull(filter, "filter");

        if (iter == null) {
            return 0;
        }

        long res = 0;

        while (iter.hasNext()) {
            if (filter.test(iter.next())) {
                res++;
            }
        }

        return Numbers.toIntExact(res);
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
    public static <T, E extends Exception> List<T> merge(final T[] a, final T[] b,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) throws E {
        if (isEmpty(a)) {
            return isEmpty(b) ? new ArrayList<>() : asList(b);
        } else if (isEmpty(b)) {
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
    public static <T, E extends Exception> List<T> merge(final Iterable<? extends T> a, final Iterable<? extends T> b,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) throws E {
        if (a == null) {
            return b == null ? new ArrayList<>() : (b instanceof Collection ? new ArrayList<>((Collection<T>) b) : toList(b.iterator()));
        } else if (b == null) {
            return (a instanceof Collection ? new ArrayList<>((Collection<T>) a) : toList(a.iterator()));
        }

        final List<T> result = new ArrayList<>(getSizeOrDefault(a, 0) + getSizeOrDefault(b, 0));
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
     *
     * @param <T>
     * @param c
     * @param nextSelector
     * @return
     */
    public static <T> List<T> merge(final Collection<? extends Iterable<? extends T>> c, final BiFunction<? super T, ? super T, MergeResult> nextSelector) {
        return merge(c, nextSelector, Factory.ofList());
    }

    /**
     *
     *
     * @param <T>
     * @param <C>
     * @param c
     * @param nextSelector
     * @param supplier
     * @return
     */
    public static <T, C extends Collection<T>> C merge(final Collection<? extends Iterable<? extends T>> c,
            final BiFunction<? super T, ? super T, MergeResult> nextSelector, final IntFunction<? extends C> supplier) {
        int size = 0;

        for (Iterable<? extends T> e : c) {
            size += getSizeOrDefault(e, 0);
        }

        final int totalCount = size;

        final Supplier<? extends C> tmp = () -> supplier.apply(totalCount);

        return toCollection(Iterators.mergeIterables(c, nextSelector), tmp);
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param a
    //     * @param b
    //     * @param nextSelector
    //     * @return
    //     * @see {@code Iterators.merge(Iterator, Iterator, BiFunction)}
    //     */
    //    public static <T, E extends Exception> List<T> merge(final Iterator<? extends T> a, final Iterator<? extends T> b,
    //            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) throws E {
    //        if (a == null) {
    //            return b == null ? new ArrayList<>() : toList(b);
    //        } else if (b == null) {
    //            return toList(a);
    //        }
    //
    //        final List<T> result = new ArrayList<>(9);
    //        final Iterator<? extends T> iterA = a;
    //        final Iterator<? extends T> iterB = b;
    //
    //        T nextA = null;
    //        T nextB = null;
    //        boolean hasNextA = false;
    //        boolean hasNextB = false;
    //
    //        while (hasNextA || hasNextB || iterA.hasNext() || iterB.hasNext()) {
    //            if (hasNextA) {
    //                if (iterB.hasNext()) {
    //                    if (nextSelector.apply(nextA, (nextB = iterB.next())) == MergeResult.TAKE_FIRST) {
    //                        hasNextA = false;
    //                        hasNextB = true;
    //                        result.add(nextA);
    //                    } else {
    //                        result.add(nextB);
    //                    }
    //                } else {
    //                    hasNextA = false;
    //                    result.add(nextA);
    //                }
    //            } else if (hasNextB) {
    //                if (iterA.hasNext()) {
    //                    if (nextSelector.apply((nextA = iterA.next()), nextB) == MergeResult.TAKE_FIRST) {
    //                        result.add(nextA);
    //                    } else {
    //                        hasNextA = true;
    //                        hasNextB = false;
    //                        result.add(nextB);
    //                    }
    //                } else {
    //                    hasNextB = false;
    //                    result.add(nextB);
    //                }
    //            } else if (iterA.hasNext()) {
    //                if (iterB.hasNext()) {
    //                    if (nextSelector.apply((nextA = iterA.next()), (nextB = iterB.next())) == MergeResult.TAKE_FIRST) {
    //                        hasNextB = true;
    //                        result.add(nextA);
    //                    } else {
    //                        hasNextA = true;
    //                        result.add(nextB);
    //                    }
    //                } else {
    //                    result.add(iterA.next());
    //                }
    //            } else {
    //                result.add(iterB.next());
    //            }
    //        }
    //
    //        return result;
    //    }

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
    public static <A, B, R, E extends Exception> List<R> zip(final A[] a, final B[] b,
            final Throwables.BiFunction<? super A, ? super B, ? extends R, E> zipFunction) throws E {
        checkArgNotNull(zipFunction);

        if (isEmpty(a) || isEmpty(b)) {
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
    public static <A, B, R, E extends Exception> List<R> zip(final Iterable<A> a, final Iterable<B> b,
            final Throwables.BiFunction<? super A, ? super B, ? extends R, E> zipFunction) throws E {
        checkArgNotNull(zipFunction);

        if (a == null || b == null) {
            return new ArrayList<>();
        }

        final Iterator<A> iterA = a.iterator();
        final Iterator<B> iterB = b.iterator();
        final int minLen = min(getSizeOrDefault(a, 0), getSizeOrDefault(b, 0));
        final List<R> result = new ArrayList<>(minLen);

        while (iterA.hasNext() && iterB.hasNext()) {
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
            final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends R, E> zipFunction) throws E {
        checkArgNotNull(zipFunction);

        if (isEmpty(a) || isEmpty(b) || isEmpty(c)) {
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
    public static <A, B, C, R, E extends Exception> List<R> zip(final Iterable<A> a, final Iterable<B> b, final Iterable<C> c,
            final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends R, E> zipFunction) throws E {
        checkArgNotNull(zipFunction);

        if (a == null || b == null || c == null) {
            return new ArrayList<>();
        }

        final Iterator<A> iterA = a.iterator();
        final Iterator<B> iterB = b.iterator();
        final Iterator<C> iterC = c.iterator();
        final int minLen = min(getSizeOrDefault(a, 0), getSizeOrDefault(b, 0), getSizeOrDefault(c, 0));
        final List<R> result = new ArrayList<>(minLen);

        while (iterA.hasNext() && iterB.hasNext() && iterC.hasNext()) {
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
            final Throwables.BiFunction<? super A, ? super B, ? extends R, E> zipFunction) throws E {
        checkArgNotNull(zipFunction);

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
    public static <A, B, R, E extends Exception> List<R> zip(final Iterable<A> a, final Iterable<B> b, final A valueForNoneA, final B valueForNoneB,
            final Throwables.BiFunction<? super A, ? super B, ? extends R, E> zipFunction) throws E {
        checkArgNotNull(zipFunction);

        final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a.iterator();
        final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b.iterator();
        final int lenA = getSizeOrDefault(a, 0);
        final int lenB = getSizeOrDefault(b, 0);
        final int maxLen = max(lenA, lenB);
        final List<R> result = new ArrayList<>(maxLen);

        if (a == null || a instanceof Collection) {
            if (b == null || b instanceof Collection) {
                for (int i = 0; i < maxLen; i++) {
                    result.add(zipFunction.apply(i < lenA ? iterA.next() : valueForNoneA, i < lenB ? iterB.next() : valueForNoneB));
                }
            } else {
                for (int i = 0; i < lenA || iterB.hasNext(); i++) {
                    result.add(zipFunction.apply(i < lenA ? iterA.next() : valueForNoneA, iterB.hasNext() ? iterB.next() : valueForNoneB));
                }
            }
        } else if (b == null || b instanceof Collection) {
            for (int i = 0; i < lenB || iterA.hasNext(); i++) {
                result.add(zipFunction.apply(iterA.hasNext() ? iterA.next() : valueForNoneA, i < lenB ? iterB.next() : valueForNoneB));
            }
        } else {
            while (iterA.hasNext() || iterB.hasNext()) {
                result.add(zipFunction.apply(iterA.hasNext() ? iterA.next() : valueForNoneA, iterB.hasNext() ? iterB.next() : valueForNoneB));
            }
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
            final C valueForNoneC, final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends R, E> zipFunction) throws E {
        checkArgNotNull(zipFunction);

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
    public static <A, B, C, R, E extends Exception> List<R> zip(final Iterable<A> a, final Iterable<B> b, final Iterable<C> c, final A valueForNoneA,
            final B valueForNoneB, final C valueForNoneC, final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends R, E> zipFunction) throws E {
        checkArgNotNull(zipFunction);

        final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a.iterator();
        final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b.iterator();
        final Iterator<C> iterC = c == null ? ObjIterator.<C> empty() : c.iterator();
        final int lenA = getSizeOrDefault(a, 0);
        final int lenB = getSizeOrDefault(b, 0);
        final int lenC = getSizeOrDefault(c, 0);
        final int maxLen = max(lenA, lenB, lenC);
        final List<R> result = new ArrayList<>(maxLen);

        if (a == null || a instanceof Collection) {
            if (b == null || b instanceof Collection) {
                if (c == null || c instanceof Collection) {
                    for (int i = 0; i < maxLen; i++) {
                        result.add(zipFunction.apply(i < lenA ? iterA.next() : valueForNoneA, i < lenB ? iterB.next() : valueForNoneB,
                                i < lenC ? iterC.next() : valueForNoneC));
                    }
                } else {
                    for (int i = 0; i < lenA || i < lenB || iterC.hasNext(); i++) {
                        result.add(zipFunction.apply(i < lenA ? iterA.next() : valueForNoneA, i < lenB ? iterB.next() : valueForNoneB,
                                iterC.hasNext() ? iterC.next() : valueForNoneC));
                    }
                }
            } else {
                if (c == null || c instanceof Collection) {
                    for (int i = 0; i < lenA || i < lenC || iterB.hasNext(); i++) {
                        result.add(zipFunction.apply(i < lenA ? iterA.next() : valueForNoneA, iterB.hasNext() ? iterB.next() : valueForNoneB,
                                i < lenC ? iterC.next() : valueForNoneC));
                    }
                } else {
                    for (int i = 0; i < lenA || iterB.hasNext() || iterC.hasNext(); i++) {
                        result.add(zipFunction.apply(i < lenA ? iterA.next() : valueForNoneA, iterB.hasNext() ? iterB.next() : valueForNoneB,
                                iterC.hasNext() ? iterC.next() : valueForNoneC));
                    }
                }
            }
        } else if (b == null || b instanceof Collection) {
            if (c == null || c instanceof Collection) {
                for (int i = 0; i < lenB || i < lenC || iterA.hasNext(); i++) {
                    result.add(zipFunction.apply(iterA.hasNext() ? iterA.next() : valueForNoneA, i < lenB ? iterB.next() : valueForNoneB,
                            i < lenC ? iterC.next() : valueForNoneC));
                }
            } else {
                for (int i = 0; i < lenB || iterA.hasNext() || iterC.hasNext(); i++) {
                    result.add(zipFunction.apply(iterA.hasNext() ? iterA.next() : valueForNoneA, i < lenB ? iterB.next() : valueForNoneB,
                            iterC.hasNext() ? iterC.next() : valueForNoneC));
                }
            }
        } else {
            if (c == null || c instanceof Collection) {
                for (int i = 0; i < lenC || iterA.hasNext() || iterB.hasNext(); i++) {
                    result.add(zipFunction.apply(iterA.hasNext() ? iterA.next() : valueForNoneA, iterB.hasNext() ? iterB.next() : valueForNoneB,
                            i < lenC ? iterC.next() : valueForNoneC));
                }
            } else {
                while (iterA.hasNext() || iterB.hasNext() || iterC.hasNext()) {
                    result.add(zipFunction.apply(iterA.hasNext() ? iterA.next() : valueForNoneA, iterB.hasNext() ? iterB.next() : valueForNoneB,
                            iterC.hasNext() ? iterC.next() : valueForNoneC));
                }
            }
        }

        return result;
    }

    //    /**
    //     *
    //     * @param <A>
    //     * @param <B>
    //     * @param <R>
    //     * @param a
    //     * @param b
    //     * @param zipFunction
    //     * @return
    //     * @see {@code Iterators.zip(Iterator, Iterator, BiFunction)}
    //     */
    //    public static <A, B, R, E extends Exception> List<R> zip(final Iterator<A> a, final Iterator<B> b,
    //            final Throwables.BiFunction<? super A, ? super B, ? extends R, E> zipFunction) throws E {
    //        checkArgNotNull(zipFunction);
    //
    //        if (a == null || b == null) {
    //            return new ArrayList<>();
    //        }
    //
    //        final Iterator<A> iterA = a;
    //        final Iterator<B> iterB = b;
    //        final List<R> result = new ArrayList<>(9);
    //
    //        while (iterA.hasNext() && iterB.hasNext()) {
    //            result.add(zipFunction.apply(iterA.next(), iterB.next()));
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     *
    //     * @param <A>
    //     * @param <B>
    //     * @param <C>
    //     * @param <R>
    //     * @param a
    //     * @param b
    //     * @param c
    //     * @param zipFunction
    //     * @return
    //     * @see {@code Iterators.zip(Iterator, Iterator, Iterator, TriFunction)}
    //     */
    //    public static <A, B, C, R, E extends Exception> List<R> zip(final Iterator<A> a, final Iterator<B> b, final Iterator<C> c,
    //            final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends R, E> zipFunction) throws E {
    //        checkArgNotNull(zipFunction);
    //
    //        if (a == null || b == null || c == null) {
    //            return new ArrayList<>();
    //        }
    //
    //        final Iterator<A> iterA = a;
    //        final Iterator<B> iterB = b;
    //        final Iterator<C> iterC = c;
    //        final List<R> result = new ArrayList<>(9);
    //
    //        while (iterA.hasNext() && iterB.hasNext() && iterC.hasNext()) {
    //            result.add(zipFunction.apply(iterA.next(), iterB.next(), iterC.next()));
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     *
    //     * @param <A>
    //     * @param <B>
    //     * @param <R>
    //     * @param a
    //     * @param b
    //     * @param valueForNoneA
    //     * @param valueForNoneB
    //     * @param zipFunction
    //     * @return
    //     * @see {@code Iterators.zip(Iterator, Iterator, Object, Object, BiFunction)}
    //     */
    //    public static <A, B, R, E extends Exception> List<R> zip(final Iterator<A> a, final Iterator<B> b, final A valueForNoneA, final B valueForNoneB,
    //            final Throwables.BiFunction<? super A, ? super B, ? extends R, E> zipFunction) throws E {
    //        checkArgNotNull(zipFunction);
    //
    //        final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a;
    //        final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b;
    //
    //        final List<R> result = new ArrayList<>(9);
    //        boolean hasA = true;
    //
    //        do {
    //            if (hasA && (hasA = iterA.hasNext())) {
    //                result.add(zipFunction.apply(iterA.next(), iterB.hasNext() ? iterB.next() : valueForNoneB));
    //            } else if (iterB.hasNext()) {
    //                result.add(zipFunction.apply(valueForNoneA, iterB.next()));
    //            } else {
    //                break;
    //            }
    //        } while (true);
    //
    //        return result;
    //    }
    //
    //    /**
    //     *
    //     * @param <A>
    //     * @param <B>
    //     * @param <C>
    //     * @param <R>
    //     * @param a
    //     * @param b
    //     * @param c
    //     * @param valueForNoneA
    //     * @param valueForNoneB
    //     * @param valueForNoneC
    //     * @param zipFunction
    //     * @return
    //     * @see {@code Iterators.zip(Iterator, Iterator, Iterator, Object, Object, Object, TriFunction)}
    //     */
    //    public static <A, B, C, R, E extends Exception> List<R> zip(final Iterator<A> a, final Iterator<B> b, final Iterator<C> c, final A valueForNoneA,
    //            final B valueForNoneB, final C valueForNoneC, final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends R, E> zipFunction) throws E {
    //        checkArgNotNull(zipFunction);
    //
    //        final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a;
    //        final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b;
    //        final Iterator<C> iterC = c == null ? ObjIterator.<C> empty() : c;
    //
    //        final List<R> result = new ArrayList<>(9);
    //        boolean hasA = true;
    //        boolean hasB = true;
    //
    //        do {
    //            if (hasA && (hasA = iterA.hasNext())) {
    //                result.add(zipFunction.apply(iterA.next(), iterB.hasNext() ? iterB.next() : valueForNoneB, iterC.hasNext() ? iterC.next() : valueForNoneC));
    //            } else if (hasB && (hasB = iterB.hasNext())) {
    //                result.add(zipFunction.apply(valueForNoneA, iterB.next(), iterC.hasNext() ? iterC.next() : valueForNoneC));
    //            } else if (iterC.hasNext()) {
    //                result.add(zipFunction.apply(valueForNoneA, valueForNoneB, iterC.hasNext() ? iterC.next() : valueForNoneC));
    //            } else {
    //                break;
    //            }
    //        } while (true);
    //
    //        return result;
    //    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <R>
     * @param <E>
     * @param targetElementType
     * @param a
     * @param b
     * @param zipFunction
     * @return
     * @throws E the e
     */
    public static <A, B, R, E extends Exception> R[] zip(final Class<R> targetElementType, final A[] a, final B[] b,
            final Throwables.BiFunction<? super A, ? super B, ? extends R, E> zipFunction) throws E {
        checkArgNotNull(zipFunction);

        final int lenA = len(a);
        final int lenB = len(b);
        final int minLen = min(lenA, lenB);

        final R[] result = newArray(targetElementType, minLen);

        for (int i = 0; i < minLen; i++) {
            result[i] = zipFunction.apply(a[i], b[i]);
        }

        return result;
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <R>
     * @param <E>
     * @param targetElementType
     * @param a
     * @param b
     * @param valueForNoneA
     * @param valueForNoneB
     * @param zipFunction
     * @return
     * @throws E the e
     */
    public static <A, B, R, E extends Exception> R[] zip(final Class<R> targetElementType, final A[] a, final B[] b, final A valueForNoneA,
            final B valueForNoneB, final Throwables.BiFunction<? super A, ? super B, ? extends R, E> zipFunction) throws E {
        checkArgNotNull(zipFunction);

        final int lenA = len(a);
        final int lenB = len(b);
        final int minLen = min(lenA, lenB);
        final int maxLen = max(lenA, lenB);

        final R[] result = newArray(targetElementType, maxLen);

        for (int i = 0; i < minLen; i++) {
            result[i] = zipFunction.apply(a[i], b[i]);
        }

        if (lenA < maxLen) {
            for (int i = lenA; i < maxLen; i++) {
                result[i] = zipFunction.apply(valueForNoneA, b[i]);
            }
        } else if (lenB < maxLen) {
            for (int i = lenB; i < maxLen; i++) {
                result[i] = zipFunction.apply(a[i], valueForNoneB);
            }
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
     * @param targetElementType
     * @param a
     * @param b
     * @param c
     * @param zipFunction
     * @return
     * @throws E the e
     */
    public static <A, B, C, R, E extends Exception> R[] zip(final Class<R> targetElementType, final A[] a, final B[] b, final C[] c,
            final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends R, E> zipFunction) throws E {
        checkArgNotNull(zipFunction);

        final int lenA = len(a);
        final int lenB = len(b);
        final int lenC = len(c);
        final int minLen = min(lenA, lenB, lenC);

        final R[] result = newArray(targetElementType, minLen);

        for (int i = 0; i < minLen; i++) {
            result[i] = zipFunction.apply(a[i], b[i], c[i]);
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
     * @param targetElementType
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
    public static <A, B, C, R, E extends Exception> R[] zip(final Class<R> targetElementType, final A[] a, final B[] b, final C[] c, final A valueForNoneA,
            final B valueForNoneB, final C valueForNoneC, final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends R, E> zipFunction) throws E {
        checkArgNotNull(zipFunction);

        final int lenA = len(a);
        final int lenB = len(b);
        final int lenC = len(c);
        final int minLen = min(lenA, lenB, lenC);
        final int maxLen = max(lenA, lenB, lenC);

        final R[] result = newArray(targetElementType, maxLen);

        for (int i = 0; i < minLen; i++) {
            result[i] = zipFunction.apply(a[i], b[i], c[i]);
        }

        if (minLen < maxLen) {
            for (int i = minLen; i < maxLen; i++) {
                result[i] = zipFunction.apply(i < lenA ? a[i] : valueForNoneA, i < lenB ? b[i] : valueForNoneB, i < lenC ? c[i] : valueForNoneC);
            }
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <A>
     * @param <B>
     * @param <E>
     * @param c
     * @param unzip the second parameter is an output parameter.
     * @return
     * @throws E the e
     */
    public static <T, A, B, E extends Exception> Pair<List<A>, List<B>> unzip(final Iterable<? extends T> c,
            final Throwables.BiConsumer<? super T, Pair<A, B>, E> unzip) throws E {
        return unzip(c, unzip, Factory.ofList());
    }

    /**
     *
     * @param <T>
     * @param <A>
     * @param <B>
     * @param <LC>
     * @param <RC>
     * @param <E>
     * @param c
     * @param unzip the second parameter is an output parameter.
     * @param supplier
     * @return
     * @throws E the e
     */
    public static <T, A, B, LC extends Collection<A>, RC extends Collection<B>, E extends Exception> Pair<LC, RC> unzip(final Iterable<? extends T> c,
            final Throwables.BiConsumer<? super T, Pair<A, B>, E> unzip, final IntFunction<? extends Collection<?>> supplier) throws E {
        checkArgNotNull(unzip);

        final int len = getSizeOrDefault(c, 0);

        final LC l = (LC) supplier.apply(len);
        final RC r = (RC) supplier.apply(len);
        final Pair<A, B> p = new Pair<>();

        if (c != null) {
            for (T e : c) {
                unzip.accept(e, p);

                l.add(p.left);
                r.add(p.right);
            }
        }

        return Pair.of(l, r);
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param <L>
    //     * @param <R>
    //     * @param iter
    //     * @param unzip the second parameter is an output parameter.
    //     * @return
    //     * @see {@code Iterators.unzip(Iterator, BiConsumer)}
    //     */
    //    public static <T, L, R, E extends Exception> Pair<List<L>, List<R>> unzip(final Iterator<? extends T> iter,
    //            final Throwables.BiConsumer<? super T, Pair<L, R>, E> unzip) throws E {
    //        checkArgNotNull(unzip);
    //
    //        final int len = 9;
    //
    //        final List<L> l = new ArrayList<>(len);
    //        final List<R> r = new ArrayList<>(len);
    //        final Pair<L, R> p = new Pair<>();
    //
    //        if (iter != null) {
    //            T e = null;
    //
    //            while (iter.hasNext()) {
    //                e = iter.next();
    //
    //                unzip.accept(e, p);
    //
    //                l.add(p.left);
    //                r.add(p.right);
    //            }
    //        }
    //
    //        return Pair.of(l, r);
    //    }

    /**
     *
     * @param <T>
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <E>
     * @param c
     * @param unzip the second parameter is an output parameter.
     * @return
     * @throws E the e
     */
    public static <T, A, B, C, E extends Exception> Triple<List<A>, List<B>, List<C>> unzipp(final Iterable<? extends T> c,
            final Throwables.BiConsumer<? super T, Triple<A, B, C>, E> unzip) throws E {
        return unzipp(c, unzip, Factory.ofList());
    }

    /**
     *
     * @param <T>
     * @param <A>
     * @param <B>
     * @param <C>
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
    public static <T, A, B, C, LC extends Collection<A>, MC extends Collection<B>, RC extends Collection<C>, E extends Exception> Triple<LC, MC, RC> unzipp(
            final Iterable<? extends T> c, final Throwables.BiConsumer<? super T, Triple<A, B, C>, E> unzip,
            final IntFunction<? extends Collection<?>> supplier) throws E {
        checkArgNotNull(unzip);

        final int len = getSizeOrDefault(c, 0);

        final LC l = (LC) supplier.apply(len);
        final MC m = (MC) supplier.apply(len);
        final RC r = (RC) supplier.apply(len);
        final Triple<A, B, C> t = new Triple<>();

        if (c != null) {
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
     *
     * @param <K>
     * @param <T>
     * @param <E>
     * @param a
     * @param keyExtractor
     * @return
     * @throws E
     */
    @Beta
    public static <K, T, E extends Exception> Map<K, List<T>> groupBy(final T[] a, final Throwables.Function<? super T, ? extends K, E> keyExtractor) throws E {
        return groupBy(a, keyExtractor, Suppliers.ofMap());
    }

    /**
     *
     *
     * @param <K>
     * @param <T>
     * @param <M>
     * @param <E>
     * @param a
     * @param keyExtractor
     * @param mapSupplier
     * @return
     * @throws E
     */
    @Beta
    public static <K, T, M extends Map<K, List<T>>, E extends Exception> M groupBy(final T[] a,
            final Throwables.Function<? super T, ? extends K, E> keyExtractor, final Supplier<M> mapSupplier) throws E {
        return groupBy(a, 0, len(a), keyExtractor, mapSupplier);
    }

    /**
     *
     * @param <K>
     * @param <T>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param keyExtractor
     * @return
     * @throws E
     */
    @Beta
    public static <K, T, E extends Exception> Map<K, List<T>> groupBy(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ? extends K, E> keyExtractor) throws E {
        return groupBy(a, fromIndex, toIndex, keyExtractor, Suppliers.ofMap());
    }

    /**
     *
     *
     * @param <K>
     * @param <T>
     * @param <M>
     * @param <E>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @param keyExtractor
     * @param mapSupplier
     * @return
     * @throws E
     */
    @Beta
    public static <K, T, M extends Map<K, List<T>>, E extends Exception> M groupBy(final T[] a, final int fromIndex, final int toIndex,
            final Throwables.Function<? super T, ? extends K, E> keyExtractor, final Supplier<M> mapSupplier) throws E {
        final int length = len(a);

        checkFromToIndex(fromIndex, toIndex, length);
        checkArgNotNull(keyExtractor, "keyExtractor"); //NOSONAR
        checkArgNotNull(mapSupplier, "mapSupplier"); //NOSONAR

        final M ret = mapSupplier.get();
        K key = null;
        List<T> val = null;

        for (int i = fromIndex; i < toIndex; i++) {
            key = keyExtractor.apply(a[i]);
            val = ret.get(key);

            if (val == null) {
                val = new ArrayList<>();

                ret.put(key, val);
            }

            val.add(a[i]);
        }

        return ret;
    }

    /**
     *
     *
     * @param <K>
     * @param <T>
     * @param <E>
     * @param iter
     * @param keyExtractor
     * @return
     * @throws E
     */
    @Beta
    public static <K, T, E extends Exception> Map<K, List<T>> groupBy(final Iterable<? extends T> iter,
            final Throwables.Function<? super T, ? extends K, E> keyExtractor) throws E {
        return groupBy(iter, keyExtractor, Suppliers.ofMap());
    }

    /**
     *
     *
     * @param <K>
     * @param <T>
     * @param <M>
     * @param <E>
     * @param iter
     * @param keyExtractor
     * @param mapSupplier
     * @return
     * @throws E
     */
    @Beta
    public static <K, T, M extends Map<K, List<T>>, E extends Exception> M groupBy(final Iterable<? extends T> iter,
            final Throwables.Function<? super T, ? extends K, E> keyExtractor, final Supplier<M> mapSupplier) throws E {
        checkArgNotNull(keyExtractor, "keyExtractor");
        checkArgNotNull(mapSupplier, "mapSupplier");

        final M ret = mapSupplier.get();

        if (iter == null) {
            return ret;
        }

        K key = null;
        List<T> val = null;

        for (T e : iter) {
            key = keyExtractor.apply(e);
            val = ret.get(key);

            if (val == null) {
                val = new ArrayList<>();

                ret.put(key, val);
            }

            val.add(e);
        }

        return ret;
    }

    /**
     *
     *
     * @param <K>
     * @param <T>
     * @param <E>
     * @param iter
     * @param keyExtractor
     * @return
     * @throws E
     */
    @Beta
    public static <K, T, E extends Exception> Map<K, List<T>> groupBy(final Iterator<? extends T> iter,
            final Throwables.Function<? super T, ? extends K, E> keyExtractor) throws E {
        return groupBy(iter, keyExtractor, Suppliers.ofMap());
    }

    /**
     *
     *
     * @param <K>
     * @param <T>
     * @param <M>
     * @param <E>
     * @param iter
     * @param keyExtractor
     * @param mapSupplier
     * @return
     * @throws E
     */
    @Beta
    public static <K, T, M extends Map<K, List<T>>, E extends Exception> M groupBy(final Iterator<? extends T> iter,
            final Throwables.Function<? super T, ? extends K, E> keyExtractor, final Supplier<M> mapSupplier) throws E {
        checkArgNotNull(keyExtractor, "keyExtractor");
        checkArgNotNull(mapSupplier, "mapSupplier");

        final M ret = mapSupplier.get();

        if (iter == null) {
            return ret;
        }

        K key = null;
        List<T> val = null;
        T e = null;

        while (iter.hasNext()) {
            e = iter.next();

            key = keyExtractor.apply(e);
            val = ret.get(key);

            if (val == null) {
                val = new ArrayList<>();

                ret.put(key, val);
            }

            val.add(e);
        }

        return ret;
    }

    /**
     *
     * @param <T>
     * @param a
     * @return
     * @see ObjIterator#of(Object...)
     */
    @Beta
    public static <T> ObjIterator<T> iterate(final T[] a) {
        return ObjIterator.of(a);
    }

    /**
     *
     * @param <T>
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @see ObjIterator#of(Object[], int, int)
     */
    @Beta
    public static <T> ObjIterator<T> iterate(final T[] a, final int fromIndex, final int toIndex) {
        return ObjIterator.of(a, fromIndex, toIndex);
    }

    /**
     *
     *
     * @param <T>
     * @param iterable
     * @return
     */
    @Beta
    public static <T> Iterator<T> iterate(final Iterable<? extends T> iterable) {
        return iterable == null ? ObjIterator.<T> empty() : (Iterator<T>) iterable.iterator();
    }

    /**
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean disjoint(final Object[] a, final Object[] b) {
        if (isEmpty(a) || isEmpty(b)) {
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
        if (isEmpty(c1) || isEmpty(c2)) {
            return true;
        }

        if (c1 instanceof Set || (!(c2 instanceof Set) && c1.size() > c2.size())) {
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
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
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
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
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
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
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
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
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
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
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
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
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
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
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
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
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
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
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
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static <T> T fromJSON(final Class<? extends T> targetClass, final String json, final int fromIndex, final int toIndex,
            final JSONDeserializationConfig config) {
        return Utils.jsonParser.deserialize(targetClass, json, fromIndex, toIndex, config);
    }

    /**
     *
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param json
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final String json) {
        return fromJSON(targetType, json, null);
    }

    /**
     *
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param json
     * @param config
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final String json, final JSONDeserializationConfig config) {
        return Utils.jsonParser.deserialize(targetType.clazz(), json, setConfig(targetType, config, true));
    }

    /**
     *
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param json
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final File json) {
        return fromJSON(targetType, json, null);
    }

    /**
     *
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param json
     * @param config
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final File json, final JSONDeserializationConfig config) {
        return Utils.jsonParser.deserialize(targetType.clazz(), json, setConfig(targetType, config, true));
    }

    /**
     *
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param json
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final InputStream json) {
        return fromJSON(targetType, json, null);
    }

    /**
     *
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param json
     * @param config
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final InputStream json, final JSONDeserializationConfig config) {
        return Utils.jsonParser.deserialize(targetType.clazz(), json, setConfig(targetType, config, true));
    }

    /**
     *
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param json
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final Reader json) {
        return fromJSON(targetType, json, null);
    }

    /**
     *
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param json
     * @param config
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final Reader json, final JSONDeserializationConfig config) {
        return Utils.jsonParser.deserialize(targetType.clazz(), json, setConfig(targetType, config, true));
    }

    /**
     *
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param json
     * @param fromIndex
     * @param toIndex
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final String json, final int fromIndex, final int toIndex) {
        return fromJSON(targetType, json, fromIndex, toIndex, null);
    }

    /**
     *
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param json
     * @param fromIndex
     * @param toIndex
     * @param config
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static <T> T fromJSON(final Type<? extends T> targetType, final String json, final int fromIndex, final int toIndex,
            final JSONDeserializationConfig config) {
        return Utils.jsonParser.deserialize(targetType.clazz(), json, fromIndex, toIndex, setConfig(targetType, config, true));
    }

    /**
     *
     * @param <T>
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @param source
     * @return
     */
    public static <T> CheckedStream<T, IOException> streamJSON(Class<? extends T> elementClass, String source) {
        return Utils.jsonParser.stream(elementClass, source);
    }

    /**
     *
     * @param <T>
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @param source
     * @param config
     * @return
     */
    public static <T> CheckedStream<T, IOException> streamJSON(Class<? extends T> elementClass, String source, JSONDeserializationConfig config) {
        return Utils.jsonParser.stream(elementClass, source, config);
    }

    /**
     *
     * @param <T>
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @param source
     * @return
     */
    public static <T> CheckedStream<T, IOException> streamJSON(Class<? extends T> elementClass, File source) {
        return Utils.jsonParser.stream(elementClass, source);
    }

    /**
     *
     * @param <T>
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @param source
     * @param config
     * @return
     */
    public static <T> CheckedStream<T, IOException> streamJSON(Class<? extends T> elementClass, File source, JSONDeserializationConfig config) {
        return Utils.jsonParser.stream(elementClass, source, config);
    }

    /**
     *
     * @param <T>
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @param source
     * @return
     */
    public static <T> CheckedStream<T, IOException> streamJSON(Class<? extends T> elementClass, InputStream source) {
        return streamJSON(elementClass, source, false);
    }

    /**
     *
     * @param <T>
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @param source
     * @param closeInputStreamWhenStreamIsClosed
     * @return
     */
    public static <T> CheckedStream<T, IOException> streamJSON(Class<? extends T> elementClass, InputStream source,
            boolean closeInputStreamWhenStreamIsClosed) {
        return Utils.jsonParser.stream(elementClass, source, closeInputStreamWhenStreamIsClosed);
    }

    /**
     *
     * @param <T>
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @param source
     * @param closeInputStreamWhenStreamIsClosed
     * @param config
     * @return
     */
    public static <T> CheckedStream<T, IOException> streamJSON(Class<? extends T> elementClass, InputStream source, boolean closeInputStreamWhenStreamIsClosed,
            JSONDeserializationConfig config) {
        return Utils.jsonParser.stream(elementClass, source, closeInputStreamWhenStreamIsClosed, config);
    }

    /**
     *
     * @param <T>
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @param source
     * @return
     */
    public static <T> CheckedStream<T, IOException> streamJSON(Class<? extends T> elementClass, Reader source) {
        return streamJSON(elementClass, source, false);
    }

    /**
     *
     * @param <T>
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @param source
     * @param closeReaderWhenStreamIsClosed
     * @return
     */
    public static <T> CheckedStream<T, IOException> streamJSON(Class<? extends T> elementClass, Reader source, boolean closeReaderWhenStreamIsClosed) {
        return Utils.jsonParser.stream(elementClass, source, closeReaderWhenStreamIsClosed);
    }

    /**
     *
     * @param <T>
     * @param elementClass Only Bean/Map/Collection/Array/DataSet element types are supported at present.
     * @param source
     * @param closeReaderWhenStreamIsClosed
     * @param config
     * @return
     */
    public static <T> CheckedStream<T, IOException> streamJSON(Class<? extends T> elementClass, Reader source, boolean closeReaderWhenStreamIsClosed,
            JSONDeserializationConfig config) {
        return Utils.jsonParser.stream(elementClass, source, closeReaderWhenStreamIsClosed, config);
    }

    /**
     *
     * @param json
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static String formatJSON(final String json) {
        return formatJSON(Object.class, json, Utils.jscPrettyFormat);
    }

    /**
     *
     * @param json
     * @param config
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static String formatJSON(final String json, final JSONSerializationConfig config) {
        return formatJSON(Object.class, json, config);
    }

    /**
     *
     * @param type
     * @param json
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static String formatJSON(final Class<?> type, final String json) {
        return toJSON(fromJSON(type, json), Utils.jscPrettyFormat);
    }

    /**
     *
     * @param type
     * @param json
     * @param config
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static String formatJSON(final Class<?> type, final String json, final JSONSerializationConfig config) {
        final JSONSerializationConfig configToUse = config == null ? Utils.jscPrettyFormat
                : (config.prettyFormat() == false ? config.copy().prettyFormat(true) : config);

        return toJSON(fromJSON(type, json), configToUse);
    }

    /**
     *
     * @param type
     * @param json
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static String formatJSON(final Type<?> type, final String json) {
        return toJSON(fromJSON(type, json), Utils.jscPrettyFormat);
    }

    /**
     *
     * @param type
     * @param json
     * @param config
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static String formatJSON(final Type<?> type, final String json, final JSONSerializationConfig config) {
        final JSONSerializationConfig configToUse = config == null ? Utils.jscPrettyFormat
                : (config.prettyFormat() == false ? config.copy().prettyFormat(true) : config);

        return toJSON(fromJSON(type, json), configToUse);
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
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
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
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
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
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
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
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
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
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
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
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
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
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
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
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static <T> T fromXML(final Class<? extends T> targetClass, final Reader xml, final XMLDeserializationConfig config) {
        return Utils.xmlParser.deserialize(targetClass, xml, config);
    }

    /**
     *
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param xml
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static <T> T fromXML(final Type<? extends T> targetType, final String xml) {
        return fromJSON(targetType, xml, null);
    }

    /**
     *
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param xml
     * @param config
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static <T> T fromXML(final Type<? extends T> targetType, final String xml, final XMLDeserializationConfig config) {
        return Utils.xmlParser.deserialize(targetType.clazz(), xml, setConfig(targetType, config, false));
    }

    /**
     *
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param xml
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static <T> T fromXML(final Type<? extends T> targetType, final File xml) {
        return fromJSON(targetType, xml, null);
    }

    /**
     *
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param xml
     * @param config
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static <T> T fromXML(final Type<? extends T> targetType, final File xml, final XMLDeserializationConfig config) {
        return Utils.xmlParser.deserialize(targetType.clazz(), xml, setConfig(targetType, config, false));
    }

    /**
     *
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param xml
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static <T> T fromXML(final Type<? extends T> targetType, final InputStream xml) {
        return fromJSON(targetType, xml, null);
    }

    /**
     *
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param xml
     * @param config
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static <T> T fromXML(final Type<? extends T> targetType, final InputStream xml, final XMLDeserializationConfig config) {
        return Utils.xmlParser.deserialize(targetType.clazz(), xml, setConfig(targetType, config, false));
    }

    /**
     *
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param xml
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static <T> T fromXML(final Type<? extends T> targetType, final Reader xml) {
        return fromJSON(targetType, xml, null);
    }

    /**
     *
     * @param <T>
     * @param targetType can be the {@code Type} of {@code Bean/Array/Collection/Map}.
     * @param xml
     * @param config
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
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
        C configToReturn = config;

        if (targetType.isCollection() || targetType.isArray()) {
            if (config == null || config.getElementType() == null) {
                configToReturn = config == null ? (C) (isJSON ? JDC.create() : XDC.create()) : (C) config.copy();

                configToReturn.setElementType(targetType.getParameterTypes()[0]);
            }
        } else if (targetType.isMap() && (config == null || config.getMapKeyType() == null || config.getMapValueType() == null)) {
            configToReturn = config == null ? (C) (isJSON ? JDC.create() : XDC.create()) : (C) config.copy();

            if (configToReturn.getMapKeyType() == null) {
                configToReturn.setMapKeyType(targetType.getParameterTypes()[0]);
            }

            if (configToReturn.getMapValueType() == null) {
                configToReturn.setMapValueType(targetType.getParameterTypes()[1]);
            }
        }

        return configToReturn;
    }

    /**
     *
     * @param xml
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static String formatXML(final String xml) {
        return formatXML(MapEntity.class, xml);
    }

    /**
     *
     * @param xml
     * @param config
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static String formatXML(final String xml, final XMLSerializationConfig config) {
        return formatXML(MapEntity.class, xml, config);
    }

    /**
     *
     * @param type
     * @param xml
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static String formatXML(final Class<?> type, final String xml) {
        return toXML(fromXML(type, xml), Utils.xscPrettyFormat);
    }

    /**
     *
     * @param type
     * @param xml
     * @param config
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static String formatXML(final Class<?> type, final String xml, final XMLSerializationConfig config) {
        final XMLSerializationConfig configToUse = config == null ? Utils.xscPrettyFormat
                : (config.prettyFormat() == false ? config.copy().prettyFormat(true) : config);

        return toXML(fromXML(type, xml), configToUse);
    }

    /**
     *
     * @param type
     * @param xml
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static String formatXML(final Type<?> type, final String xml) {
        return toXML(fromXML(type, xml), Utils.xscPrettyFormat);
    }

    /**
     *
     * @param type
     * @param xml
     * @param config
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static String formatXML(final Type<?> type, final String xml, final XMLSerializationConfig config) {
        final XMLSerializationConfig configToUse = config == null ? Utils.xscPrettyFormat
                : (config.prettyFormat() == false ? config.copy().prettyFormat(true) : config);

        return toXML(fromXML(type, xml), configToUse);
    }

    /**
     * Xml 2 JSO.
     *
     * @param xml
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static String xml2JSON(final String xml) {
        return xml2JSON(Map.class, xml);
    }

    /**
     * Xml 2 JSO.
     *
     * @param cls
     * @param xml
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static String xml2JSON(final Class<?> cls, final String xml) {
        return Utils.jsonParser.serialize(Utils.xmlParser.deserialize(cls, xml), Utils.jsc);
    }

    /**
     * Json 2 XML.
     *
     * @param json
     * @return
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
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
     * @see com.landawn.abacus.util.TypeReference
     * @see com.landawn.abacus.util.TypeReference.TypeToken
     */
    public static String json2XML(final Class<?> cls, final String json) {
        return Utils.xmlParser.serialize(Utils.jsonParser.deserialize(cls, json));
    }

    /**
     *
     * @param cmd
     * @param retryTimes
     * @param retryIntervallInMillis
     * @param retryCondition
     * @see Retry#of(int, long, Predicate)
     * @see Retry#of(int, long, Predicate)
     */
    public static void execute(final Throwables.Runnable<? extends Exception> cmd, final int retryTimes, final long retryIntervallInMillis,
            final Predicate<? super Exception> retryCondition) {
        try {
            Retry.of(retryTimes, retryIntervallInMillis, retryCondition).run(cmd);
        } catch (Exception e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     *
     * @param <R>
     * @param cmd
     * @param retryTimes
     * @param retryIntervallInMillis
     * @param retryCondition
     * @return
     * @see Retry#of(int, long, Predicate)
     * @see Retry#of(int, long, Predicate)
     */
    public static <R> R execute(final Callable<R> cmd, final int retryTimes, final long retryIntervallInMillis,
            final BiPredicate<? super R, ? super Exception> retryCondition) {
        try {
            final Retry<R> retry = Retry.of(retryTimes, retryIntervallInMillis, retryCondition);
            return retry.call(cmd);
        } catch (Exception e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     *
     * @param command
     * @return
     * @see Futures
     */
    public static ContinuableFuture<Void> asyncExecute(final Throwables.Runnable<? extends Exception> command) {
        return asyncExecutor.execute(command);
    }

    /**
     *
     * @param command
     * @param delayInMillis
     * @return
     * @see Futures
     */
    @MayReturnNull
    public static ContinuableFuture<Void> asyncExecute(final Throwables.Runnable<? extends Exception> command, final long delayInMillis) {
        return new ContinuableFuture<>(SCHEDULED_EXECUTOR.schedule(() -> {
            command.run();
            return null;
        }, delayInMillis, TimeUnit.MILLISECONDS));
    }

    /**
     *
     * @param commands
     * @return
     * @see Futures
     */
    @SuppressWarnings("deprecation")
    @SafeVarargs
    public static List<ContinuableFuture<Void>> asyncExecute(final Throwables.Runnable<? extends Exception>... commands) {
        return asyncExecutor.execute(commands);
    }

    /**
     *
     * @param commands
     * @return
     * @see Futures
     */
    public static List<ContinuableFuture<Void>> asyncExecute(final List<? extends Throwables.Runnable<? extends Exception>> commands) {
        return asyncExecutor.execute(commands);
    }

    /**
     *
     * @param commands
     * @param executor
     * @return
     * @see Futures
     */
    public static List<ContinuableFuture<Void>> asyncExecute(final List<? extends Throwables.Runnable<? extends Exception>> commands, final Executor executor) {
        if (isEmpty(commands)) {
            return new ArrayList<>();
        }

        final List<ContinuableFuture<Void>> results = new ArrayList<>(commands.size());

        for (Throwables.Runnable<? extends Exception> cmd : commands) {
            results.add(ContinuableFuture.run(cmd, executor));
        }

        return results;
    }

    /**
     *
     * @param <R>
     * @param command
     * @return
     * @see Futures
     */
    public static <R> ContinuableFuture<R> asyncExecute(final Callable<R> command) {
        return asyncExecutor.execute(command);
    }

    /**
     *
     * @param <R>
     * @param command
     * @param delayInMillis
     * @return
     * @see Futures
     */
    public static <R> ContinuableFuture<R> asyncExecute(final Callable<R> command, final long delayInMillis) {
        return new ContinuableFuture<>(SCHEDULED_EXECUTOR.schedule(command, delayInMillis, TimeUnit.MILLISECONDS));
    }

    /**
     *
     * @param <R>
     * @param commands
     * @return
     * @see Futures
     */
    @SuppressWarnings("deprecation")
    @SafeVarargs
    public static <R> List<ContinuableFuture<R>> asyncExecute(final Callable<R>... commands) {
        return asyncExecutor.execute(commands);
    }

    /**
     *
     * @param <R>
     * @param commands
     * @return
     * @see Futures
     */
    public static <R> List<ContinuableFuture<R>> asyncExecute(final Collection<? extends Callable<R>> commands) {
        return asyncExecutor.execute(commands);
    }

    /**
     *
     * @param <R>
     * @param commands
     * @param executor
     * @return
     * @see Futures
     */
    public static <R> List<ContinuableFuture<R>> asyncExecute(final Collection<? extends Callable<R>> commands, final Executor executor) {
        if (isEmpty(commands)) {
            return new ArrayList<>();
        }

        final List<ContinuableFuture<R>> results = new ArrayList<>(commands.size());

        for (Callable<R> cmd : commands) {
            results.add(ContinuableFuture.call(cmd, executor));
        }

        return results;
    }

    /**
     *
     * @param cmd
     * @param retryTimes
     * @param retryIntervallInMillisInMillis
     * @param retryCondition
     * @return
     * @see Futures
     */
    @MayReturnNull
    public static ContinuableFuture<Void> asyncExecute(final Throwables.Runnable<? extends Exception> cmd, final int retryTimes,
            final long retryIntervallInMillisInMillis, final Predicate<? super Exception> retryCondition) {
        return asyncExecutor.execute((Callable<Void>) () -> {
            Retry.of(retryTimes, retryIntervallInMillisInMillis, retryCondition).run(cmd);
            return null;
        });
    }

    /**
     *
     * @param <R>
     * @param cmd
     * @param retryTimes
     * @param retryIntervallInMillisInMillis
     * @param retryCondition
     * @return
     * @see Futures
     */
    public static <R> ContinuableFuture<R> asyncExecute(final Callable<R> cmd, final int retryTimes, final long retryIntervallInMillisInMillis,
            final BiPredicate<? super R, ? super Exception> retryCondition) {
        return asyncExecutor.execute((Callable<R>) () -> {
            final Retry<R> retry = Retry.of(retryTimes, retryIntervallInMillisInMillis, retryCondition);
            return retry.call(cmd);
        });
    }

    /**
     *
     *
     * @param command
     * @param executor
     * @return
     */
    public static ContinuableFuture<Void> asyncExecute(final Throwables.Runnable<? extends Exception> command, final Executor executor) {
        return ContinuableFuture.run(command, executor);
    }

    /**
     *
     *
     * @param <R>
     * @param command
     * @param executor
     * @return
     */
    public static <R> ContinuableFuture<R> asyncExecute(final Callable<R> command, final Executor executor) {
        return ContinuableFuture.call(command, executor);
    }

    /**
     * Executes the specified commands/tasks asynchronous and immediately returns an {@code iterator} for iterating the result lazily.
     * The first element will be the result of the command/task which is completed first.
     * <br />
     * If error happens in one command/task, iteration will be interrupted and error will be thrown. But other commands/tasks won't be impacted or cancelled.
     *
     * @param commands
     * @return
     */
    public static ObjIterator<Void> asynRun(final Collection<? extends Throwables.Runnable<? extends Exception>> commands) {
        return asynRun(commands, asyncExecutor.getExecutor());
    }

    /**
     * Executes the specified commands/tasks asynchronous and immediately returns an {@code iterator} for iterating the result lazily.
     * The first element will be the result of the command/task which is completed first.
     * <br />
     * If error happens in one command/task, iteration will be interrupted and error will be thrown. But other commands/tasks won't be impacted or cancelled.
     *
     *
     * @param commands
     * @param executor
     * @return
     */
    public static ObjIterator<Void> asynRun(final Collection<? extends Throwables.Runnable<? extends Exception>> commands, final Executor executor) {
        checkArgNotNull(executor, "executor");

        if (isEmpty(commands)) {
            return ObjIterator.empty();
        }

        final int cmdCount = commands.size();
        final List<FutureTask<Object>> futures = new LinkedList<>();
        final ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<>(cmdCount);
        final Object none = NULL_MASK;

        for (Throwables.Runnable<? extends Exception> cmd : commands) {
            final FutureTask<Object> futureTask = new FutureTask<>(() -> {
                cmd.run();

                queue.add(none);

                return null;
            });

            executor.execute(futureTask);

            futures.add(futureTask);
        }

        return new ObjIterator<>() {
            @Override
            public boolean hasNext() {
                if (queue.size() > 0) {
                    return true;
                }

                while (true) {
                    final Iterator<FutureTask<Object>> iter = futures.iterator();

                    while (iter.hasNext()) {
                        final FutureTask<Object> future = iter.next();

                        if (future.isDone()) {
                            try {
                                future.get();
                            } catch (InterruptedException | ExecutionException e) {
                                // cause inconsistent if iterate result or not. Secondly, asynchronized execution should not impact each other.
                                //    while (iter.hasNext()) {
                                //        iter.next().cancel(false);
                                //    }

                                throw toRuntimeException(e);
                            }

                            iter.remove();

                            if (queue.size() > 0) {
                                return true;
                            }
                        }
                    }

                    if (queue.size() > 0) {
                        return true;
                    }

                    if (futures.size() == 0) {
                        break;
                    }

                    sleepUninterruptibly(1);
                }

                return queue.size() > 0;
            }

            @Override
            public Void next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                queue.poll();

                return null;
            }
        };
    }

    /**
     * Executes the specified commands/tasks asynchronous and immediately returns an {@code iterator} for iterating the result lazily.
     * The first element will be the result of the command/task which is completed first.
     * <br />
     * If error happens in one command/task, iteration will be interrupted and error will be thrown. But other commands/tasks won't be impacted or cancelled.
     *
     *
     * @param <R>
     * @param commands
     * @return
     */
    public static <R> ObjIterator<R> asynCall(final Collection<? extends Callable<? extends R>> commands) {
        return asynCall(commands, asyncExecutor.getExecutor());
    }

    /**
     * Executes the specified commands/tasks asynchronous and immediately returns an {@code iterator} for iterating the result lazily.
     * The first element will be the result of the command/task which is completed first.
     * <br />
     * If error happens in one command/task, iteration will be interrupted and error will be thrown. But other commands/tasks won't be impacted or cancelled.
     *
     *
     * @param <R>
     * @param commands
     * @param executor
     * @return
     */
    public static <R> ObjIterator<R> asynCall(final Collection<? extends Callable<? extends R>> commands, final Executor executor) {
        checkArgNotNull(executor, "executor");

        if (isEmpty(commands)) {
            return ObjIterator.empty();
        }

        final int cmdCount = commands.size();
        final List<FutureTask<R>> futures = new LinkedList<>();
        final ArrayBlockingQueue<R> queue = new ArrayBlockingQueue<>(cmdCount);
        final R none = (R) NULL_MASK;

        for (Callable<? extends R> cmd : commands) {
            final FutureTask<R> futureTask = new FutureTask<>(() -> {
                final R ret = cmd.call();

                if (ret == null) {
                    queue.add(none);
                } else {
                    queue.add(ret);
                }

                return ret;
            });

            executor.execute(futureTask);

            futures.add(futureTask);
        }

        return new ObjIterator<>() {
            private R next = null;

            @Override
            public boolean hasNext() {
                if (queue.size() > 0) {
                    return true;
                }

                while (true) {
                    final Iterator<FutureTask<R>> iter = futures.iterator();

                    while (iter.hasNext()) {
                        final FutureTask<R> future = iter.next();

                        if (future.isDone()) {
                            try {
                                future.get();
                            } catch (InterruptedException | ExecutionException e) {
                                // cause inconsistent if iterate result or not. Secondly, asynchronized execution should not impact each other.
                                //    while (iter.hasNext()) {
                                //        iter.next().cancel(false);
                                //    }

                                throw toRuntimeException(e);
                            }

                            iter.remove();

                            if (queue.size() > 0) {
                                return true;
                            }
                        }
                    }

                    if (queue.size() > 0) {
                        return true;
                    }

                    if (futures.size() == 0) {
                        break;
                    }

                    sleepUninterruptibly(1);
                }

                return queue.size() > 0;
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                next = queue.poll();
                return next == none ? null : next;
            }
        };
    }

    /**
     * Executes and complete the input commands in parallel.
     * <br />
     * if error happens in one task, {@code cancel} will be called for other unfinished tasks.
     *
     * @param command to be completed in current thread.
     * @param command2 to be completed in another thread.
     *
     * @see Fn#jc2r(Callable)
     */
    public static void runInParallel(final Throwables.Runnable<? extends Exception> command, final Throwables.Runnable<? extends Exception> command2) {
        final ContinuableFuture<Void> f2 = asyncExecute(command2);
        boolean hasException = true;

        try {
            command.run();
            f2.get();

            hasException = false;
        } catch (Exception e) {
            throw ExceptionUtil.toRuntimeException(e);
        } finally {
            if (hasException) {
                if (f2.isDone() == false) { // NOSONAR
                    f2.cancel(false);
                }
            }
        }
    }

    /**
     * Executes and complete the input commands in parallel.
     * <br />
     * if error happens in one task, {@code cancel} will be called for other unfinished tasks.
     *
     * @param command to be completed in current thread.
     * @param command2 to be completed in another thread.
     * @param command3 to be completed in another thread.
     *
     * @see Fn#jc2r(Callable)
     */
    public static void runInParallel(final Throwables.Runnable<? extends Exception> command, final Throwables.Runnable<? extends Exception> command2,
            final Throwables.Runnable<? extends Exception> command3) {
        final ContinuableFuture<Void> f2 = asyncExecute(command2);
        final ContinuableFuture<Void> f3 = asyncExecute(command3);
        boolean hasException = true;

        try {
            command.run();
            f2.get();
            f3.get();

            hasException = false;
        } catch (Exception e) {
            throw ExceptionUtil.toRuntimeException(e);
        } finally {
            if (hasException) {
                if (f2.isDone() == false) {
                    f2.cancel(false);
                }

                if (f3.isDone() == false) {
                    f3.cancel(false);
                }
            }
        }
    }

    /**
     * Executes and complete the input commands in parallel.
     * <br />
     * if error happens in one task, {@code cancel} will be called for other unfinished tasks.
     *
     * @param command to be completed in current thread.
     * @param command2 to be completed in another thread.
     * @param command3 to be completed in another thread.
     * @param command4 to be completed in another thread.
     *
     * @see Fn#jc2r(Callable)
     */
    public static void runInParallel(final Throwables.Runnable<? extends Exception> command, final Throwables.Runnable<? extends Exception> command2,
            final Throwables.Runnable<? extends Exception> command3, final Throwables.Runnable<? extends Exception> command4) {
        final ContinuableFuture<Void> f2 = asyncExecute(command2);
        final ContinuableFuture<Void> f3 = asyncExecute(command3);
        final ContinuableFuture<Void> f4 = asyncExecute(command4);
        boolean hasException = true;

        try {
            command.run();
            f2.get();
            f3.get();
            f4.get();

            hasException = false;
        } catch (Exception e) {
            throw ExceptionUtil.toRuntimeException(e);
        } finally {
            if (hasException) {
                if (f2.isDone() == false) {
                    f2.cancel(false);
                }

                if (f3.isDone() == false) {
                    f3.cancel(false);
                }

                if (f4.isDone() == false) {
                    f4.cancel(false);
                }
            }
        }
    }

    /**
     * Executes and complete the input commands in parallel.
     * <br />
     * if error happens in one task, {@code cancel} will be called for other unfinished tasks.
     *
     * @param command to be completed in current thread.
     * @param command2 to be completed in another thread.
     * @param command3 to be completed in another thread.
     * @param command4 to be completed in another thread.
     * @param command5 to be completed in another thread.
     *
     * @see Fn#jc2r(Callable)
     */
    public static void runInParallel(final Throwables.Runnable<? extends Exception> command, final Throwables.Runnable<? extends Exception> command2,
            final Throwables.Runnable<? extends Exception> command3, final Throwables.Runnable<? extends Exception> command4,
            final Throwables.Runnable<? extends Exception> command5) {
        final ContinuableFuture<Void> f2 = asyncExecute(command2);
        final ContinuableFuture<Void> f3 = asyncExecute(command3);
        final ContinuableFuture<Void> f4 = asyncExecute(command4);
        final ContinuableFuture<Void> f5 = asyncExecute(command5);
        boolean hasException = true;

        try {
            command.run();
            f2.get();
            f3.get();
            f4.get();
            f5.get();

            hasException = false;
        } catch (Exception e) {
            throw ExceptionUtil.toRuntimeException(e);
        } finally {
            if (hasException) {
                if (f2.isDone() == false) {
                    f2.cancel(false);
                }

                if (f3.isDone() == false) {
                    f3.cancel(false);
                }

                if (f4.isDone() == false) {
                    f4.cancel(false);
                }

                if (f5.isDone() == false) {
                    f5.cancel(false);
                }
            }
        }
    }

    /**
     * Executes and complete the input commands in parallel.
     * <br />
     * if error happens in one task, {@code cancel} will be called for other unfinished tasks.
     *
     * @param commands
     */
    public static void runInParallel(final Collection<? extends Throwables.Runnable<? extends Exception>> commands) {
        runInParallel(commands, asyncExecutor.getExecutor());
    }

    /**
     * Executes and complete the input commands in parallel.
     * <br />
     * if error happens in one task, {@code cancel} will be called for other unfinished tasks.
     *
     * @param commands
     * @param executor
     */
    public static void runInParallel(final Collection<? extends Throwables.Runnable<? extends Exception>> commands, final Executor executor) {
        checkArgNotNull(executor, "executor");

        if (isEmpty(commands)) {
            return;
        }

        final int cmdSize = commands.size();
        final List<ContinuableFuture<Void>> futures = new ArrayList<>(cmdSize - 1);
        boolean hasException = true;

        try {
            final Iterator<? extends Throwables.Runnable<? extends Exception>> iter = commands.iterator();

            for (int i = 0, toIndex = cmdSize - 1; i < toIndex; i++) {
                futures.add(asyncExecute(iter.next(), executor));
            }

            iter.next().run();

            for (ContinuableFuture<Void> f : futures) {
                f.get();
            }

            hasException = false;
        } catch (Exception e) {
            throw ExceptionUtil.toRuntimeException(e);
        } finally {
            if (hasException) {
                for (ContinuableFuture<Void> f : futures) {
                    if (f.isDone() == false) {
                        f.cancel(false);
                    }
                }
            }
        }
    }

    /**
     * Executes and complete the input commands in parallel.
     * <br />
     * if error happens in one task, {@code cancel} will be called for other unfinished tasks.
     *
     * @param <R>
     * @param <R2>
     * @param command to be completed in current thread.
     * @param command2 to be completed in another thread.
     * @return
     * @see Fn#jr2c(Runnable)
     */
    public static <R, R2> Tuple2<R, R2> callInParallel(final Callable<R> command, final Callable<R2> command2) {
        final ContinuableFuture<R2> f2 = asyncExecute(command2);
        boolean hasException = true;

        try {
            final R r = command.call();
            final R2 r2 = f2.get();

            hasException = false;

            return Tuple.of(r, r2);
        } catch (Exception e) {
            throw ExceptionUtil.toRuntimeException(e);
        } finally {
            if (hasException) {
                if (f2.isDone() == false) { // NOSONAR
                    f2.cancel(false);
                }
            }
        }
    }

    /**
     * Executes and complete the input commands in parallel.
     * <br />
     * if error happens in one task, {@code cancel} will be called for other unfinished tasks.
     *
     * @param <R>
     * @param <R2>
     * @param <R3>
     * @param command to be completed in current thread.
     * @param command2 to be completed in another thread.
     * @param command3 to be completed in another thread.
     * @return
     * @see Fn#jr2c(Runnable)
     */
    public static <R, R2, R3> Tuple3<R, R2, R3> callInParallel(final Callable<R> command, final Callable<R2> command2, final Callable<R3> command3) {
        final ContinuableFuture<R2> f2 = asyncExecute(command2);
        final ContinuableFuture<R3> f3 = asyncExecute(command3);
        boolean hasException = true;

        try {
            final R r = command.call();
            final R2 r2 = f2.get();
            final R3 r3 = f3.get();

            hasException = false;

            return Tuple.of(r, r2, r3);
        } catch (Exception e) {
            throw ExceptionUtil.toRuntimeException(e);
        } finally {
            if (hasException) {
                if (f2.isDone() == false) {
                    f2.cancel(false);
                }

                if (f3.isDone() == false) {
                    f3.cancel(false);
                }
            }
        }
    }

    /**
     * Executes and complete the input commands in parallel.
     * <br />
     * if error happens in one task, {@code cancel} will be called for other unfinished tasks.
     *
     * @param <R>
     * @param <R2>
     * @param <R3>
     * @param <R4>
     * @param command to be completed in current thread.
     * @param command2 to be completed in another thread.
     * @param command3 to be completed in another thread.
     * @param command4 to be completed in another thread.
     * @return
     * @see Fn#jr2c(Runnable)
     */
    public static <R, R2, R3, R4> Tuple4<R, R2, R3, R4> callInParallel(final Callable<R> command, final Callable<R2> command2, final Callable<R3> command3,
            final Callable<R4> command4) {
        final ContinuableFuture<R2> f2 = asyncExecute(command2);
        final ContinuableFuture<R3> f3 = asyncExecute(command3);
        final ContinuableFuture<R4> f4 = asyncExecute(command4);
        boolean hasException = true;

        try {
            final R r = command.call();
            final R2 r2 = f2.get();
            final R3 r3 = f3.get();
            final R4 r4 = f4.get();

            hasException = false;

            return Tuple.of(r, r2, r3, r4);
        } catch (Exception e) {
            throw ExceptionUtil.toRuntimeException(e);
        } finally {
            if (hasException) {
                if (f2.isDone() == false) {
                    f2.cancel(false);
                }

                if (f3.isDone() == false) {
                    f3.cancel(false);
                }

                if (f4.isDone() == false) {
                    f4.cancel(false);
                }
            }
        }
    }

    /**
     * Executes and complete the input commands in parallel.
     * <br />
     * if error happens in one task, {@code cancel} will be called for other unfinished tasks.
     *
     * @param <R>
     * @param <R2>
     * @param <R3>
     * @param <R4>
     * @param <R5>
     * @param command to be completed in current thread.
     * @param command2 to be completed in another thread.
     * @param command3 to be completed in another thread.
     * @param command4 to be completed in another thread.
     * @param command5 to be completed in another thread.
     * @return
     * @see Fn#jr2c(Runnable)
     */
    public static <R, R2, R3, R4, R5> Tuple5<R, R2, R3, R4, R5> callInParallel(final Callable<R> command, final Callable<R2> command2,
            final Callable<R3> command3, final Callable<R4> command4, final Callable<R5> command5) {
        final ContinuableFuture<R2> f2 = asyncExecute(command2);
        final ContinuableFuture<R3> f3 = asyncExecute(command3);
        final ContinuableFuture<R4> f4 = asyncExecute(command4);
        final ContinuableFuture<R5> f5 = asyncExecute(command5);
        boolean hasException = true;

        try {
            final R r = command.call();
            final R2 r2 = f2.get();
            final R3 r3 = f3.get();
            final R4 r4 = f4.get();
            final R5 r5 = f5.get();

            hasException = false;

            return Tuple.of(r, r2, r3, r4, r5);
        } catch (Exception e) {
            throw ExceptionUtil.toRuntimeException(e);
        } finally {
            if (hasException) {
                if (f2.isDone() == false) {
                    f2.cancel(false);
                }

                if (f3.isDone() == false) {
                    f3.cancel(false);
                }

                if (f4.isDone() == false) {
                    f4.cancel(false);
                }

                if (f5.isDone() == false) {
                    f5.cancel(false);
                }
            }
        }
    }

    /**
     * Executes and complete the input commands in parallel.
     * <br />
     * if error happens in one task, {@code cancel} will be called for other unfinished tasks.
     *
     * @param <R>
     * @param commands
     * @return
     */
    public static <R> List<R> callInParallel(final Collection<? extends Callable<? extends R>> commands) {
        return callInParallel(commands, asyncExecutor.getExecutor());
    }

    /**
     * Executes and complete the input commands in parallel.
     * <br />
     * if error happens in one task, {@code cancel} will be called for other unfinished tasks.
     *
     * @param <R>
     * @param commands
     * @param executor
     * @return
     */
    public static <R> List<R> callInParallel(final Collection<? extends Callable<? extends R>> commands, final Executor executor) {
        checkArgNotNull(executor, "executor");

        if (isEmpty(commands)) {
            return new ArrayList<>(0);
        }

        final int cmdSize = commands.size();
        final List<ContinuableFuture<? extends R>> futures = new ArrayList<>(cmdSize - 1);
        boolean hasException = true;

        try {
            final Iterator<? extends Callable<? extends R>> iter = commands.iterator();

            for (int i = 0, toIndex = cmdSize - 1; i < toIndex; i++) {
                futures.add(asyncExecute(iter.next(), executor));
            }

            final R r = iter.next().call();

            final List<R> result = new ArrayList<>(cmdSize);

            for (ContinuableFuture<? extends R> f : futures) {
                result.add(f.get());
            }

            result.add(r);

            hasException = false;

            return result;
        } catch (Exception e) {
            throw ExceptionUtil.toRuntimeException(e);
        } finally {
            if (hasException) {
                for (ContinuableFuture<? extends R> f : futures) {
                    if (f.isDone() == false) {
                        f.cancel(false);
                    }
                }
            }
        }
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param batchSize
     * @param batchAction
     * @throws E
     */
    public static <T, E extends Exception> void runByBatch(final T[] a, final int batchSize,
            final Throwables.Consumer<? super List<? extends T>, E> batchAction) throws E {
        if (isEmpty(a)) {
            return;
        }

        runByBatch(Arrays.asList(a), batchSize, batchAction);
    }

    /**
    *
    * @param <T>
    * @param <E>
    * @param iter
    * @param batchSize
    * @param batchAction
    * @throws E
    */
    public static <T, E extends Exception> void runByBatch(Iterable<? extends T> iter, final int batchSize,
            final Throwables.Consumer<? super List<? extends T>, E> batchAction) throws E {
        checkArgPositive(batchSize, "batchSize");
        checkArgNotNull(batchAction, "batchAction");

        if (iter == null) {
            return;
        }

        if (iter instanceof List) {
            final List<T> list = (List<T>) iter;
            final int totalSize = list.size();

            for (int i = 0; i < totalSize; i += batchSize) {
                batchAction.accept(list.subList(i, min(i + batchSize, totalSize)));
            }
        } else {
            runByBatch(iter.iterator(), batchSize, batchAction);
        }
    }

    /**
    *
    * @param <T>
    * @param <E>
    * @param iter
    * @param batchSize
    * @param batchAction
    * @throws E
    */
    public static <T, E extends Exception> void runByBatch(final Iterator<? extends T> iter, final int batchSize,
            final Throwables.Consumer<? super List<? extends T>, E> batchAction) throws E {
        checkArgPositive(batchSize, "batchSize");
        checkArgNotNull(batchAction, "batchAction");

        if (iter == null || iter.hasNext() == false) {
            return;
        }

        final T[] a = (T[]) new Object[batchSize];
        int cnt = 0;

        while (iter.hasNext()) {
            a[cnt++ % batchSize] = iter.next();

            if (cnt % batchSize == 0) {
                batchAction.accept(ImmutableList.of(a));
            }
        }

        if (cnt % batchSize != 0) {
            batchAction.accept(ImmutableList.of(copyOfRange(a, 0, cnt % batchSize)));
        }
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param <E2>
     * @param a
     * @param batchSize
     * @param elementConsumer
     * @param batchAction
     * @throws E
     * @throws E2
     */
    public static <T, E extends Exception, E2 extends Exception> void runByBatch(final T[] a, final int batchSize,
            final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> batchAction) throws E, E2 {
        if (isEmpty(a)) {
            return;
        }

        runByBatch(Arrays.asList(a), batchSize, elementConsumer, batchAction);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param <E2>
     * @param iter
     * @param batchSize
     * @param elementConsumer
     * @param batchAction
     * @throws E
     * @throws E2
     */
    public static <T, E extends Exception, E2 extends Exception> void runByBatch(final Iterable<? extends T> iter, final int batchSize,
            final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> batchAction) throws E, E2 {
        if (iter == null) {
            return;
        }

        runByBatch(iter.iterator(), batchSize, elementConsumer, batchAction);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param <E2>
     * @param iter
     * @param batchSize
     * @param elementConsumer
     * @param batchAction
     * @throws E
     * @throws E2
     */
    public static <T, E extends Exception, E2 extends Exception> void runByBatch(final Iterator<? extends T> iter, final int batchSize,
            final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Runnable<E2> batchAction) throws E, E2 {
        checkArgPositive(batchSize, "batchSize");
        checkArgNotNull(elementConsumer, "elementConsumer");
        checkArgNotNull(batchAction, "batchAction");

        if (iter == null || iter.hasNext() == false) {
            return;
        }

        int cnt = 0;

        while (iter.hasNext()) {
            elementConsumer.accept(iter.next());
            cnt++;

            if (cnt % batchSize == 0) {
                batchAction.run();
            }
        }

        if (cnt % batchSize != 0) {
            batchAction.run();
        }
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param a
     * @param batchSize
     * @param batchAction
     * @throws E
     */
    public static <T, R, E extends Exception> List<R> callByBatch(final T[] a, final int batchSize,
            final Throwables.Function<? super List<? extends T>, R, E> batchAction) throws E {
        if (isEmpty(a)) {
            return new ArrayList<>(0);
        }

        return callByBatch(Arrays.asList(a), batchSize, batchAction);
    }

    /**
    *
    * @param <T>
    * @param <R>
    * @param <E>
    * @param iter
    * @param batchSize
    * @param batchAction
    * @throws E
    */
    public static <T, R, E extends Exception> List<R> callByBatch(Iterable<? extends T> iter, final int batchSize,
            final Throwables.Function<? super List<? extends T>, R, E> batchAction) throws E {
        checkArgPositive(batchSize, "batchSize");
        checkArgNotNull(batchAction, "batchAction");

        if (iter == null) {
            return new ArrayList<>(0);
        }

        if (iter instanceof List) {
            final List<T> list = (List<T>) iter;
            final int totalSize = list.size();
            final List<R> result = new ArrayList<>(totalSize % batchSize == 0 ? totalSize / batchSize : totalSize / batchSize + 1);

            for (int i = 0; i < totalSize; i += batchSize) {
                result.add(batchAction.apply(list.subList(i, min(i + batchSize, totalSize))));
            }

            return result;
        } else {
            return callByBatch(iter.iterator(), batchSize, batchAction);
        }
    }

    /**
    *
    * @param <T>
    * @param <R>
    * @param <E>
    * @param iter
    * @param batchSize
    * @param batchAction
    * @throws E
    */
    public static <T, R, E extends Exception> List<R> callByBatch(final Iterator<? extends T> iter, final int batchSize,
            final Throwables.Function<? super List<? extends T>, R, E> batchAction) throws E {
        checkArgPositive(batchSize, "batchSize");
        checkArgNotNull(batchAction, "batchAction");

        if (iter == null || iter.hasNext() == false) {
            return new ArrayList<>();
        }

        final T[] a = (T[]) new Object[batchSize];
        final List<R> result = new ArrayList<>();
        int cnt = 0;

        while (iter.hasNext()) {
            a[cnt++ % batchSize] = iter.next();

            if (cnt % batchSize == 0) {
                result.add(batchAction.apply(ImmutableList.of(a)));
            }
        }

        if (cnt % batchSize != 0) {
            result.add(batchAction.apply(ImmutableList.of(copyOfRange(a, 0, cnt % batchSize))));
        }

        return result;
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param <E2>
     * @param a
     * @param batchSize
     * @param elementConsumer
     * @param batchAction
     * @throws E
     * @throws E2
     */
    public static <T, R, E extends Exception, E2 extends Exception> List<R> callByBatch(final T[] a, final int batchSize,
            final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Callable<? extends R, E2> batchAction) throws E, E2 {
        if (isEmpty(a)) {
            return new ArrayList<>(0);
        }

        return callByBatch(Arrays.asList(a), batchSize, elementConsumer, batchAction);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param <E2>
     * @param iter
     * @param batchSize
     * @param elementConsumer
     * @param batchAction
     * @throws E
     * @throws E2
     */
    public static <T, R, E extends Exception, E2 extends Exception> List<R> callByBatch(final Iterable<? extends T> iter, final int batchSize,
            final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Callable<? extends R, E2> batchAction) throws E, E2 {
        if (iter == null) {
            return new ArrayList<>(0);
        }

        return callByBatch(iter.iterator(), batchSize, elementConsumer, batchAction);
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param <E2>
     * @param iter
     * @param batchSize
     * @param elementConsumer
     * @param batchAction
     * @throws E
     * @throws E2
     */
    public static <T, R, E extends Exception, E2 extends Exception> List<R> callByBatch(final Iterator<? extends T> iter, final int batchSize,
            final Throwables.Consumer<? super T, E> elementConsumer, final Throwables.Callable<? extends R, E2> batchAction) throws E, E2 {
        checkArgPositive(batchSize, "batchSize");
        checkArgNotNull(elementConsumer, "elementConsumer");
        checkArgNotNull(batchAction, "batchAction");

        if (iter == null || iter.hasNext() == false) {
            return new ArrayList<>();
        }

        final List<R> result = new ArrayList<>();
        int cnt = 0;

        while (iter.hasNext()) {
            elementConsumer.accept(iter.next());
            cnt++;

            if (cnt % batchSize == 0) {
                result.add(batchAction.call());
            }
        }

        if (cnt % batchSize != 0) {
            result.add(batchAction.call());
        }

        return result;
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
     * @throws IllegalArgumentException if the specified <code>unit/cmd</code> is <code>null</code>.
     */
    public static void runUninterruptibly(final long timeout, final TimeUnit unit, final Throwables.BiConsumer<Long, TimeUnit, InterruptedException> cmd)
            throws IllegalArgumentException {
        checkArgNotNull(unit, "unit");
        checkArgNotNull(cmd, "cmd");

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
     * @throws IllegalArgumentException if the specified <code>unit/cmd</code> is <code>null</code>.
     */
    public static <T> T callUninterruptibly(final long timeout, final TimeUnit unit, final Throwables.BiFunction<Long, TimeUnit, T, InterruptedException> cmd)
            throws IllegalArgumentException {
        checkArgNotNull(unit, "unit");
        checkArgNotNull(cmd, "cmd");

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
     * @throws IllegalArgumentException if the specified <code>unit</code> is <code>null</code>.
     */
    public static void sleep(final long timeout, final TimeUnit unit) throws IllegalArgumentException {
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
     * @throws IllegalArgumentException if the specified <code>unit</code> is <code>null</code>.
     */
    public static void sleepUninterruptibly(final long timeout, final TimeUnit unit) throws IllegalArgumentException {
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
     *
     * @param e
     * @return
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class,Function)
     */
    @Beta
    public static RuntimeException toRuntimeException(final Exception e) {
        return ExceptionUtil.toRuntimeException(e);
    }

    /**
     * Converts the specified {@code Throwable} to a {@code RuntimeException} if it's a checked {@code exception} or an {@code Error}, otherwise returns itself.
     *
     * @param e
     * @return
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class,Function)
     */
    @Beta
    public static RuntimeException toRuntimeException(final Throwable e) {
        return ExceptionUtil.toRuntimeException(e);
    }

    /**
     * Converts the specified {@code Throwable} to a {@code RuntimeException} if it's a checked {@code exception}, or throw it if it's an {@code Error}. Otherwise returns itself.
     *
     * @param e
     * @param throwIfItIsError
     * @return
     * @see ExceptionUtil#toRuntimeException(Throwable, boolean)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class,Function)
     */
    @Beta
    public static RuntimeException toRuntimeException(final Throwable e, final boolean throwIfItIsError) {
        return ExceptionUtil.toRuntimeException(e, throwIfItIsError);
    }

    //    /**
    //     *
    //     * @param e
    //     * @param type
    //     * @return
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     */
    //    @Beta
    //    public static boolean hasCause(final Throwable e, final Class<? extends Throwable> type) {
    //        return ExceptionUtil.hasCause(e, type);
    //    }
    //
    //    /**
    //     * Returns the specified {@code Throwable e} if there is no cause found in it ({@code e.getCause() == null}).
    //     *
    //     * @param e
    //     * @return
    //     * @see ExceptionUtil#firstCause(Throwable)
    //     */
    //    @Beta
    //    public static Throwable firstCause(final Throwable e) {
    //        return ExceptionUtil.firstCause(e);
    //    }

    /**
     *
     * @param <T>
     * @param obj
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> T println(final T obj) {
        if (obj instanceof Collection) {
            System.out.println(Joiner.with(Strings.ELEMENT_SEPARATOR, "[", "]").reuseCachedBuffer().appendAll((Collection) obj));
        } else if (obj instanceof Map) {
            System.out.println(Joiner.with(Strings.ELEMENT_SEPARATOR, "=", "{", "}").reuseCachedBuffer().appendEntries((Map) obj));
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
        if (isPrimitiveType(targetType)) {
            return val != null && wrap(targetType).isAssignableFrom(val.getClass()) ? Nullable.of((T) val) : Nullable.<T> empty();
        }

        return val == null || targetType.isAssignableFrom(val.getClass()) ? Nullable.of((T) val) : Nullable.<T> empty();
    }

    /**
     * Returns a {@code Nullable} with the value returned by {@code action} or an empty {@code Nullable} if exception happens.
     *
     * @param <R>
     * @param cmd
     * @return
     * @see Try#call(Throwables.Function)
     * @see Try#call(Throwables.Function, Object)
     */
    @Beta
    public static <R> Nullable<R> tryOrEmptyIfExceptionOccurred(final Callable<R> cmd) {
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
     * @param init
     * @param func
     * @return
     * @see Try#call(Throwables.Function)
     * @see Try#call(Throwables.Function, Object)
     */
    @Beta
    public static <T, R> Nullable<R> tryOrEmptyIfExceptionOccurred(final T init, final Throwables.Function<? super T, ? extends R, ? extends Exception> func) {
        try {
            return Nullable.of(func.apply(init));
        } catch (Exception e) {
            return Nullable.<R> empty();
        }
    }

    /**
     * Returns the value returned by {@code action} or {@code defaultIfExceptionOccurred} if exception happens.
     *
     * @param <R>
     * @param cmd
     * @param defaultIfExceptionOccurred
     * @return
     * @see Try#call(Throwables.Function)
     * @see Try#call(Throwables.Function, Object)
     */
    @Beta
    public static <R> R tryOrDefaultIfExceptionOccurred(final Callable<R> cmd, final R defaultIfExceptionOccurred) {
        try {
            return cmd.call();
        } catch (Exception e) {
            return defaultIfExceptionOccurred;
        }
    }

    /**
     * Returns the value returned by {@code action} or {@code defaultIfExceptionOccurred} if exception happens.
     *
     * @param <T>
     * @param <R>
     * @param init
     * @param func
     * @param defaultIfExceptionOccurred
     * @return
     * @see Try#call(Throwables.Function)
     * @see Try#call(Throwables.Function, Object)
     */
    @Beta
    public static <T, R> R tryOrDefaultIfExceptionOccurred(final T init, final Throwables.Function<? super T, ? extends R, ? extends Exception> func,
            final R defaultIfExceptionOccurred) {
        try {
            return func.apply(init);
        } catch (Exception e) {
            return defaultIfExceptionOccurred;
        }
    }

    /**
     * Returns the value returned by {@code action} or {@code {@code supplierForDefaultIfExceptionOccurred}} if exception happens.
     *
     * @param <R>
     * @param cmd
     * @param supplierForDefaultIfExceptionOccurred
     * @return
     * @see Try#call(Throwables.Function)
     * @see Try#call(Throwables.Function, Object)
     * @see Try#call(Throwables.Function, Supplier)
     */
    @Beta
    public static <R> R tryOrDefaultIfExceptionOccurred(final Callable<R> cmd, final Supplier<R> supplierForDefaultIfExceptionOccurred) {
        try {
            return cmd.call();
        } catch (Exception e) {
            return supplierForDefaultIfExceptionOccurred.get();
        }
    }

    /**
     * Returns the value returned by {@code action} or {@code defaultIfExceptionOccurred} if exception happens.
     *
     * @param <T>
     * @param <R>
     * @param init
     * @param func
     * @param supplierForDefaultIfExceptionOccurred
     * @return
     * @see Try#call(Throwables.Function)
     * @see Try#call(Throwables.Function, Object)
     * @see Try#call(Throwables.Function, Supplier)
     */
    @Beta
    public static <T, R> R tryOrDefaultIfExceptionOccurred(final T init, final Throwables.Function<? super T, ? extends R, ? extends Exception> func,
            final Supplier<R> supplierForDefaultIfExceptionOccurred) {
        try {
            return func.apply(init);
        } catch (Exception e) {
            return supplierForDefaultIfExceptionOccurred.get();
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
    @Beta
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
    @Beta
    public static <T, R, E extends Exception> Nullable<R> ifOrEmpty(final boolean b, final T init, final Throwables.Function<? super T, ? extends R, E> func)
            throws E {
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
    @Beta
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
    @Beta
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

    /**
     *
     *
     * @param <T>
     * @param supplier
     * @return
     */
    @Beta
    public static <T> LazyInitializer<T> lazyInit(final Supplier<T> supplier) {
        return LazyInitializer.of(supplier);
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param supplier
     * @return
     */
    @Beta
    public static <T, E extends Exception> Throwables.LazyInitializer<T, E> lazyInitialize(final Throwables.Supplier<T, E> supplier) {
        return Throwables.LazyInitializer.of(supplier);
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param a
    //     * @param b
    //     * @param nextSelector
    //     * @return
    //     * @see {@code Iterators.merge(Iterator, Iterator, BiFunction)}
    //     */
    //    public static <T, E extends Exception> List<T> merge(final Iterator<? extends T> a, final Iterator<? extends T> b,
    //            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) throws E {
    //        if (a == null) {
    //            return b == null ? new ArrayList<>() : toList(b);
    //        } else if (b == null) {
    //            return toList(a);
    //        }
    //
    //        final List<T> result = new ArrayList<>(9);
    //        final Iterator<? extends T> iterA = a;
    //        final Iterator<? extends T> iterB = b;
    //
    //        T nextA = null;
    //        T nextB = null;
    //        boolean hasNextA = false;
    //        boolean hasNextB = false;
    //
    //        while (hasNextA || hasNextB || iterA.hasNext() || iterB.hasNext()) {
    //            if (hasNextA) {
    //                if (iterB.hasNext()) {
    //                    if (nextSelector.apply(nextA, (nextB = iterB.next())) == MergeResult.TAKE_FIRST) {
    //                        hasNextA = false;
    //                        hasNextB = true;
    //                        result.add(nextA);
    //                    } else {
    //                        result.add(nextB);
    //                    }
    //                } else {
    //                    hasNextA = false;
    //                    result.add(nextA);
    //                }
    //            } else if (hasNextB) {
    //                if (iterA.hasNext()) {
    //                    if (nextSelector.apply((nextA = iterA.next()), nextB) == MergeResult.TAKE_FIRST) {
    //                        result.add(nextA);
    //                    } else {
    //                        hasNextA = true;
    //                        hasNextB = false;
    //                        result.add(nextB);
    //                    }
    //                } else {
    //                    hasNextB = false;
    //                    result.add(nextB);
    //                }
    //            } else if (iterA.hasNext()) {
    //                if (iterB.hasNext()) {
    //                    if (nextSelector.apply((nextA = iterA.next()), (nextB = iterB.next())) == MergeResult.TAKE_FIRST) {
    //                        hasNextB = true;
    //                        result.add(nextA);
    //                    } else {
    //                        hasNextA = true;
    //                        result.add(nextB);
    //                    }
    //                } else {
    //                    result.add(iterA.next());
    //                }
    //            } else {
    //                result.add(iterB.next());
    //            }
    //        }
    //
    //        return result;
    //    }

    //    /**
    //     *
    //     * @param <A>
    //     * @param <B>
    //     * @param <R>
    //     * @param a
    //     * @param b
    //     * @param zipFunction
    //     * @return
    //     * @see {@code Iterators.zip(Iterator, Iterator, BiFunction)}
    //     */
    //    public static <A, B, R, E extends Exception> List<R> zip(final Iterator<A> a, final Iterator<B> b,
    //            final Throwables.BiFunction<? super A, ? super B, ? extends R, E> zipFunction) throws E {
    //        checkArgNotNull(zipFunction);
    //
    //        if (a == null || b == null) {
    //            return new ArrayList<>();
    //        }
    //
    //        final Iterator<A> iterA = a;
    //        final Iterator<B> iterB = b;
    //        final List<R> result = new ArrayList<>(9);
    //
    //        while (iterA.hasNext() && iterB.hasNext()) {
    //            result.add(zipFunction.apply(iterA.next(), iterB.next()));
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     *
    //     * @param <A>
    //     * @param <B>
    //     * @param <C>
    //     * @param <R>
    //     * @param a
    //     * @param b
    //     * @param c
    //     * @param zipFunction
    //     * @return
    //     * @see {@code Iterators.zip(Iterator, Iterator, Iterator, TriFunction)}
    //     */
    //    public static <A, B, C, R, E extends Exception> List<R> zip(final Iterator<A> a, final Iterator<B> b, final Iterator<C> c,
    //            final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends R, E> zipFunction) throws E {
    //        checkArgNotNull(zipFunction);
    //
    //        if (a == null || b == null || c == null) {
    //            return new ArrayList<>();
    //        }
    //
    //        final Iterator<A> iterA = a;
    //        final Iterator<B> iterB = b;
    //        final Iterator<C> iterC = c;
    //        final List<R> result = new ArrayList<>(9);
    //
    //        while (iterA.hasNext() && iterB.hasNext() && iterC.hasNext()) {
    //            result.add(zipFunction.apply(iterA.next(), iterB.next(), iterC.next()));
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     *
    //     * @param <A>
    //     * @param <B>
    //     * @param <R>
    //     * @param a
    //     * @param b
    //     * @param valueForNoneA
    //     * @param valueForNoneB
    //     * @param zipFunction
    //     * @return
    //     * @see {@code Iterators.zip(Iterator, Iterator, Object, Object, BiFunction)}
    //     */
    //    public static <A, B, R, E extends Exception> List<R> zip(final Iterator<A> a, final Iterator<B> b, final A valueForNoneA, final B valueForNoneB,
    //            final Throwables.BiFunction<? super A, ? super B, ? extends R, E> zipFunction) throws E {
    //        checkArgNotNull(zipFunction);
    //
    //        final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a;
    //        final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b;
    //
    //        final List<R> result = new ArrayList<>(9);
    //        boolean hasA = true;
    //
    //        do {
    //            if (hasA && (hasA = iterA.hasNext())) {
    //                result.add(zipFunction.apply(iterA.next(), iterB.hasNext() ? iterB.next() : valueForNoneB));
    //            } else if (iterB.hasNext()) {
    //                result.add(zipFunction.apply(valueForNoneA, iterB.next()));
    //            } else {
    //                break;
    //            }
    //        } while (true);
    //
    //        return result;
    //    }
    //
    //    /**
    //     *
    //     * @param <A>
    //     * @param <B>
    //     * @param <C>
    //     * @param <R>
    //     * @param a
    //     * @param b
    //     * @param c
    //     * @param valueForNoneA
    //     * @param valueForNoneB
    //     * @param valueForNoneC
    //     * @param zipFunction
    //     * @return
    //     * @see {@code Iterators.zip(Iterator, Iterator, Iterator, Object, Object, Object, TriFunction)}
    //     */
    //    public static <A, B, C, R, E extends Exception> List<R> zip(final Iterator<A> a, final Iterator<B> b, final Iterator<C> c, final A valueForNoneA,
    //            final B valueForNoneB, final C valueForNoneC, final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends R, E> zipFunction) throws E {
    //        checkArgNotNull(zipFunction);
    //
    //        final Iterator<A> iterA = a == null ? ObjIterator.<A> empty() : a;
    //        final Iterator<B> iterB = b == null ? ObjIterator.<B> empty() : b;
    //        final Iterator<C> iterC = c == null ? ObjIterator.<C> empty() : c;
    //
    //        final List<R> result = new ArrayList<>(9);
    //        boolean hasA = true;
    //        boolean hasB = true;
    //
    //        do {
    //            if (hasA && (hasA = iterA.hasNext())) {
    //                result.add(zipFunction.apply(iterA.next(), iterB.hasNext() ? iterB.next() : valueForNoneB, iterC.hasNext() ? iterC.next() : valueForNoneC));
    //            } else if (hasB && (hasB = iterB.hasNext())) {
    //                result.add(zipFunction.apply(valueForNoneA, iterB.next(), iterC.hasNext() ? iterC.next() : valueForNoneC));
    //            } else if (iterC.hasNext()) {
    //                result.add(zipFunction.apply(valueForNoneA, valueForNoneB, iterC.hasNext() ? iterC.next() : valueForNoneC));
    //            } else {
    //                break;
    //            }
    //        } while (true);
    //
    //        return result;
    //    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param <L>
    //     * @param <R>
    //     * @param iter
    //     * @param unzip the second parameter is an output parameter.
    //     * @return
    //     * @see {@code Iterators.unzip(Iterator, BiConsumer)}
    //     */
    //    public static <T, L, R, E extends Exception> Pair<List<L>, List<R>> unzip(final Iterator<? extends T> iter,
    //            final Throwables.BiConsumer<? super T, Pair<L, R>, E> unzip) throws E {
    //        checkArgNotNull(unzip);
    //
    //        final int len = 9;
    //
    //        final List<L> l = new ArrayList<>(len);
    //        final List<R> r = new ArrayList<>(len);
    //        final Pair<L, R> p = new Pair<>();
    //
    //        if (iter != null) {
    //            T e = null;
    //
    //            while (iter.hasNext()) {
    //                e = iter.next();
    //
    //                unzip.accept(e, p);
    //
    //                l.add(p.left);
    //                r.add(p.right);
    //            }
    //        }
    //
    //        return Pair.of(l, r);
    //    }

    /**
     * Checks if is primitive type.
     *
     * @param cls
     * @return true, if is primitive type
     */
    public static boolean isPrimitiveType(final Class<?> cls) {
        checkArgNotNull(cls, "cls");

        return typeOf(cls).isPrimitiveType();
    }

    /**
     * Checks if is wrapper type.
     *
     * @param cls
     * @return true, if is wrapper type
     */
    public static boolean isWrapperType(final Class<?> cls) {
        checkArgNotNull(cls, "cls");

        return typeOf(cls).isPrimitiveWrapper();
    }

    /**
     * Checks if is primitive array type.
     *
     * @param cls
     * @return true, if is primitive array type
     */
    public static boolean isPrimitiveArrayType(final Class<?> cls) {
        checkArgNotNull(cls, "cls");

        return typeOf(cls).isPrimitiveArray();
    }

    /**
     * Returns the corresponding wrapper type of {@code type} if it is a primitive type; otherwise
     * returns {@code type} itself. Idempotent.
     *
     * <pre>
     *     wrap(int.class) == Integer.class
     *     wrap(Integer.class) == Integer.class
     *     wrap(String.class) == String.class
     * </pre>
     *
     * @param cls
     * @return
     */
    public static Class<?> wrap(final Class<?> cls) {
        checkArgNotNull(cls, "cls");

        final Class<?> wrapped = PRIMITIVE_2_WRAPPER.get(cls);

        return wrapped == null ? cls : wrapped;
    }

    /**
     * Returns the corresponding primitive type of {@code type} if it is a wrapper type; otherwise
     * returns {@code type} itself. Idempotent.
     *
     * <pre>
     *     unwrap(Integer.class) == int.class
     *     unwrap(int.class) == int.class
     *     unwrap(String.class) == String.class
     * </pre>
     *
     * @param cls
     * @return
     */
    public static Class<?> unwrap(final Class<?> cls) {
        checkArgNotNull(cls, "cls");

        Class<?> unwrapped = PRIMITIVE_2_WRAPPER.getByValue(cls);

        return unwrapped == null ? cls : unwrapped;
    }

    // Boolean utilities
    //--------------------------------------------------------------------------

    //    @Beta
    //    public static boolean isNullOrFalse(final Boolean bool) {
    //        if (bool == null) {
    //            return true;
    //        }
    //
    //        return Boolean.FALSE.equals(bool);
    //    }
    //
    //    @Beta
    //    public static boolean isNullOrTrue(final Boolean bool) {
    //        if (bool == null) {
    //            return true;
    //        }
    //
    //        return Boolean.TRUE.equals(bool);
    //    }

    /**
     * Returns {@code true} if the specified {@code boolean} is {@code Boolean.TRUE}, not {@code null} or {@code Boolean.FALSE}.
     *
     * @param bool
     * @return
     */
    @Beta
    public static boolean isTrue(final Boolean bool) {
        return Boolean.TRUE.equals(bool);
    }

    /**
     * Returns {@code true} if the specified {@code boolean} is {@code null} or {@code Boolean.FALSE}.
     *
     * @param bool
     * @return
     */
    @Beta
    public static boolean isNotTrue(final Boolean bool) {
        return bool == null || Boolean.FALSE.equals(bool);
    }

    /**
     * Returns {@code true} if the specified {@code boolean} is {@code Boolean.FALSE}, not {@code null} or {@code Boolean.TRUE}.
     *
     * @return
     */
    @Beta
    public static boolean isFalse(final Boolean bool) {
        return Boolean.FALSE.equals(bool);
    }

    /**
     * Returns {@code true} if the specified {@code boolean} is {@code null} or {@code Boolean.TRUE}.
     *
     * @param bool
     * @return
     */
    @Beta
    public static boolean isNotFalse(final Boolean bool) {
        return bool == null || Boolean.TRUE.equals(bool);
    }

    /**
     * <p>Note: copied from Apache commons Lang under Apache license v2.0 </p>
     *
     * <p>Negates the specified boolean.</p>
     *
     * <p>If {@code null} is passed in, {@code null} will be returned.</p>
     *
     * <p>NOTE: This returns null and will throw a NullPointerException if autoboxed to a boolean. </p>
     *
     * <pre>
     *   BooleanUtils.negate(Boolean.TRUE)  = Boolean.FALSE;
     *   BooleanUtils.negate(Boolean.FALSE) = Boolean.TRUE;
     *   BooleanUtils.negate(null)          = null;
     * </pre>
     *
     * @param bool  the Boolean to negate, may be null
     * @return the negated Boolean, or {@code null} if {@code null} input
     */
    @MayReturnNull
    @Beta
    public static Boolean negate(final Boolean bool) {
        if (bool == null) {
            return null; //NOSONAR
        }

        return bool.booleanValue() ? Boolean.FALSE : Boolean.TRUE;
    }

    /**
     * <p>Negates boolean values in the specified boolean array</p>
     *
     *
     * @param a
     */
    @Beta
    public static void negate(final boolean[] a) {
        if (isEmpty(a)) {
            return;
        }

        negate(a, 0, a.length);
    }

    /**
     * <p>Negates boolean values {@code fromIndex} to {@code toIndex} in the specified boolean array</p>
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     */
    @Beta
    public static void negate(final boolean[] a, final int fromIndex, final int toIndex) {
        checkFromToIndex(fromIndex, toIndex, len(a));

        if (fromIndex == toIndex) {
            return;
        }

        for (int i = fromIndex; i < toIndex; i++) {
            a[i] = !a[i];
        }
    }

    //    /**
    //     * Returns {@code 0} if the specified {@code bool} is {@code null} or {@code false}, otherwise {@code 1} is returned.
    //     *
    //     * @param bool
    //     * @return
    //     */
    //    @Beta
    //    public static int toIntOneZero(final Boolean bool) {
    //        if (bool == null) {
    //            return 0;
    //        }
    //
    //        return bool.booleanValue() ? 1 : 0;
    //    }
    //
    //    /**
    //     * Returns {@code 'N'} if the specified {@code bool} is {@code null} or {@code false}, otherwise {@code 'Y'} is returned.
    //     *
    //     *
    //     * @param bool
    //     * @return
    //     */
    //    @Beta
    //    public static char toCharYN(final Boolean bool) {
    //        if (bool == null) {
    //            return 'N';
    //        }
    //
    //        return bool.booleanValue() ? 'Y' : 'N';
    //    }
    //
    //    /**
    //     * Returns {@code "no"} if the specified {@code bool} is {@code null} or {@code false}, otherwise {@code "yes"} is returned.
    //     *
    //     *
    //     * @param bool
    //     * @return
    //     */
    //    @Beta
    //    public static String toStringYesNo(final Boolean bool) {
    //        if (bool == null) {
    //            return "no";
    //        }
    //
    //        return bool.booleanValue() ? "yes" : "no";
    //    }
    /**
     * Add it because {@code Comparator.reversed()} doesn't work well in some scenarios.
     *
     * @param <T>
     * @param cmp
     * @return
     * @see Collections#reverseOrder(Comparator)
     */
    @Beta
    public static <T> Comparator<T> reverseOrder(final Comparator<T> cmp) {
        return Comparators.reverseOrder(cmp);
    }

    /**
     * Returns an {@code unmodifiable view} of the specified {@code Collection}. Or an empty {@code Collection} if the specified {@code collection} is null.
     *
     * @param <T>
     * @param c
     * @return an empty {@code Collection} if the specified {@code c} is null.
     */
    public static <T> Collection<T> unmodifiableCollection(Collection<? extends T> c) {
        if (c == null) {
            return emptyList();
        }

        return Collections.unmodifiableCollection(c);
    }

    /**
     * Returns an {@code unmodifiable view} of the specified {@code List}. Or an empty {@code List} if the specified {@code list} is null.
     *
     * @param <T>
     * @param list
     * @return
     * @see Collections.unmodifiableList(List)
     */
    public static <T> List<T> unmodifiableList(final List<? extends T> list) {
        if (list == null) {
            return emptyList();
        }

        return Collections.unmodifiableList(list);
    }

    /**
     * Returns an {@code unmodifiable view} of the specified {@code Set}. Or an empty {@code Set} if the specified {@code set} is null.
     *
     * @param <T>
     * @param s
     * @return
     * @see Collections.unmodifiableSet(Set)
     */
    public static <T> Set<T> unmodifiableSet(final Set<? extends T> s) {
        if (s == null) {
            return emptySet();
        }

        return Collections.unmodifiableSet(s);
    }

    /**
     * Returns an {@code unmodifiable view} of the specified {@code SortedSet}. Or an empty {@code SortedSet} if the specified {@code set} is null.
     *
     * @param <T>
     * @param s
     * @return
     * @see Collections.unmodifiableSet(SortedSet)
     */
    public static <T> SortedSet<T> unmodifiableSortedSet(final SortedSet<T> s) {
        if (s == null) {
            return emptySortedSet();
        }

        return Collections.unmodifiableSortedSet(s);
    }

    /**
     * Returns an {@code unmodifiable view} of the specified {@code NavigableSet}. Or an empty {@code NavigableSet} if the specified {@code set} is null.
     *
     * @param <T>
     * @param s
     * @return
     * @see Collections.unmodifiableNavigableSet(NavigableSet)
     */
    public static <T> NavigableSet<T> unmodifiableNavigableSet(final NavigableSet<T> s) {
        if (s == null) {
            return emptyNavigableSet();
        }

        return Collections.unmodifiableNavigableSet(s);
    }

    /**
     * Returns an {@code unmodifiable view} of the specified {@code Map}. Or an empty {@code Map} if the specified {@code map} is null.
     *
     * @param <K>
     * @param <V>
     * @param m
     * @return
     * @see Collections#unmodifiableMap(Map)
     */
    public static <K, V> Map<K, V> unmodifiableMap(Map<? extends K, ? extends V> m) {
        if (m == null) {
            return emptyMap();
        }

        return Collections.unmodifiableMap(m);
    }

    /**
     * Returns an {@code unmodifiable view} of the specified {@code SortedMap}. Or an empty {@code SortedMap} if the specified {@code map} is null.
     *
     * @param <K>
     * @param <V>
     * @param m
     * @return
     * @see Collections#unmodifiableSortedMap(SortedMap)
     */
    public static <K, V> SortedMap<K, V> unmodifiableSortedMap(SortedMap<K, ? extends V> m) {
        if (m == null) {
            return emptySortedMap();
        }

        return Collections.unmodifiableSortedMap(m);
    }

    /**
     * Returns an {@code unmodifiable view} of the specified {@code NavigableMap}. Or an empty {@code NavigableMap} if the specified {@code map} is null.
     *
     * @param <K>
     * @param <V>
     * @param m
     * @return
     * @see Collections#unmodifiableNavigableMap(NavigableMap)
     */
    public static <K, V> NavigableMap<K, V> unmodifiableNavigableMap(NavigableMap<K, ? extends V> m) {
        if (m == null) {
            return emptyNavigableMap();
        }

        return Collections.unmodifiableNavigableMap(m);
    }

    /**
     * Updates each element in the specified array {@code a} with specified function {@code converter}.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param converter
     * @throws E
     */
    @Beta
    public static <T, E extends Exception> void applyToEach(final T[] a, final Throwables.Function<? super T, ? extends T, E> converter) throws E {
        checkArgNotNull(converter);

        if (isEmpty(a)) {
            return;
        }

        for (int i = 0, len = a.length; i < len; i++) {
            a[i] = converter.apply(a[i]);
        }
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param converter
     * @throws E
     */
    @Beta
    public static <T, E extends Exception> void applyToEach(final List<T> c, final Throwables.Function<? super T, ? extends T, E> converter) throws E {
        checkArgNotNull(converter);

        if (isEmpty(c)) {
            return;
        }

        if (c instanceof ArrayList) {
            for (int i = 0, size = c.size(); i < size; i++) {
                c.set(i, converter.apply(c.get(i)));
            }
        } else {
            final ListIterator<T> iter = c.listIterator();

            while (iter.hasNext()) {
                iter.set(converter.apply(iter.next()));
            }
        }
    }

    /**
     * Copy the specified array {@code a} first, then call {@code converter} on the copy.
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param converter
     * @return updated copy of {@code a}. {@code null} if {@code (a == null)}. (auto-generated java doc for return)
     * @throws E
     * @see {@link #map(Object[], com.landawn.abacus.util.Throwables.Function)}
     * @see {@link N#map(Iterable, com.landawn.abacus.util.Throwables.Function)}
     */
    @MayReturnNull
    public static <T, E extends Exception> T[] copyThenApply(final T[] a, final Throwables.Function<? super T, ? extends T, E> converter) throws E {
        checkArgNotNull(converter);

        if (a == null) {
            return null; // NOSONAR
        } else if (a.length == 0) {
            return a.clone();
        }

        final T[] copy = a.clone();

        for (int i = 0, len = a.length; i < len; i++) {
            copy[i] = converter.apply(a[i]);
        }

        return a;
    }
}
